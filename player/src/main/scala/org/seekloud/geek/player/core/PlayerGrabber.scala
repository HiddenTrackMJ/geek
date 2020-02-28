package org.seekloud.geek.player.core

import java.io.InputStream
import java.nio.{Buffer, ByteBuffer, ShortBuffer}
import java.util.concurrent.LinkedBlockingDeque

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{Behaviors, TimerScheduler}
import javafx.scene.canvas.GraphicsContext
import org.bytedeco.javacv.{FFmpegFrameGrabber, Frame}
import org.seekloud.geek.player.core.PlayerManager.MediaSettings
import org.seekloud.geek.player.processor.ImageConverter
import org.seekloud.geek.player.protocol.Messages
import org.seekloud.geek.player.protocol.Messages.{AddPicture, AddSamples, PictureFinish, SoundFinish}
import org.seekloud.geek.player.sdk.MediaPlayer
import org.seekloud.geek.player.sdk.MediaPlayer.executor
import org.slf4j.LoggerFactory

import concurrent.duration._
import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * User: TangYaruo
  * Date: 2019/8/28
  * Time: 15:25
  *
  * @author zwq
  */
object PlayerGrabber {

  private val log = LoggerFactory.getLogger(this.getClass)
  private var timeGetter: () => Long = _


  private var debug: Boolean = true

  private def debug(str: String): Unit = {
    if (debug) log.debug(str)
  }

  private var needTime: Boolean = false

  /*timer key*/
  final object GRAB_VIDEO_TIMER_KEY

  final object GRAB_AUDIO_TIMER_KEY


  /*monitor*/
  sealed trait MonitorCmd

  final case class StartedGrabber(grabber: FFmpegFrameGrabber, replyTo: ActorRef[Messages.RTCommand], graphContext: Option[GraphicsContext]) extends MonitorCmd

  final case class FailedStartGrabber(grabber: FFmpegFrameGrabber, replyTo: ActorRef[Messages.RTCommand], ex: Throwable) extends MonitorCmd

  final case object StartGrab extends MonitorCmd with WorkCmd

  final case object PauseGrab extends MonitorCmd with WorkCmd

  final case object ContinueGrab extends MonitorCmd with WorkCmd

  final case class StopGrab(resetFunc: () => Unit) extends MonitorCmd with WorkCmd

  final case class AskPicture(ref: Either[ActorRef[ImageActor.ImageCmd], ActorRef[Messages.RTCommand]]) extends MonitorCmd

  final case class AskSamples(ref: Either[ActorRef[SoundActor.SoundCmd], ActorRef[Messages.RTCommand]]) extends MonitorCmd

  private final object GrabbingFinish extends MonitorCmd

  final case object WorkerStopped extends MonitorCmd

  final case class StopWorkerTimeout(reSetFunc: () => Unit) extends MonitorCmd

  final case object StopSelf extends MonitorCmd

  final case class SetTimeGetter(func: () => Long) extends MonitorCmd with WorkCmd

  /*worker*/
  sealed trait WorkCmd

  final case object Work extends WorkCmd


  def create(
    id: String,
    replyTo: ActorRef[Messages.RTCommand],
    graphContext: Option[GraphicsContext],
    input: Either[String, InputStream],
    supervisor: ActorRef[PlayerManager.SupervisorCmd],
    settings: MediaSettings,
    isDebug: Boolean = true,
    needTimestamp: Boolean = true,
    timeGetter: () => Long
  ): Behavior[MonitorCmd] =
    Behaviors.setup[MonitorCmd] { ctx =>
      log.info(s"PlayerGrabber-$id is starting...")
      debug = isDebug
      needTime = needTimestamp
      val grabber = input match {
        case Left(rtmpString) =>
          new FFmpegFrameGrabber(rtmpString)
        case Right(inputStream) =>
          new FFmpegFrameGrabber(inputStream)
      }
      grabber.setFrameRate(settings.frameRate)
      //      grabber.setOption("fflags", "nobuffer")
      Future {
        log.info(s"grabber-$id is starting...")
        grabber.start()
        grabber
      }.onComplete {
        case Success(grab) =>
          log.info(s"grabber-$id started success.")
          ctx.self ! StartedGrabber(grab, replyTo, graphContext)
        case Failure(exception) =>
          log.info(s"grabber-$id start failed: $exception.")
          ctx.self ! FailedStartGrabber(grabber, replyTo, exception)
      }

      init(id, grabber, supervisor)
    }

  private def init(
    id: String,
    grabber: FFmpegFrameGrabber,
    supervisor: ActorRef[PlayerManager.SupervisorCmd],
  ): Behavior[MonitorCmd] =
    Behaviors.receive[MonitorCmd] { (ctx, msg) =>
      msg match {
        case SetTimeGetter(func) =>
          timeGetter = func
          Behaviors.same

        case StartedGrabber(grabber, replyTo, gc) =>
          val hasVideo = grabber.hasVideo
          val hasAudio = grabber.hasAudio
          val format = grabber.getFormat
          val frameRate = grabber.getFrameRate
          val width = grabber.getImageWidth
          val height = grabber.getImageHeight
          val sampleRate = grabber.getSampleRate
          val channels = grabber.getAudioChannels

          val mediaInfo = PlayerManager.MediaInfo(id, gc, replyTo, hasVideo, hasAudio, format, frameRate, width, height, sampleRate, channels)
          println(mediaInfo)
          supervisor ! mediaInfo

          val pQ = new java.util.concurrent.LinkedBlockingDeque[AddPicture]()
          val sQ = new java.util.concurrent.LinkedBlockingDeque[AddSamples]()

          //创建worker
          val workActor = ctx.spawn(worker(id, ctx.self, grabber, pQ, sQ), "worker", MediaPlayer.blockingDispatcher)
          ctx.self ! StartGrab

          Behaviors.withTimers[MonitorCmd] { implicit timer =>
            grabbing(id, grabber, supervisor, workActor, pQ, sQ)
          }

        case FailedStartGrabber(grab, replyTo, ex) =>
          log.info(s"grabber-$id init failed: ${ex.getMessage}")
          supervisor ! PlayerManager.GrabberFailInit(id, replyTo, ex)
          Behaviors.stopped


        case msg: StopGrab =>
          log.info(s"GrabActor-$id is stopped before started.")
          //          try {
          //            grabber.releaseUnsafe()
          //            msg.resetFunc()
          //          } catch {
          //            case ex: Exception =>
          //              log.debug(s"GrabActor-$id stop in init ex: $ex")
          //          }
          Behaviors.stopped

        case x =>
          log.warn(s"unknown msg in init: $x")
          Behaviors.unhandled

      }
    }


  private def grabbing(
    id: String,
    grabber: FFmpegFrameGrabber,
    supervisor: ActorRef[PlayerManager.SupervisorCmd],
    workActor: ActorRef[WorkCmd],
    pictureQueue: LinkedBlockingDeque[AddPicture],
    samplesQueue: LinkedBlockingDeque[AddSamples]
  )(
    implicit timer: TimerScheduler[MonitorCmd]
  ): Behavior[MonitorCmd] =
    Behaviors.receive[MonitorCmd] { (ctx, msg) =>

      msg match {
        case StartGrab =>
          workActor ! StartGrab
          log.info(s"worker start......")
          Behaviors.same

        case PauseGrab =>
          /*切换模式，暂停当前PlayerGrabber操作*/
          log.info(s"PlayerGrabber-$id got PauseGrab.")
          pictureQueue.clear()
          samplesQueue.clear()
          workActor ! PauseGrab
          Behaviors.same

        case ContinueGrab =>
          log.info(s"PlayerGrabber-$id got ContinueGrab.")
          workActor ! ContinueGrab
          Behaviors.same

        case msg: StopGrab =>
          log.info(s"PlayerGrabber-$id got StopGrab.")
          workActor ! msg
          timer.startSingleTimer(
            StopWorkerTimeout(msg.resetFunc),
            StopWorkerTimeout(msg.resetFunc),
            5.seconds
          )
          Behaviors.same

        case msg: StopWorkerTimeout =>
          log.info(s"PlayerGrabber-$id hard stop.")
          ctx.stop(workActor)
          timer.startSingleTimer(
            StopSelf,
            StopSelf,
            1.second
          )
          Behaviors.same

        case StopSelf =>
          log.info(s"PlayerGrabber-$id hard stopped.")
          pictureQueue.clear()
          samplesQueue.clear()
          try {
            log.info(s"Grabber-$id hard stopping...")
            //            grabber.releaseUnsafe()
            //            msg.reSetFunc()
            log.info(s"Grabber-$id hard stopped.")
          } catch {
            case ex: Exception =>
              log.error(s"grabber-$id hard stop ex: $ex")
          }
          Behaviors.stopped

        case WorkerStopped =>
          log.info(s"PlayerGrabber-$id is stopped.")
          pictureQueue.clear()
          samplesQueue.clear()
          Behaviors.stopped

        case m: AskPicture =>
          //          println(s"playerGrabber-$id get AskPicture,now queue size: ${pictureQueue.size()}")
          val pic = pictureQueue.poll()
          if (pic != null) {
            m.ref match {
              case Left(imageActor) =>
                imageActor ! pic
              case Right(replyToActor) =>
                replyToActor ! pic
            }
          } else {
            //            log.warn(s"no pic in grabbing.")
            //            if (!timer.isTimerActive(GRAB_VIDEO_TIMER_KEY)) {
            //              timer.startSingleTimer(
            //                GRAB_VIDEO_TIMER_KEY,
            //                m,
            //                5.millis
            //              )
            //            }
          }
          Behaviors.same

        case m: AskSamples =>
          //          println(s"playerGrabber-$id get AskSamples,now queue size: ${samplesQueue.size()}")
          val samples = samplesQueue.poll()
          if (samples != null) {
            m.ref match {
              case Left(soundActor) =>
                soundActor ! samples
              case Right(replyToActor) =>
                replyToActor ! samples
            }
          } else {
            //            log.warn(s"no sample in grabbing.")
            //            if (!timer.isTimerActive(GRAB_AUDIO_TIMER_KEY)) {
            //              timer.startSingleTimer(GRAB_AUDIO_TIMER_KEY, m, 5 millis)
            //            }
          }
          Behaviors.same


        case GrabbingFinish =>
          log.debug(s"grabber-$id get GrabbingFinish.")
          Behaviors.same

        case msg: SetTimeGetter =>
          workActor ! msg
          Behaviors.same

        case x =>
          log.warn(s"unknown msg in grabbing: $x")
          Behaviors.unhandled
      }
    }


  //  private def finished(
  //    pictureQueue: LinkedBlockingDeque[AddPicture],
  //    samplesQueue: LinkedBlockingDeque[AddSamples]
  //  ): Behavior[MonitorCmd] =
  //    Behaviors.receive[MonitorCmd] { (ctx, msg) =>
  //      msg match {
  //        case AskPicture(ref) =>
  //          val pic = pictureQueue.poll()
  //          if (pic != null) {
  //            ref match {
  //              case Left(imageActor) =>
  //                imageActor ! pic
  //              case Right(replyToActor) =>
  //                replyToActor ! pic
  //            }
  //          } else {
  //            ref match {
  //              case Left(imageActor) =>
  //                imageActor ! PictureFinish()
  //              case Right(replyToActor) =>
  //                replyToActor ! PictureFinish()
  //            }
  //          }
  //          Behaviors.same
  //        case AskSamples(ref) =>
  //          println("grabber got AskSamples")
  //          val samples = samplesQueue.poll()
  //          if (samples != null) {
  //            ref match {
  //              case Left(soundActor) =>
  //                soundActor ! samples
  //              case Right(replyToActor) =>
  //                replyToActor ! samples
  //            }
  //          } else {
  //            ref match {
  //              case Left(soundActor) =>
  //                soundActor ! SoundFinish
  //              case Right(replyToActor) =>
  //                replyToActor ! SoundFinish
  //            }
  //          }
  //          Behaviors.same
  //        case GrabbingFinish =>
  //          log.info("got GrabbingFinish Msg in finished")
  //          Behaviors.same
  //        case x =>
  //          log.warn(s"unknown msg in finish: $x")
  //          Behaviors.unhandled
  //      }
  //    }


  /*work*/
  private def worker(
    id: String,
    monitor: ActorRef[MonitorCmd],
    grabber: FFmpegFrameGrabber,
    pictureQueue: LinkedBlockingDeque[AddPicture],
    samplesQueue: LinkedBlockingDeque[AddSamples],
    isWorking: Boolean = true
  ): Behavior[WorkCmd] = {
    log.debug(s"worker-$id is starting...")

    //    val imgConverter = new JavaFXFrameConverter2()
    val imgConverter = new ImageConverter
    if (needTime) imgConverter.setNeedTimestamp()
    imgConverter.setTimeGetter(timeGetter)

    val hasVideo = grabber.hasVideo
    val hasAudio = grabber.hasAudio

    println(s"has video: $hasVideo , has audio: $hasAudio")

    var dataBuf = ByteBuffer.allocateDirect(4096) //init 4096
    var shortView = dataBuf.asShortBuffer() //4096 byte => 2048 short
    var dst = new Array[Byte](4096)
    var nowLength = 4096

    @inline
    def frameSamples2ByteArray(
      samples: Array[Buffer]
    ) = {
      val sampleBuf = samples(0).asInstanceOf[ShortBuffer]
      //      println("samples音频长度：" + samples.length)
      //      println("sampleBuf音频ShortBuffer:" + sampleBuf.capacity(),sampleBuf.limit(),sampleBuf.position(),sampleBuf.remaining())
      //      println("dataBuf: capacity " + dataBuf.capacity())

      val byteDataLength = sampleBuf.remaining() * 2
      if (byteDataLength > dataBuf.capacity()) {
        println("dataBuf allocateDirect expand")
        dataBuf = ByteBuffer.allocateDirect(byteDataLength)
        shortView = dataBuf.asShortBuffer()
      }

      shortView.clear()

      shortView.put(sampleBuf)
      dataBuf.position(0).limit(byteDataLength)
      if(byteDataLength != nowLength) {
        dst = new Array[Byte](byteDataLength)
        nowLength = byteDataLength
      }
      dataBuf.get(dst)
      dst
    }

    @inline
    def bufferSample(frame: Frame) = {
      if (frame != null) {
        if (frame.samples != null) {
          //          log.debug(s"grabber-$id grab samples.")
          var sampleData = AddSamples(frameSamples2ByteArray(frame.samples), frame.timestamp)
          samplesQueue.offer(sampleData)
          sampleData = null
          //          if(samplesQueue.size() > 5) samplesQueue.clear()
        } else {
          log.warn("warning: no samples to buffer.")
        }
      }
      //      println(s"PlayGrabber samplesQueue add new, now size: ${samplesQueue.size()}")
    }

    var picCount = 0

    @inline
    def bufferPicture(frame: Frame) = {
      if (frame != null) {
        if (frame.image != null) {
          //          if (picCount % 100 == 0) {
          //            log.debug(s"--------------- grab frameRate: ${grabber.getFrameRate}")
          //          }
          picCount += 1
          //          log.debug(s"grabber-$id grab bufferPicture")
          //          val ts1 = System.currentTimeMillis()
          val img = imgConverter.convert(frame)
          //          val ts2 = System.currentTimeMillis() -ts1
          //          debug(s"$ts2..........$img")
          //          println(s"img w :${img.getWidth}, h: ${img.getHeight}")
          pictureQueue.offer(AddPicture(img, frame.timestamp))
        } else {
          log.warn("warning: no picture to buffer.")
        }
      }
      //      println(s"PlayGrabber pictureQueue add new, now size: ${pictureQueue.size()}")
    }

    var grabFinish = false

    Behaviors.withTimers[WorkCmd] { timer =>
      log.debug(s"worker-$id is prepared.")
      Behaviors.receive[WorkCmd] { (ctx, msg) =>
        msg match {
          case StartGrab =>
            //            log.debug(s"worker-$id work.")
            try {
              if (isWorking) {
                if (!grabFinish) {
                  if (hasVideo && pictureQueue.isEmpty && !grabFinish) {
                    var frame = grabber.grab()
                    while (frame != null && frame.image == null) {
                      bufferSample(frame)
                      frame = grabber.grab()
                    }
                    grabFinish = frame == null
                    bufferPicture(frame)
                  }

                  if (hasAudio && samplesQueue.isEmpty && !grabFinish) {
                    var frame = grabber.grab()
                    while (frame != null && frame.samples == null) {
                      bufferPicture(frame)
                      frame = grabber.grab()
                    }
                    grabFinish = frame == null
                    bufferSample(frame)
                  }
                  ctx.self ! StartGrab
                } else {
                  log.info(s"grab-$id finish: [$grabFinish]")
                  monitor ! GrabbingFinish
                  grabFinish = false //重试抓取
                }
              }
            } catch {
              case ex: Exception =>
                log.warn(s"worker grab ex: $ex")
            }
            Behaviors.same

          case PauseGrab =>
            log.info(s"worker-$id is paused.")
            worker(
              id,
              monitor,
              grabber,
              pictureQueue,
              samplesQueue,
              isWorking = false
            )

          case ContinueGrab =>
            log.info(s"worker-$id is continued.")
            ctx.self ! StartGrab
            worker(
              id,
              monitor,
              grabber,
              pictureQueue,
              samplesQueue
            )

          case msg: StopGrab =>
            log.info(s"worker-$id got StopGrab.")
            try {
              grabber.stop()
              grabber.close()
              grabber.release()
              msg.resetFunc()
            } catch {
              case ex: Exception =>
                log.error(s"PlayerGrabber-$id close error: $ex")
            }
            monitor ! WorkerStopped
            Behaviors.stopped

          case SetTimeGetter(func) =>
            imgConverter.setTimeGetter(func)
            Behaviors.same

          case x =>
            log.warn(s"unknown msg in worker: $x")
            Behaviors.unhandled
        }
      }
    }
  }


}
