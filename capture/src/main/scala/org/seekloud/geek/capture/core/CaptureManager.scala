package org.seekloud.geek.capture.core

import java.io.{File, OutputStream}
import java.util.concurrent.LinkedBlockingDeque

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import javax.sound.sampled._
import org.bytedeco.ffmpeg.global.{avcodec, avutil}
import org.bytedeco.ffmpeg.global.avcodec._
import org.bytedeco.javacv.{FFmpegFrameGrabber, FFmpegFrameRecorder, JavaFXFrameConverter1, OpenCVFrameGrabber}
import org.seekloud.geek.capture.processor.ImageConverter
import org.seekloud.geek.capture.protocol.Messages
import org.seekloud.geek.capture.protocol.Messages._
import org.seekloud.geek.capture.sdk.MediaCapture.{blockingDispatcher, executor}
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import concurrent.duration._
import language.postfixOps

/**
  * User: TangYaruo
  * Date: 2019/8/28
  * Time: 11:57
  */
object CaptureManager {

  private val log = LoggerFactory.getLogger(this.getClass)
  private var debug: Boolean = true
  private var needTimeMark: Boolean = false
  var timeGetter: () => Long = _

  private def debug(msg: String): Unit = {
    if (debug) log.debug(msg)
  }

  /*Data*/
  private val latestFrame = new java.util.concurrent.LinkedBlockingDeque[Messages.LatestFrame](1)
  //  private val latestSound = new java.util.concurrent.LinkedBlockingDeque[Messages.LatestSound]()
  def setLatestFrame(): Unit = latestFrame.clear()

  private val imageConverter = new ImageConverter

  //  private val imageConverter = new JavaFXFrameConverter()

  trait Command

  case class MediaSettings(
    imageWidth: Int,
    imageHeight: Int,
    frameRate: Int,
    outputBitrate: Int,
    needImage: Boolean,
    sampleRate: Float,
    sampleSizeInBits: Int,
    channels: Int,
    needSound: Boolean,
    audioCodec: Int,
    videoCodec: Int,
    camDeviceIndex: Int,
    audioDeviceIndex: Int
  )

  private case class ChildDead[U](name: String, childRef: ActorRef[U]) extends Command

  final case class CameraGrabberStarted(grabber: OpenCVFrameGrabber, only: Boolean) extends Command

  final case class StartCameraFailed(ex: Throwable) extends Command

  final case class DesktopGrabberStarted(grabber: FFmpegFrameGrabber, only: Boolean) extends Command

  final case class StartedDesktopFailed(ex: Throwable) extends Command

  final case class TargetDataLineStarted(line: TargetDataLine) extends Command

  final case class StartTargetDataLineFailed(ex: Throwable) extends Command

  final case class SetTimerGetter(func: () => Long) extends Command

  final case class CameraPosition(position: Int) extends Command

  final case object StartCapture extends Command

  final case object StopCapture extends Command

  final case object StopDelay extends Command

  final case object ShowDesktop extends Command

  final case object ShowPerson extends Command

  final case object ShowBoth extends Command

  private object STOP_DELAY_TIMER_KEY


  def create(
    replyTo: ActorRef[Messages.ReplyToCommand],
    mediaSettings: MediaSettings,
    outputStream: Option[OutputStream],
    outputFile: Option[File],
    isDebug: Boolean,
    needTimestamp: Boolean
  ): Behavior[Command] =
    Behaviors.setup[Command] { ctx =>
      log.info(s"CaptureManager is starting...")
      debug = isDebug
      needTimeMark = needTimestamp
      if(needTimestamp) imageConverter.setNeedTimestamp()
      implicit val stashBuffer: StashBuffer[Command] = StashBuffer[Command](Int.MaxValue)

      if (mediaSettings.needImage) {

        val cameraGrabber = new OpenCVFrameGrabber(mediaSettings.camDeviceIndex)
        cameraGrabber.setImageWidth(mediaSettings.imageWidth)
        cameraGrabber.setImageHeight(mediaSettings.imageHeight)

        Future {
          debug(s"cameraGrabber-${mediaSettings.camDeviceIndex} is starting...")
          cameraGrabber.start()
          debug(s"cameraGrabber-${mediaSettings.camDeviceIndex} started.")
          cameraGrabber
        }.onComplete {
          case Success(grabber) => ctx.self ! CameraGrabberStarted(grabber, true)
          case Failure(ex) => ctx.self ! StartCameraFailed(ex)
        }
      }

      if (mediaSettings.needSound) {
        val audioFormat = new AudioFormat(mediaSettings.sampleRate, mediaSettings.sampleSizeInBits, mediaSettings.channels, true, false)
        val minfoSet: Array[Mixer.Info] = AudioSystem.getMixerInfo
        val mixer: Mixer = AudioSystem.getMixer(minfoSet(mediaSettings.audioDeviceIndex))
        val dataLineInfo = new DataLine.Info(classOf[TargetDataLine], audioFormat)

        Future {
          val line = AudioSystem.getLine(dataLineInfo).asInstanceOf[TargetDataLine]
          line.open(audioFormat)
          line.start()
          line
        }.onComplete {
          case Success(line) => ctx.self ! TargetDataLineStarted(line)
          case Failure(ex) => ctx.self ! StartTargetDataLineFailed(ex)
        }
      }

      Behaviors.withTimers[Command] { implicit timer =>
        init(replyTo, mediaSettings, outputStream, outputFile)
      }
    }


  private def init(
    replyTo: ActorRef[Messages.ReplyToCommand],
    mediaSettings: MediaSettings,
    outputStream: Option[OutputStream],
    outputFile: Option[File],
    grabber: Option[OpenCVFrameGrabber] = None,
    line: Option[TargetDataLine] = None,
    imageFail: Boolean = false,
    soundFail: Boolean = false
  )(
    implicit stashBuffer: StashBuffer[Command],
    timer: TimerScheduler[Command]
  ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case msg: SetTimerGetter =>
          timeGetter = msg.func
          imageConverter.setTimeGetter(msg.func)
          Behaviors.same

        case CameraGrabberStarted(g, only) =>
          log.info(s"Start camera success.")
          if (line.nonEmpty || soundFail || !mediaSettings.needSound) {
            ctx.self ! StartCapture
            if (soundFail)
              replyTo ! Messages.CannotAccessSound(ctx.self)
            else
              replyTo ! Messages.CaptureStartSuccess(ctx.self)
          }
          init(replyTo, mediaSettings, outputStream, outputFile, Some(g), line, imageFail, soundFail)

        case TargetDataLineStarted(l) =>
          log.info(s"Start targetDataLine success.")
          if (grabber.nonEmpty || imageFail || !mediaSettings.needImage) {
            ctx.self ! StartCapture
            if (imageFail)
              replyTo ! Messages.CannotAccessImage(ctx.self)
            else
              replyTo ! Messages.CaptureStartSuccess(ctx.self)
          }
          init(replyTo, mediaSettings, outputStream, outputFile, grabber, Some(l), imageFail, soundFail)

        case StartCameraFailed(ex) =>
          log.info(s"Start Camera failed: $ex")
          if (line.nonEmpty) {
            ctx.self ! StartCapture
            replyTo ! Messages.CannotAccessImage(ctx.self)
          }
          else if (soundFail || !mediaSettings.needSound) {
            ctx.self ! StopCapture
          }
          init(replyTo, mediaSettings, outputStream, outputFile, grabber, line, imageFail = true, soundFail)

        case StartTargetDataLineFailed(ex) =>
          log.info(s"Start targetDataLine failed: $ex")
          if (grabber.nonEmpty) {
            ctx.self ! StartCapture
            replyTo ! Messages.CannotAccessSound(ctx.self)
          }
          else if (imageFail || !mediaSettings.needImage) {
            ctx.self ! StopCapture
          }
          init(replyTo, mediaSettings, outputStream, outputFile, grabber, line, imageFail, soundFail = true)


        case StartCapture =>
          log.info("流程: StartCapture")
          val encodeActorMap = mutable.HashMap[EncoderType.Value, ActorRef[EncodeActor.Command]]()
          val montageActor = getMontageActor(ctx, latestFrame, mediaSettings)

          val imageCaptureOpt = if (grabber.nonEmpty) {
            Some(getImageCapture(ctx, grabber.get, montageActor, mediaSettings.frameRate, debug))
          } else None

          val soundCaptureOpt = if (line.nonEmpty) {
            Some(getSoundCapture(ctx, replyTo, line.get, encodeActorMap, mediaSettings.frameRate, mediaSettings.sampleRate, mediaSettings.channels, mediaSettings.sampleSizeInBits, debug))
          } else None

          if (outputStream.nonEmpty) {
            val streamEncoder = if (grabber.nonEmpty && line.isEmpty) { // image only
              new FFmpegFrameRecorder(outputStream.get, grabber.get.getImageWidth, grabber.get.getImageHeight)
            } else if (grabber.isEmpty && line.nonEmpty) { //sound only
              new FFmpegFrameRecorder(outputStream.get, mediaSettings.channels)
            } else {
              new FFmpegFrameRecorder(outputStream.get, grabber.get.getImageWidth, grabber.get.getImageHeight, mediaSettings.channels)
            }
            setEncoder(ctx, mediaSettings, streamEncoder, EncoderType.STREAM, imageCaptureOpt, soundCaptureOpt, encodeActorMap, replyTo)
          }

          if (outputFile.nonEmpty) {
            val src = "rtmp://10.1.29.247:1935/live/1000_4"

//            val fileEncoder = if (grabber.nonEmpty && line.isEmpty) { // image only
//              new FFmpegFrameRecorder(src, grabber.get.getImageWidth, grabber.get.getImageHeight)
//            } else if (grabber.isEmpty && line.nonEmpty) { //sound only
//              new FFmpegFrameRecorder(src, mediaSettings.channels)
//            } else {
//              new FFmpegFrameRecorder(src, grabber.get.getImageWidth, grabber.get.getImageHeight, mediaSettings.channels)
//            }
            avutil.av_log_set_level(-8)

            val fileEncoder = new FFmpegFrameRecorder(src, grabber.get.getImageWidth, grabber.get.getImageHeight, mediaSettings.channels)
            fileEncoder.setInterleaved(true)

            setEncoder(ctx, mediaSettings, fileEncoder, EncoderType.FILE, imageCaptureOpt, soundCaptureOpt, encodeActorMap, replyTo)
          }

          stashBuffer.unstashAll(ctx, idle(replyTo, mediaSettings, grabber, line, imageCaptureOpt, None, montageActor, soundCaptureOpt, encodeActorMap))

        case StopCapture =>
          log.info(s"CaptureManager stopped in init.")
          replyTo ! Messages.CaptureStartFailed
          Behaviors.stopped

        case StopMediaCapture =>
          log.info(s"Your stop command executed. Capture stopped in init.")
          try {
            grabber.foreach(_.close())
            line.foreach {
              l =>
                l.stop()
                l.flush()
                l.close()
            }
          } catch {
            case ex: Exception =>
              log.warn(s"release resources in init error: $ex")
          }
          Behaviors.stopped

        case x =>
          log.warn(s"unknown msg in init: $x")
          stashBuffer.stash(x)
          Behaviors.same
      }
    }

  private def idle(
    replyTo: ActorRef[Messages.ReplyToCommand],
    mediaSettings: MediaSettings,
    grabber: Option[OpenCVFrameGrabber] = None,
    line: Option[TargetDataLine] = None,
    imageCaptureOpt: Option[ActorRef[ImageCapture.Command]] = None,
    desktopCaptureOpt: Option[ActorRef[DesktopCapture.Command]] = None,
    montageActor: ActorRef[MontageActor.Command],
    soundCaptureOpt: Option[ActorRef[SoundCapture.Command]] = None,
    encodeActorMap: mutable.HashMap[EncoderType.Value, ActorRef[EncodeActor.Command]])(
    implicit stashBuffer: StashBuffer[Command],
    timer: TimerScheduler[Command]
  ): Behavior[Command] = {
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case msg: SetTimerGetter =>
          timeGetter = msg.func
          imageConverter.setTimeGetter(msg.func)
          Behaviors.same

        case AskImage =>
          val targetImage = latestFrame.peek()
          if (targetImage != null) {
            val image = imageConverter.convert(targetImage.frame.clone())
            replyTo ! Messages.ImageRsp(LatestImage(image, System.currentTimeMillis()))
          } else {
//            log.info(s"No image captured yet.")
            replyTo ! Messages.NoImage
          }

          Behaviors.same

        case CameraPosition(position) =>
          montageActor ! MontageActor.CameraPosition(position)
          Behaviors.same

        case AskSamples =>
          soundCaptureOpt.foreach(_ ! SoundCapture.AskSamples)
          Behaviors.same

        case msg: StartEncodeStream =>
          log.info(s"Start encode stream.")
          val streamEncoder = if (grabber.nonEmpty && line.isEmpty) { // image only
            new FFmpegFrameRecorder(msg.outputStream, grabber.get.getImageWidth, grabber.get.getImageHeight)
          } else if (grabber.isEmpty && line.nonEmpty) { //sound only
            new FFmpegFrameRecorder(msg.outputStream, mediaSettings.channels)
          } else {
            new FFmpegFrameRecorder(msg.outputStream, grabber.get.getImageWidth, grabber.get.getImageHeight, mediaSettings.channels)
          }
          setEncoder(ctx, mediaSettings, streamEncoder, EncoderType.STREAM, imageCaptureOpt, soundCaptureOpt, encodeActorMap, replyTo)
          Behaviors.same

        case StopEncodeStream =>
          encodeActorMap.get(EncoderType.STREAM).foreach(_ ! EncodeActor.StopEncode)
          encodeActorMap.remove(EncoderType.STREAM)
          Behaviors.same

        case CameraGrabberStarted(grabber1, only) =>
          if(only){
            if(desktopCaptureOpt.nonEmpty){
              desktopCaptureOpt.foreach(_ ! DesktopCapture.StopGrab)
            }
            montageActor ! MontageActor.ShowCamera
          }else{
            if(desktopCaptureOpt.nonEmpty) montageActor ! MontageActor.ShowBoth
          }
          val desktop = if(only) None else desktopCaptureOpt
          val imageCaptureOpt1 =
            Some(getImageCapture(ctx, grabber1, montageActor, mediaSettings.frameRate, debug))
          idle(
            replyTo,
            mediaSettings,
            grabber,
            line,
            imageCaptureOpt1,
            desktop,
            montageActor,
            soundCaptureOpt,
            encodeActorMap)

        case DesktopGrabberStarted(grabber1, only) =>
          if(only){
            if(imageCaptureOpt.nonEmpty){
              imageCaptureOpt.foreach(_ ! ImageCapture.StopCamera)
            }
            montageActor ! MontageActor.ShowDesktop
          }else{
            if(imageCaptureOpt.nonEmpty) montageActor ! MontageActor.ShowBoth
          }
          val camera = if(only) None else imageCaptureOpt
          val desktopCaptureOpt1 =
            Some(getDesktopCapture(ctx, grabber1, montageActor, mediaSettings.frameRate, debug))
          idle(
            replyTo,
            mediaSettings,
            grabber,
            line,
            camera,
            desktopCaptureOpt1,
            montageActor,
            soundCaptureOpt,
            encodeActorMap)


        case StartCameraFailed(ex) =>
          replyTo ! Messages.CannotAccessImage(ctx.self)
          Behaviors.same

        case StartedDesktopFailed(ex) =>
          replyTo ! Messages.CannotAccessDesktop(ctx.self)
          Behaviors.same

        case ShowPerson =>
          if(imageCaptureOpt.nonEmpty){
            if(desktopCaptureOpt.nonEmpty){
              desktopCaptureOpt.foreach(_ ! DesktopCapture.StopGrab)
            }
            montageActor ! MontageActor.ShowCamera
            idle(
              replyTo,
              mediaSettings,
              grabber,
              line,
              imageCaptureOpt,
              None,
              montageActor,
              soundCaptureOpt,
              encodeActorMap)
          }else{
            if (mediaSettings.needImage){
              Future {
                val cameraGrabber = new OpenCVFrameGrabber(mediaSettings.camDeviceIndex)
                cameraGrabber.setImageWidth(mediaSettings.imageWidth)
                cameraGrabber.setImageHeight(mediaSettings.imageHeight)
                debug(s"cameraGrabber-${mediaSettings.camDeviceIndex} is starting...")
                cameraGrabber.start()
                debug(s"cameraGrabber-${mediaSettings.camDeviceIndex} started.")
                cameraGrabber
              }.onComplete {
                case Success(grabber) => ctx.self ! CameraGrabberStarted(grabber, true)
                case Failure(ex) => ctx.self ! StartCameraFailed(ex)
              }
            }
            Behaviors.same
          }

        case ShowDesktop =>
          if(desktopCaptureOpt.nonEmpty) {
            if (imageCaptureOpt.nonEmpty) {
              imageCaptureOpt.foreach(_ ! ImageCapture.StopCamera)
            }
            montageActor ! MontageActor.ShowDesktop
            idle(
              replyTo,
              mediaSettings,
              grabber,
              line,
              None,
              desktopCaptureOpt,
              montageActor,
              soundCaptureOpt,
              encodeActorMap)
          }else{
            if (mediaSettings.needImage){
              Future{
                val desktopGrabber = new FFmpegFrameGrabber("desktop")
                desktopGrabber.setFormat("gdigrab")
                debug(s"desktopGrabber is starting...")
                desktopGrabber.start()
                debug(s"desktopGrabber started.")
                desktopGrabber
              }.onComplete{
                case Success(grabber) => ctx.self ! DesktopGrabberStarted(grabber, true)
                case Failure(ex) => ctx.self ! StartedDesktopFailed(ex)
              }
            }
            Behaviors.same
          }


        case ShowBoth =>
          if(desktopCaptureOpt.isDefined && imageCaptureOpt.isDefined){
            montageActor ! MontageActor.ShowBoth
          }else{
            if(desktopCaptureOpt.isEmpty){
              Future{
                val desktopGrabber = new FFmpegFrameGrabber("desktop")
                desktopGrabber.setFormat("gdigrab")
                debug(s"desktopGrabber is starting...")
                desktopGrabber.start()
                debug(s"desktopGrabber started.")
                desktopGrabber
              }.onComplete{
                case Success(grabber) => ctx.self ! DesktopGrabberStarted(grabber, false)
                case Failure(ex) => ctx.self ! StartedDesktopFailed(ex)
              }
            }
            if(imageCaptureOpt.isEmpty){
              Future {
                val cameraGrabber = new OpenCVFrameGrabber(mediaSettings.camDeviceIndex)
                cameraGrabber.setImageWidth(mediaSettings.imageWidth)
                cameraGrabber.setImageHeight(mediaSettings.imageHeight)
                debug(s"cameraGrabber-${mediaSettings.camDeviceIndex} is starting...")
                cameraGrabber.start()
                debug(s"cameraGrabber-${mediaSettings.camDeviceIndex} started.")
                cameraGrabber
              }.onComplete {
                case Success(grabber) => ctx.self ! CameraGrabberStarted(grabber, false)
                case Failure(ex) => ctx.self ! StartCameraFailed(ex)
              }
            }
          }
          Behaviors.same

        case msg: StartEncodeFile =>
//          val fileEncoder = if (grabber.nonEmpty && line.isEmpty) { // image only
//            new FFmpegFrameRecorder(msg.file,  640, 480)
//          } else if (grabber.isEmpty && line.nonEmpty) { //sound only
//            new FFmpegFrameRecorder(msg.file, mediaSettings.channels)
//          } else {
//            new FFmpegFrameRecorder(msg.file, grabber.get.getImageWidth, grabber.get.getImageHeight, mediaSettings.channels)
//          }
          val fileEncoder = new FFmpegFrameRecorder(msg.file, 640, 480, mediaSettings.channels)

          setEncoder(ctx, mediaSettings, fileEncoder, EncoderType.FILE, imageCaptureOpt, soundCaptureOpt, encodeActorMap, replyTo)
          Behaviors.same

        case msg:StartEncodeRtmp =>
//          val fileEncoder = if (grabber.nonEmpty && line.isEmpty) { // image only
//            new FFmpegFrameRecorder(msg.rtmpDes, grabber.get.getImageWidth, grabber.get.getImageHeight)
//          } else if (grabber.isEmpty && line.nonEmpty) { //sound only
//            new FFmpegFrameRecorder(msg.rtmpDes, mediaSettings.channels)
//          } else {
//            new FFmpegFrameRecorder(msg.rtmpDes, grabber.get.getImageWidth, grabber.get.getImageHeight, mediaSettings.channels)
//          }
          avutil.av_log_set_level(-8)
          val fileEncoder = new FFmpegFrameRecorder(msg.rtmpDes, grabber.get.getImageWidth, grabber.get.getImageHeight, mediaSettings.channels)
          fileEncoder.setInterleaved(true)
          setEncoder(ctx, mediaSettings, fileEncoder, EncoderType.FILE, imageCaptureOpt, soundCaptureOpt, encodeActorMap, replyTo)
          Behaviors.same


        case StopEncodeFile =>
          encodeActorMap.get(EncoderType.FILE).foreach(_ ! EncodeActor.StopEncode)
          encodeActorMap.remove(EncoderType.FILE)
          Behaviors.same

        case StopCapture =>
          log.info(s"CaptureManager stopped in idle.")
          imageCaptureOpt.foreach(_ ! ImageCapture.StopCamera)
          soundCaptureOpt.foreach(_ ! SoundCapture.StopSample)
          encodeActorMap.foreach(_._2 ! EncodeActor.StopEncode)
          desktopCaptureOpt.foreach(_ ! DesktopCapture.StopGrab)
          montageActor ! MontageActor.Stop
          timer.startSingleTimer(STOP_DELAY_TIMER_KEY, StopDelay, 1.second)
          Behaviors.same

        case StopMediaCapture =>
          log.info(s"Your stop command executed. Capture stopping in idle.")
          imageCaptureOpt.foreach(_ ! ImageCapture.StopCamera)
          soundCaptureOpt.foreach(_ ! SoundCapture.StopSample)
          encodeActorMap.foreach(_._2 ! EncodeActor.StopEncode)
          desktopCaptureOpt.foreach(_ ! DesktopCapture.StopGrab)
          montageActor ! MontageActor.Stop
          timer.startSingleTimer(STOP_DELAY_TIMER_KEY, StopDelay, 1.second)
          Behaviors.same

        case StopDelay =>
          log.info(s"Capture Manager stopped.")
          replyTo ! Messages.ManagerStopped
          Behaviors.stopped

        case ChildDead(child, childRef) =>
          debug(s"CaptureManager unWatch child-$child")
          ctx.unwatch(childRef)
          Behaviors.same


        case x =>
          log.warn(s"unknown msg in idle: $x")
          Behaviors.unhandled
      }
    }
  }

  def setEncoder(
    ctx: ActorContext[Command],
    mediaSettings: MediaSettings,
    encoder: FFmpegFrameRecorder,
    encoderType: EncoderType.Value,
    imageCaptureOpt: Option[ActorRef[ImageCapture.Command]] = None,
    soundCaptureOpt: Option[ActorRef[SoundCapture.Command]] = None,
    encodeActorMap: mutable.HashMap[EncoderType.Value, ActorRef[EncodeActor.Command]],
    replyTo: ActorRef[Messages.ReplyToCommand]
  ): Unit = {

    log.info("流程：setEncoder")
//    encoder.setFormat("mpegts")
//
//    /*video*/
//    encoder.setVideoOption("tune", "zerolatency")
//    encoder.setVideoOption("preset", "ultrafast")
//    encoder.setVideoOption("crf", "25")
//    //    encoder.setVideoOption("keyint", "1")
//    encoder.setVideoBitrate(mediaSettings.outputBitrate)
//    encoder.setVideoCodec(mediaSettings.videoCodec)
//    encoder.setFrameRate(mediaSettings.frameRate)
//
//    /*audio*/
//    encoder.setAudioOption("crf", "0")
//    encoder.setAudioQuality(0)
//    encoder.setAudioBitrate(192000)
//    encoder.setSampleRate(mediaSettings.sampleRate.toInt)
//    encoder.setAudioChannels(mediaSettings.channels)
//    encoder.setAudioCodec(mediaSettings.audioCodec)

//    encoder.setInterleaved(true)
    encoder.setFrameRate(mediaSettings.frameRate)
    encoder.setVideoBitrate(mediaSettings.outputBitrate)
    //          recorder4ts.setVideoCodec(avcodec.AV_CODEC_ID_MPEG2VIDEO)
    //          recorder4ts.setAudioCodec(avcodec.AV_CODEC_ID_MP2)
    //          recorder4ts.setVideoOption("preset","ultrafast")
    encoder.setVideoOption("crf", "25")
    encoder.setAudioQuality(0)
    encoder.setSampleRate(44100)
    encoder.setMaxBFrames(0)
    //          recorder4ts.setFormat("mpegts")
    encoder.setVideoCodec(avcodec.AV_CODEC_ID_H264)
    encoder.setAudioCodec(avcodec.AV_CODEC_ID_AAC)
    encoder.setFormat("flv")
    encoderType match {
      case EncoderType.STREAM =>
        log.info(s"streamEncoder start success.")
        val encodeActor = getEncodeActor(ctx, replyTo, EncoderType.STREAM, encoder, latestFrame, mediaSettings.needImage, mediaSettings.needSound, debug)
        encodeActorMap.put(EncoderType.STREAM, encodeActor)
      case EncoderType.FILE =>
        log.info(s"fileEncoder start success.")
        val encodeActor = getEncodeActor(ctx, replyTo, EncoderType.FILE, encoder, latestFrame, mediaSettings.needImage, mediaSettings.needSound, debug)
        encodeActorMap.put(EncoderType.FILE, encodeActor)
      case EncoderType.RTMP =>
        log.info(s"rtmpEncoder start success.")
        val encodeActor = getEncodeActor(ctx, replyTo, EncoderType.RTMP, encoder, latestFrame, mediaSettings.needImage, mediaSettings.needSound, debug)
        encodeActorMap.put(EncoderType.RTMP, encodeActor)
    }
  }


  private def getImageCapture(
    ctx: ActorContext[Command],
    grabber: OpenCVFrameGrabber,
    montageActor: ActorRef[MontageActor.Command],
    frameRate: Int,
    debug: Boolean
  ) = {
    val childName = "ImageCapture"
    ctx.child(childName).getOrElse {
      val actor = ctx.spawn(ImageCapture.create(grabber, frameRate, debug, montageActor), childName, blockingDispatcher)
      ctx.watchWith(actor, ChildDead(childName, actor))
      actor
    }.unsafeUpcast[ImageCapture.Command]
  }

  private def getDesktopCapture(
    ctx: ActorContext[Command],
    grabber: FFmpegFrameGrabber,
    montageActor: ActorRef[MontageActor.Command],
    frameRate: Int,
    debug: Boolean
  ) = {
    val childName = "DesktopCapture"
    ctx.child(childName).getOrElse {
      val actor = ctx.spawn(DesktopCapture.create(grabber, frameRate, debug, montageActor), childName, blockingDispatcher)
      ctx.watchWith(actor, ChildDead(childName, actor))
      actor
    }.unsafeUpcast[DesktopCapture.Command]
  }

  private def getMontageActor(
    ctx: ActorContext[Command],
    imageQueue: LinkedBlockingDeque[LatestFrame],
    mediaSettings: MediaSettings
  ) = {
    val childName = "MontageActor"
    ctx.child(childName).getOrElse{
      val actor = ctx.spawn(MontageActor.create(imageQueue, mediaSettings), childName, blockingDispatcher)
      ctx.watchWith(actor, ChildDead(childName, actor))
      actor
    }.unsafeUpcast[MontageActor.Command]
  }

  private def getSoundCapture(
    ctx: ActorContext[Command],
    replyTo: ActorRef[Messages.ReplyToCommand],
    line: TargetDataLine,
    //    soundQueue: LinkedBlockingDeque[LatestSound],
    encoders: mutable.HashMap[EncoderType.Value, ActorRef[EncodeActor.Command]],
    frameRate: Int,
    sampleRate: Float,
    channels: Int,
    sampleSize: Int,
    debug: Boolean
  ) = {
    val childName = "SoundCapture"
    ctx.child(childName).getOrElse {
      val actor = ctx.spawn(SoundCapture.create(replyTo, line, encoders, frameRate, sampleRate, channels, sampleSize, debug), childName, blockingDispatcher)
      ctx.watchWith(actor, ChildDead(childName, actor))
      actor
    }.unsafeUpcast[SoundCapture.Command]
  }

  private def getEncodeActor(
    ctx: ActorContext[Command],
    replyTo: ActorRef[Messages.ReplyToCommand],
    encodeType: EncoderType.Value,
    encoder: FFmpegFrameRecorder,
    imageCache: LinkedBlockingDeque[LatestFrame],
    //    soundCache: LinkedBlockingDeque[LatestSound],
    needImage: Boolean,
    needSound: Boolean,
    debug: Boolean
  ) = {
    val childName = s"EncodeActor-$encodeType"
    ctx.child(childName).getOrElse {
      val actor = ctx.spawn(EncodeActor.create(replyTo, encodeType, encoder, imageCache, needImage, needSound, debug, needTimeMark), childName, blockingDispatcher)
      ctx.watchWith(actor, ChildDead(childName, actor))
      actor
    }.unsafeUpcast[EncodeActor.Command]

  }


}
