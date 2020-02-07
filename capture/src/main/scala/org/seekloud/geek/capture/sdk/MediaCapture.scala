package org.seekloud.geek.capture.sdk

import java.io.{File, OutputStream}
import java.util.concurrent.atomic.AtomicInteger

import akka.actor.{ActorSystem, Scheduler}
import akka.actor.typed.{ActorRef, DispatcherSelector}
import akka.actor.typed.scaladsl.adapter._
import akka.dispatch.MessageDispatcher
import akka.stream.ActorMaterializer
import akka.util.Timeout
import org.bytedeco.ffmpeg.global.avcodec
import org.seekloud.geek.capture.common.AppSettings
import org.seekloud.geek.capture.core.CaptureManager
import org.seekloud.geek.capture.core.CaptureManager.MediaSettings
import org.seekloud.geek.capture.protocol.Messages
import org.slf4j.LoggerFactory

import concurrent.duration._
import language.postfixOps

/**
  * User: TangYaruo
  * Date: 2019/8/28
  * Time: 11:56
  *
  *
  * 采集数据SDK - 非阻塞式交互
  *
  */

object MediaCapture {

  /**
    *
    * @param replyTo           用户创建，负责接收SDK端返回消息的actor
    * @param outputStream      若用户传入outputStream，sdk则编码输出mpegts流
    * @param camDeviceIndex    摄像头设备序号
    * @param audioDeviceIndex  声音设备序号
    *
    * */
  def apply(
    replyTo: ActorRef[Messages.ReplyToCommand],
    outputStream: Option[OutputStream] = None,
    camDeviceIndex: Int = 0,
    audioDeviceIndex: Int = 4,
    debug: Boolean = true,
    needTimestamp: Boolean = true
  ): MediaCapture = new MediaCapture(replyTo, outputStream, camDeviceIndex, audioDeviceIndex, debug, needTimestamp)


  import org.seekloud.geek.capture.common.AppSettings._

  private val log = LoggerFactory.getLogger(this.getClass)

  implicit val system: ActorSystem = ActorSystem("CaptureSystem", config)
  implicit val executor: MessageDispatcher = system.dispatchers.lookup("akka.actor.my-blocking-dispatcher")
  val blockingDispatcher: DispatcherSelector = DispatcherSelector.fromConfig("akka.actor.my-blocking-dispatcher")

  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val scheduler: Scheduler = system.scheduler
  implicit val timeout: Timeout = Timeout(20 seconds)


}


class MediaCapture(
  replyTo: ActorRef[Messages.ReplyToCommand],
  outputStream: Option[OutputStream] = None,
  camDeviceIndex: Int = 0,
  audioDeviceIndex: Int = 4,
  debug: Boolean = true,
  needTimestamp: Boolean = true
) {

  import MediaCapture._

   var mediaSettings: Option[MediaSettings] = None

  private var captureManager: Option[ ActorRef[CaptureManager.Command]] = None

  private val captureNum : AtomicInteger = new AtomicInteger(0)

  /*提供设置的默认值*/
  /*画面配置*/
  private var imageWidth = 640
  private var imageHeight = 360
  private var frameRate = AppSettings.frameRate
  private var outputBitrate = AppSettings.bit
  private var needImage = true
  private var videoCodec = avcodec.AV_CODEC_ID_H264


  /*声音配置*/
  private var sampleRate = 44100.0F
  private var sampleSizeInBits = 16
  private var channels = 2
  private var needSound = true
  private var audioCodec = avcodec.AV_CODEC_ID_AAC

  /*录制选项*/
  private var outRtmp :Option[String] = None
  private var outputFile: Option[File] = None
  private var startOutputFile = true//改成false由startEncode 开始控制


  def setImageWidth(imageWidth: Int): Unit = {
    this.imageWidth = imageWidth
  }

  def getImageWidth: Int = this.imageWidth

  def setImageHeight(imageHeight: Int): Unit = {
    this.imageHeight = imageHeight
  }

  def getImageHeight: Int = this.imageHeight

  def setFrameRate(frameRate: Int): Unit = {
    this.frameRate = frameRate
  }

  def getFrameRate: Int = this.frameRate

  def setOutputBitrate(bitrate: Int): Unit = {
    this.outputBitrate = bitrate
  }

  def getOutputBitrate: Int = this.outputBitrate

  def needImage(needOrNot: Boolean): Unit = {
    this.needImage = needOrNot
  }

  def isImageNeeded: Boolean = this.needImage

  def setSampleRate(sampleRate: Float): Unit = {
    this.sampleRate = sampleRate
  }

  def getSampleRate: Float = this.sampleRate

  def setSampleSizeInBits(sampleSize: Int): Unit = {
    this.sampleSizeInBits = sampleSize
  }

  def getSampleSizeInBits: Int = this.sampleSizeInBits

  def setChannels(channel: Int): Unit = {
    this.channels = channel
  }

  def getChannels: Int = this.channels

  def needSound(needOrNot: Boolean): Unit = {
    this.needSound = needOrNot
  }

  def isSoundNeeded: Boolean = this.needSound

  def setOutputFile(file: File): Unit = {
    this.outputFile = Some(file)
  }

  def serOutPutRtmp(rtmp:String):Unit = {
    this.outRtmp = Some(rtmp)
  }

  def getOutputFileName: Option[String] = this.outputFile.map(_.getName)

  def getAudioCodec: Int = this.audioCodec

  def getVideoCode: Int = this.videoCodec

  def setOptions(imageWidth: Option[Int] = None,
    imageHeight: Option[Int] = None,
    frameRate: Option[Int] = None,
    outputBitrate: Option[Int] = None,
    needImage: Option[Boolean] = None,
    sampleRate: Option[Float] = None,
    sampleSizeInBits: Option[Int] = None,
    channels: Option[Int] = None,
    needSound: Option[Boolean] = None,
    audioCodec: Option[Int] = None,
    videoCodec: Option[Int] = None): Unit = {

    this.imageWidth = imageWidth.getOrElse(this.imageWidth)
    this.imageHeight = imageHeight.getOrElse(this.imageHeight)
    this.frameRate = frameRate.getOrElse(this.frameRate)
    this.outputBitrate = outputBitrate.getOrElse(this.outputBitrate)
    this.needImage = needImage.getOrElse(this.needImage)
    this.sampleRate = sampleRate.getOrElse(this.sampleRate)
    this.sampleSizeInBits = sampleSizeInBits.getOrElse(this.sampleSizeInBits)
    this.channels = channels.getOrElse(this.channels)
    this.needSound = needSound.getOrElse(this.needSound)
    this.audioCodec = audioCodec.getOrElse(this.audioCodec)
    this.videoCodec = videoCodec.getOrElse(this.videoCodec)
    mediaSettings = Some(MediaSettings(
      this.imageWidth,
      this.imageHeight,
      this.frameRate,
      this.outputBitrate,
      this.needImage,
      this.sampleRate,
      this.sampleSizeInBits,
      this.channels,
      this.needSound,
      this.audioCodec,
      this.videoCodec,
      this.camDeviceIndex,
      this.audioDeviceIndex
    ))
  }

  def setTimeGetter(func: () => Long): Unit = {
    captureManager.foreach(_ ! CaptureManager.SetTimerGetter(func))
  }

  def showDesktop(): Unit ={
    captureManager.foreach(_ ! CaptureManager.ShowDesktop)
  }

  def showCamera(): Unit ={
    captureManager.foreach(_ ! CaptureManager.ShowPerson)
  }

  def showBoth(): Unit ={
    captureManager.foreach(_ ! CaptureManager.ShowBoth)
  }



  def changeCameraPosition(position: Int): Unit ={
    captureManager.foreach(_ ! CaptureManager.CameraPosition(position))
  }


  /**
    * 初始化并开始工作
    *
    * */
  def start(): Unit = {

//    val number = captureNum.incrementAndGet()

    val mediaNo = s"captureManager-${System.currentTimeMillis()}"
    log.info(s"$mediaNo is starting...")

    val mediaSettings =
      if (this.mediaSettings.isEmpty) {MediaSettings(
        imageWidth,
        imageHeight,
        frameRate,
        outputBitrate,
        needImage,
        sampleRate,
        sampleSizeInBits,
        channels,
        needSound,
        audioCodec,
        videoCodec,
        camDeviceIndex,
        audioDeviceIndex
      )}
      else this.mediaSettings.get

    val file = if(startOutputFile) outputFile else None
    captureManager = Some(system.spawn(CaptureManager.create(replyTo, mediaSettings, outputStream, file, debug, needTimestamp), mediaNo))

  }

  def stop(startOutputFile: Boolean = false): Unit = {
    this.startOutputFile = startOutputFile
    log.info(s"MediaCapture-${captureNum.get()} is stopping...")
    captureManager.foreach { c =>
      c ! CaptureManager.StopCapture
    }
    if (captureManager.isDefined) captureManager = None

  }

}
