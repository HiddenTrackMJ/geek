package org.seekloud.geek.core

import java.awt.Graphics
import java.awt.image.BufferedImage
import java.io.{File, OutputStream}
import java.nio.ShortBuffer

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{Behaviors, StashBuffer, TimerScheduler}
import org.bytedeco.ffmpeg.global.{avcodec, avutil}
import org.bytedeco.javacv.{FFmpegFrameFilter, FFmpegFrameRecorder, Frame, Java2DFrameConverter}
import org.seekloud.geek.Boot
import org.seekloud.geek.common.AppSettings
import org.seekloud.geek.core.Grabber.State
import org.seekloud.geek.core.RoomDealer.getVideoDuration
import org.seekloud.geek.models.SlickTables
import org.seekloud.geek.models.dao.VideoDao
import org.seekloud.geek.shared.ptcl.Protocol.OutTarget
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.collection.immutable.Queue
import scala.util.{Failure, Success, Try}
import scala.concurrent.duration._

/**
 * Author: Jason
 * Date: 2020/1/28
 * Time: 12:20
 */
object Recorder {

  var audioChannels = 2 //todo 待议
  val sampleFormat = 1 //todo 待议
  var frameRate = 30
  val bitRate = 2000000

  private var peopleOnline = 0

  private val log = LoggerFactory.getLogger(this.getClass)

  sealed trait Command

  case class UpdateRoomInfo(roomId: Long, layout: Int) extends Command

  case object Init extends Command

  case class GetGrabber(id: String, grabber: ActorRef[Grabber.Command]) extends Command

  case object RestartRecord extends Command

  case class StopRecorder(msg: String) extends Command

  case class GrabberStopped(liveId: String) extends Command

  case object CloseRecorder extends Command

  case object StoreOver extends Command

  case class NewFrame(liveId: String, frame: Frame) extends Command

  case class Shield(liveId: String, image: Boolean, audio: Boolean) extends Command

  case class Appoint(liveId: String, status: Boolean) extends Command

  case class UpdateRecorder(channel: Int, sampleRate: Int, frameRate: Double, width: Int, height: Int, liveId: String) extends Command

  case object TimerKey4Close

  final case object TimerKey4ImageRec

  case object RecordImage extends Command with DrawCommand


  sealed trait DrawCommand

  case class Image4Host(id: String, frame: Frame) extends DrawCommand

  case class Image4Others(id: String, frame: Frame) extends DrawCommand

  case class DeleteImage4Others(id: String) extends DrawCommand

  case class SetLayout(layout: Int) extends DrawCommand

  case class NewRecord4Ts(recorder4ts: FFmpegFrameRecorder) extends DrawCommand

  case object Close extends DrawCommand

  sealed trait SampleCommand

  case class GetFFfilter(filters: mutable.HashMap[Int, FFmpegFrameFilter]) extends SampleCommand

  case class Sample4Host(id: String, shieldMap :mutable.HashMap[String, State] = mutable.HashMap.empty, isOne: Boolean, frame: Frame, isSilence: Boolean) extends SampleCommand

  case class Sample4All(id: String, frame: Frame, isSilence: Boolean) extends SampleCommand

  case class DeleteSample4Others(id: String) extends SampleCommand

  case object CloseSample extends SampleCommand

  case class Ts4Host(var time: Long = 0)

  case class Ts4Others(var time: Long = 0)

  case class Image(var frame: mutable.HashMap[String, Queue[Frame]] = mutable.HashMap.empty)

  case class Audio(var frame: mutable.HashMap[String, (Int, Queue[Either[Frame, Frame]])] = mutable.HashMap.empty)

  case class Ts4LastImage(var frame: mutable.HashMap[String, Frame] = mutable.HashMap.empty)

  case class Ts4LastSample(var time: Long = 0)

  def create(roomId: Long, hostId: Long, stream: String, pullLiveId:List[String], roomActor: ActorRef[RoomDealer.Command], layout: Int, outTarget: Option[OutTarget] = None): Behavior[Command] = {
    Behaviors.setup[Command] { ctx =>
      implicit val stashBuffer: StashBuffer[Command] = StashBuffer[Command](Int.MaxValue)
      Behaviors.withTimers[Command] {
        implicit timer =>
          log.info(s"recorder-$roomId start----")
          avutil.av_log_set_level(-8)
          val srcPath = AppSettings.rtmpServer + stream//System.currentTimeMillis().toString
          println(s"path: $srcPath")
          val recorder4ts = new FFmpegFrameRecorder(srcPath, 640, 480, audioChannels)
          recorder4ts.setInterleaved(true)
          recorder4ts.setFrameRate(frameRate)
          recorder4ts.setVideoBitrate(bitRate)
          //          recorder4ts.setVideoCodec(avcodec.AV_CODEC_ID_MPEG2VIDEO)
          //          recorder4ts.setAudioCodec(avcodec.AV_CODEC_ID_MP2)
          //          recorder4ts.setVideoOption("preset","ultrafast")
          recorder4ts.setVideoOption("crf", "25")
          recorder4ts.setAudioQuality(0)
          recorder4ts.setSampleRate(44100)
          recorder4ts.setMaxBFrames(0)
          //          recorder4ts.setFormat("mpegts")
          recorder4ts.setVideoCodec(avcodec.AV_CODEC_ID_H264)
          recorder4ts.setAudioCodec(avcodec.AV_CODEC_ID_AAC)
          recorder4ts.setFormat("flv")
          Try {
            recorder4ts.start()
          } match {
            case Success(_) =>
              log.info(s"$roomId recorder starts successfully")
            case Failure(e) =>
              log.error(s" recorder starts error: ${e.getMessage}")
          }
          val canvas = new BufferedImage(640, 480, BufferedImage.TYPE_3BYTE_BGR)
          val drawer = ctx.spawn(draw(canvas, canvas.getGraphics, Ts4LastImage(), Queue.empty, Image(), recorder4ts,
            new Java2DFrameConverter(), new Java2DFrameConverter(),new Java2DFrameConverter,mutable.HashMap.empty, layout, "defaultImg.jpg", roomId, (640, 480)), s"drawer_$roomId")

          val sampleRecorder = ctx.spawn(sampleRecord(roomId, Ts4LastSample(), Audio(), recorder4ts,mutable.HashMap.empty, None), s"sampleRecorder_$roomId")
          ctx.self ! Init
          idle(roomId, hostId, stream, pullLiveId, roomActor,List.empty, pullLiveId.head, recorder4ts, mutable.HashMap.empty, drawer, sampleRecorder, mutable.HashMap.empty)
      }
    }
  }

  private def idle(
    roomId: Long,
    hostId: Long,
    stream: String,
    pullLiveId:List[String],
    roomDealer: ActorRef[RoomDealer.Command],
    online: List[Int],
    host: String,
    recorder4ts: FFmpegFrameRecorder,
    ffFilter:  mutable.HashMap[Int, FFmpegFrameFilter],
    drawer: ActorRef[DrawCommand],
    sampleRecorder: ActorRef[SampleCommand],
    grabbers: mutable.HashMap[String, ActorRef[Grabber.Command]], // id -> grabActor
    indexMap :mutable.HashMap[String, Int] = mutable.HashMap.empty,
    shieldMap :mutable.HashMap[String, State] = mutable.HashMap.empty,
    filterInUse: Option[FFmpegFrameFilter] = None,
  )(
    implicit stashBuffer: StashBuffer[Command],
    timer: TimerScheduler[Command]
  ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case Init =>
          val filters = mutable.HashMap[Int, FFmpegFrameFilter]()
          if (pullLiveId.size > 1) {
            (2 to pullLiveId.size).foreach { p =>
              val complexFilter = s" amix=inputs=$p:duration=longest:dropout_transition=3:weights=3"
              //音频混流
              var filterStr = complexFilter
              (1 to p).reverse.foreach { i =>
                filterStr = s"[${i - 1}:a]" + filterStr
              }
              filterStr += "[a]"
              log.debug(s"audio filter-$p: $filterStr")

              val filter = new FFmpegFrameFilter(
                filterStr,
                audioChannels
              )
              filter.setAudioChannels(audioChannels)
              filter.setSampleFormat(sampleFormat)
              filter.setAudioInputs(p)
              //              filter.setFilters(filterStr)
              filters.put(p, filter)
              filter.startUnsafe()
            }
          }
          sampleRecorder ! GetFFfilter(filters)
          idle(roomId, hostId, stream, pullLiveId, roomDealer, online, host, recorder4ts,filters, drawer, sampleRecorder, grabbers, indexMap, shieldMap, filterInUse)

        case GetGrabber(id, grabber) =>
          grabber ! Grabber.GetRecorder(ctx.self)
          grabbers.put(id, grabber)
          Behaviors.same

        case GrabberStopped(liveId) =>
          grabbers.-=(liveId)
          drawer ! DeleteImage4Others(liveId)
          Behaviors.same

        case Shield(liveId, image, audio) =>
          shieldMap.update(liveId, State(image, audio))
          println(s"shieldMap : $shieldMap")
          if (!image) drawer ! DeleteImage4Others(liveId)
          Behaviors.same

        case Appoint(liveId, status) =>
          log.info(s"appoint to $liveId")
          val newShield = if (status) {
            shieldMap.map { s =>
              if (s._1 != liveId) (s._1, State(s._2.image, audio = false))
              else (s._1, State(image = true, audio = false))
            }
          }
          else {
            shieldMap.map { s =>
              (s._1, State(image = true, audio = true))
            }
          }
          idle(roomId, hostId, stream, pullLiveId, roomDealer, online, liveId, recorder4ts, ffFilter, drawer, sampleRecorder, grabbers, indexMap, newShield, filterInUse)


        case UpdateRecorder(channel, sampleRate, f, width, height, liveId) =>
          peopleOnline += 1
          val onlineNew = online :+ liveId.split("_").last.toInt
          shieldMap.put(liveId, State(image = true, audio = true))
          log.info(s"$roomId updateRecorder channel:$channel, sampleRate:$sampleRate, frameRate:$f, width:$width, height:$height")
          if(liveId == host) {
            //            log.info(s"$roomId updateRecorder channel:$channel, sampleRate:$sampleRate, frameRate:$f, width:$width, height:$height")
            recorder4ts.setFrameRate(f)
            recorder4ts.setAudioChannels(channel)
            recorder4ts.setSampleRate(sampleRate)
            ffFilter.foreach(_._2.setAudioChannels(channel))
            ffFilter.foreach(_._2.setSampleRate(sampleRate))
            recorder4ts.setImageWidth(width)
            recorder4ts.setImageHeight(height)
            //            timer.startPeriodicTimer(TimerKey4ImageRec, RecordImage, 10.millis)
            idle(roomId, hostId, stream, pullLiveId, roomDealer, onlineNew, host, recorder4ts, ffFilter, drawer, sampleRecorder, grabbers, indexMap, shieldMap, filterInUse)
          }
          else idle(roomId, hostId, stream, pullLiveId, roomDealer, onlineNew, host, recorder4ts, ffFilter, drawer, sampleRecorder, grabbers, indexMap, shieldMap, filterInUse)


        case RecordImage =>
          drawer ! RecordImage
          Behaviors.same

        case NewFrame(liveId, frame) =>
          if (frame.image != null) {
            //            println(liveId, frame.timestamp)
            if (liveId == host && shieldMap.filter(_._2.image).exists(_._1 == liveId)) {
              //              recorder4ts.record(frame.clone())
              drawer ! Image4Others(liveId, frame)
              drawer ! Image4Host(liveId, frame)
            } else if (pullLiveId.contains(liveId) && shieldMap.filter(_._2.image).exists(_._1 == liveId)) {
              drawer ! Image4Others(liveId, frame)
            } else {
              log.info(s"wrong, liveId, work got wrong img")
            }
          }
          var index = liveId.split("_").last.toInt
          var newFilterInUse = filterInUse
          if (frame.samples != null) {
            if (liveId == host) {
              //              recorder4ts.record(frame.clone())
              sampleRecorder ! Sample4All(liveId, frame, !shieldMap.filter(_._2.audio).exists(_._1 == liveId))
              sampleRecorder ! Sample4Host(liveId, shieldMap,online.size == 1 || shieldMap.count(_._2.audio) == 1, frame, !shieldMap.filter(_._2.audio).exists(_._1 == liveId))
            } else if (pullLiveId.contains(liveId)) {
              sampleRecorder ! Sample4All(liveId, frame, !shieldMap.filter(_._2.audio).exists(_._1 == liveId))
            } else {
              log.info(s"wrong, liveId, work got wrong sample")
            }
          }
          idle(roomId, hostId, stream, pullLiveId, roomDealer, online, host, recorder4ts, ffFilter, drawer, sampleRecorder, grabbers, indexMap, shieldMap, newFilterInUse)

        case CloseRecorder =>
          val video = SlickTables.rVideo(0L, hostId, roomId, stream.split("_").last.toLong, stream + ".mp4", "",0,"")
          try {
            log.info(s"Recorder-${roomId} is storing video...path: ${AppSettings.videoPath}${video.filename}")
            var d = ""
            val file = new File(s"${AppSettings.videoPath}${video.filename}")
            if(file.exists()){
              d = getVideoDuration(s"${file.getPath}")
              log.info(s"get durationpath:${file.getPath}")
              log.info(s"duration:$d")
              val videoNew = video.copy(length = d)
              VideoDao.addVideo(videoNew)
            }else{
              log.info(s"no record for roomId:${roomId} and startTime:${video.timestamp}")
            }
            //Thread.sleep(2000)
            //            roomDealer ! RoomDealer.StoreVideo(video)
          } catch {
            case e: Exception =>
              log.error(s"$roomId record stored error, ${e.getMessage}")
          }
          timer.startSingleTimer("StoreOver", StoreOver, 5.seconds)
          Behaviors.same

        case StoreOver =>
          Behaviors.stopped

        case StopRecorder(msg) =>
          log.info(s"recorder-$roomId stop because $msg")
          sampleRecorder ! CloseSample
          drawer ! Close
          timer.startSingleTimer(TimerKey4Close, CloseRecorder, 10.seconds)
          Behaviors.same

        case x@_ =>
          log.info(s"${ctx.self} got an unknown msg:$x")
          Behaviors.same
      }
    }

    var last = System.currentTimeMillis()
  def draw(
    canvas: BufferedImage,
    graph: Graphics,
    lastTime: Ts4LastImage,
    hostFrame: Queue[Frame],
    clientFrame: Image,
    recorder4ts: FFmpegFrameRecorder,
    convert1: Java2DFrameConverter, convert2: Java2DFrameConverter, convert:Java2DFrameConverter,
    converter4Others: mutable.HashMap[String, Java2DFrameConverter],
    layout: Int = 0,
    bgImg: String,
    roomId: Long,
    canvasSize: (Int, Int)): Behavior[DrawCommand] = {
    Behaviors.setup[DrawCommand] { ctx =>
      Behaviors.receiveMessage[DrawCommand] {
        case t: Image4Host =>
          //          log.info(s"add host")
          val time = t.frame.timestamp
          val hostFrame = if (clientFrame.frame.get(t.id).isEmpty) lastTime.frame(t.id) else {
            val res = clientFrame.frame(t.id).dequeue
            clientFrame.frame.update(t.id, res._2)
            res._1
          }
          val img = convert1.convert(hostFrame)
          lastTime.frame.put(t.id, hostFrame)
          val clientImg = if(clientFrame.frame.nonEmpty) clientFrame.frame.toList.filter(_._1 != t.id).sortBy(_._1.split("_").last.toInt).zipWithIndex.map{
            j =>
              val i = j._1
              if (i._2.nonEmpty) {
                val frameNew = i._2.dequeue
                //                val newFrame = frameNew._1.clone()
                clientFrame.frame.update(i._1, frameNew._2)
                lastTime.frame.put(i._1, frameNew._1)
                if (converter4Others.contains(i._1)){
                  converter4Others(i._1).convert(frameNew._1)
                }
                else {
                  val newConvert = new Java2DFrameConverter
                  converter4Others.put(i._1, newConvert)
                  newConvert.convert(frameNew._1)
                }
              }
              else {
                convert2.convert(lastTime.frame.getOrElse(i._1, t.frame))
              }
          }
          else List.empty
          //          graph.clearRect(0, 0, canvasSize._1, canvasSize._2)
          layout match {
            case 0 =>
              graph.drawImage(img, 0, 0, canvasSize._1 , canvasSize._2 , null)
              //              graph.drawString("主播", 24, 24)
              //              graph.drawImage(clientImg, canvasSize._1 / 2, canvasSize._2 / 4, canvasSize._1 / 2, canvasSize._2 / 2, null)
              //              graph.drawString("观众", 344, 24)
              clientImg.zipWithIndex.foreach{ i =>
                val index = i._2
                index match {
                  case 0 => graph.drawImage(i._1, 0, 0, canvasSize._1 / 3, canvasSize._2 / 3, null)
                  case 1 => graph.drawImage(i._1, 0, canvasSize._2 / 3 * 2, canvasSize._1 / 3, canvasSize._2 / 3, null)
                  case 2 => graph.drawImage(i._1, canvasSize._1 / 3 * 2, 0, canvasSize._1 / 3, canvasSize._2 / 3, null)
                  case 3 => graph.drawImage(i._1, canvasSize._1 / 3 * 2, canvasSize._2 / 3 * 2, canvasSize._1 / 3, canvasSize._2 / 3, null)
                  case _ =>
                }
              }

            case 1 =>
              graph.drawImage(img, 0, 0, canvasSize._1, canvasSize._2, null)
              graph.drawString("主播", 24, 24)
            //              graph.drawImage(clientImg, canvasSize._1 / 4 * 3, 0, canvasSize._1 / 4, canvasSize._2 / 4, null)
            //              graph.drawString("观众", 584, 24)

            case 2 =>
              //              graph.drawImage(clientImg, 0, 0, canvasSize._1, canvasSize._2, null)
              //              graph.drawString("观众", 24, 24)
              graph.drawImage(img, canvasSize._1 / 4 * 3, 0, canvasSize._1 / 4, canvasSize._2 / 4, null)
              graph.drawString("主播", 584, 24)

          }
          //fixme 此处为何不直接recordImage
          val frame = convert.convert(canvas)
          try{
            //            log.info(s"record image: ${frame.imageWidth}")
            //            recorder4ts.record(frame.clone())
            val f = frame.clone()
            recorder4ts.recordImage(
              f.imageWidth,
              f.imageHeight,
              f.imageDepth,
              f.imageChannels,
              f.imageStride,
              recorder4ts.getPixelFormat,
              f.image: _*
            )
//                        val a = System.currentTimeMillis()
//                        println(s"record: ${a - last}ms")
//                        last = a
          }
          catch {
            case e: Exception =>
              log.info(s"record error: ${e.getMessage}")
          }
          //            lastTime.time = time
          Behaviors.same

        case Image4Others(id, frame) =>
          //          log.info(s"add ${t.id}")
          //          clientFrame.frame.put(t.id, t.frame)
          if (!clientFrame.frame.contains(id)) {
            val sampleQueue = Queue.empty.enqueue(frame)
            clientFrame.frame.put(id,  sampleQueue)
          }
          else {
            val oldInfo = clientFrame.frame(id)
            clientFrame.frame.update(id, oldInfo.enqueue(frame))
          }
          Behaviors.same

        case RecordImage =>
          val frame = convert.convert(canvas)
          try{
            log.info("record image")
            val f = frame.clone()
            recorder4ts.recordImage(
              f.imageWidth,
              f.imageHeight,
              f.imageDepth,
              f.imageChannels,
              f.imageStride,
              recorder4ts.getPixelFormat,
              f.image: _*
            )
          }
          catch {
            case e: Exception =>
              log.info(s"record error: ${e.getMessage}")
          }
          Behaviors.same


        case t: DeleteImage4Others =>
          log.info(s"delete ${t.id}'s img")
          clientFrame.frame.-=(t.id)
          Behaviors.same

        case m@NewRecord4Ts(recorder4ts) =>
          log.info(s"got msg: $m")
          draw(canvas, graph, lastTime, hostFrame, clientFrame, recorder4ts, convert1, convert2,convert, converter4Others, layout, bgImg, roomId, canvasSize)

        case Close =>
          log.info(s"drawer stopped")
          recorder4ts.releaseUnsafe()
          Behaviors.stopped

        case t: SetLayout =>
          log.info(s"got msg: $t")
          draw(canvas, graph, lastTime, hostFrame, clientFrame, recorder4ts, convert1, convert2,convert, converter4Others, t.layout, bgImg, roomId, canvasSize)
      }
    }
  }

  def sampleRecord(
    roomId: Long,
    lastTime  : Ts4LastSample,
    clientFrame: Audio,
    recorder4ts: FFmpegFrameRecorder,
    ffFilter:  mutable.HashMap[Int, FFmpegFrameFilter],
    filterInUse: Option[FFmpegFrameFilter] = None,
    indexMap :mutable.HashMap[String, Int] = mutable.HashMap.empty,
    //    shieldMap :mutable.HashMap[String, State] = mutable.HashMap.empty,
  ): Behavior[SampleCommand] = {
    Behaviors.setup[SampleCommand] { ctx =>
      Behaviors.receiveMessage[SampleCommand] {

        case GetFFfilter(filters) =>
          sampleRecord(roomId, lastTime, clientFrame, recorder4ts, filters, filterInUse)

        case Sample4Host(hostLiveId, shieldMap, isOne, frame, isSilence) =>
          var newFilterInUse = filterInUse
          if (clientFrame.frame.get(hostLiveId).isDefined) {
            //            println(s"$liveId timeStamp: ${frame.timestamp}")
            if (ffFilter.nonEmpty) {
              //              println(online.size, shieldMap.count(_._2.audio) )
              if (isOne) {
                //                println(2, ffFilter)
                try {
                  clientFrame.frame.toList.filter(i => i._2._2.nonEmpty && shieldMap.filter(_._2.audio).contains(i._1)).sortBy(_._2._1).foreach {
                    f =>
                      val res = f._2._2.dequeue
                      clientFrame.frame.update(hostLiveId, (clientFrame.frame(hostLiveId)._1, res._2))
                      res._1 match {
                        case Left(v) =>
                          recorder4ts.recordSamples(v.sampleRate, v.audioChannels, v.samples: _*)

                        case Right(n) =>
                          recorder4ts.recordSamples(n.sampleRate, n.audioChannels, n.samples: _*)
                      }
                  }

//                  val samples = new Array[Short](sampleFrame.sampleRate)
//                  val sp = ShortBuffer.wrap(samples, 0, sampleFrame.sampleRate)
//                  if(isSilence) {
//                    recorder4ts.recordSamples(sampleFrame.sampleRate, sampleFrame.audioChannels, sp)
//                  } else
//                    recorder4ts.recordSamples(sampleFrame.sampleRate, sampleFrame.audioChannels, sampleFrame.samples: _*)
                }
                catch {
                  case ex: Exception =>
                    println(s"record solo sample error: $ex")
                }
              } else {
                val fil = ffFilter(shieldMap.count(_._2.audio))
                //                println(3, peopleOnline, fil)
                if (filterInUse.nonEmpty && fil != filterInUse.get) {
                  println(5, fil.getAudioInputs)
                  filterInUse.get.close()
                  fil.start()
                  newFilterInUse = Some(fil)
                } else if (filterInUse.isEmpty) {
                  println(6, fil.getAudioInputs)
                  fil.start()
                  newFilterInUse = Some(fil)
                }
                try {
                  //                  println(7, indexMap, fil, "index: " + (index - 1), sampleFrame.audioChannels, sampleFrame.sampleRate, fil.getSampleFormat)
                  //                  fil.pushSamples(indexMap(liveId), sampleFrame.audioChannels, sampleFrame.sampleRate, 1, sampleFrame.samples: _*)
                  //fil.pushSamples(0, sampleFrame.audioChannels, sampleFrame.sampleRate, fil.getSampleFormat, sampleFrame.samples: _*)
                  clientFrame.frame.toList.filter(i => i._2._2.nonEmpty && shieldMap.filter(_._2.audio).contains(i._1)).sortBy(_._2._1).foreach { f =>
                    val frameNew = f._2._2.dequeue
                    clientFrame.frame.update(f._1, (f._2._1, frameNew._2))
                    frameNew._1 match {
                      case Left(newFrame) =>
                        fil.pushSamples(f._2._1, newFrame.audioChannels, newFrame.sampleRate, fil.getSampleFormat, newFrame.samples: _*)

                      case Right(newSilenceFrame) =>
                        val samples = new Array[Short](newSilenceFrame.sampleRate)
                        val sp = ShortBuffer.wrap(samples, 0, newSilenceFrame.sampleRate)
                        fil.pushSamples(f._2._1, newSilenceFrame.audioChannels, newSilenceFrame.sampleRate, fil.getSampleFormat, sp)

                    }

                  }
                  val f = fil.pullSamples()
                  if (f != null) {
                    val ff = f.clone()
                    recorder4ts.recordSamples(ff.sampleRate, ff.audioChannels, ff.samples: _*)
                    //                    log.debug(s"record sample...")
                  }
                } catch {
                  case ex: Exception =>
                    println(s"record sample1 error: $ex")
                }
              }
            }
//            else {
//              try {
//                //                log.info(s"record audio: ${sampleFrame.sampleRate}")
//                val samples = new Array[Short](sampleFrame.samples.length)
//                val sp = ShortBuffer.wrap(samples, 0, sampleFrame.samples.length)
//                if(isSilence)
//                  recorder4ts.recordSamples(sampleFrame.sampleRate, sampleFrame.audioChannels, sp)
//                else
//                  recorder4ts.recordSamples(sampleFrame.sampleRate, sampleFrame.audioChannels, sampleFrame.samples: _*)
//              }
//              catch {
//                case e: Exception =>
//                  log.info(s"record sample2 error: ${e.getMessage}")
//              }
//            }
          }
          sampleRecord(roomId, lastTime, clientFrame, recorder4ts, ffFilter, newFilterInUse)

        case Sample4All(id, frame, isSilence) =>
          if (!clientFrame.frame.contains(id)) {
            val sampleQueue = Queue.empty.enqueue(if(!isSilence) Left(frame) else Right(frame))
            clientFrame.frame.put(id, (clientFrame.frame.size, sampleQueue))
            //            println("c1: " + clientFrame)
          }
          else {
            val oldInfo = clientFrame.frame(id)
            clientFrame.frame.update(id, (oldInfo._1, oldInfo._2.enqueue(if(!isSilence) Left(frame) else Right(frame))))
            //            println("c3: " + clientFrame)
          }
          Behaviors.same

        case t: DeleteSample4Others =>
          clientFrame.frame.-=(t.id)
          Behaviors.same


        case CloseSample =>
          log.info(s"sample recorder stopped")
          filterInUse.foreach(_.close())
          //          recorder4ts.releaseUnsafe()
          Behaviors.stopped

      }
    }
  }

}
////            println(s"$liveId timeStamp: ${frame.timestamp}")
//val sampleFrame = frame.clone()
//if (ffFilter.nonEmpty) {
//  //              println(1,"index: " + index)
//  //              println(online.size, shieldMap.count(_._2.audio) )
//  if (online.size == 1 || shieldMap.count(_._2.audio) == 1) {
//  //                println(2, ffFilter)
//  try {
//  recorder4ts.recordSamples(sampleFrame.sampleRate, sampleFrame.audioChannels, sampleFrame.samples: _*)
//}
//  catch {
//  case ex: Exception =>
//  println(s"record solo sample error: $ex")
//}
//  //                log.debug(s"record sample...")
//} else {
//  val fil = ffFilter(shieldMap.count(_._2.audio))
//  //                println(3, peopleOnline, fil)
//  if (filterInUse.nonEmpty && fil != filterInUse.get) {
//  println(5, fil.getAudioInputs)
//  filterInUse.get.close()
//  fil.start()
//  newFilterInUse = Some(fil)
//} else if (filterInUse.isEmpty) {
//  println(6, fil.getAudioInputs)
//  fil.start()
//  newFilterInUse = Some(fil)
//}
//  try {
//  if (indexMap.isEmpty && !indexMap.contains(liveId)) indexMap.put(liveId, 0)
//  else if (indexMap.nonEmpty && !indexMap.contains(liveId)) indexMap.put(liveId, indexMap.maxBy(_._2)._2 + 1)
//  println(7, indexMap, fil, "index: " + (index-1), sampleFrame.audioChannels, sampleFrame.sampleRate, fil.getSampleFormat)
//  fil.pushSamples(indexMap(liveId), sampleFrame.audioChannels, sampleFrame.sampleRate, fil.getSampleFormat, sampleFrame.samples: _*)
//  val f = fil.pullSamples()
//  if (f != null) {
//  val ff = f.clone()
//  recorder4ts.recordSamples(ff.sampleRate, ff.audioChannels, ff.samples: _*)
//  //                    log.debug(s"record sample...")
//}
//} catch {
//  case ex: Exception =>
//  println(s"record sample1 error: $ex")
//}
//
//}
//}
//  else {
//  try {
//  recorder4ts.recordSamples(sampleFrame.sampleRate, sampleFrame.audioChannels, sampleFrame.samples: _*)
//}
//  catch {
//  case e: Exception =>
//  log.info(s"record sample2 error: ${e.getMessage}")
//}
//}