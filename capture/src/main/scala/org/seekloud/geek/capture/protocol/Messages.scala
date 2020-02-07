package org.seekloud.geek.capture.protocol

import java.io.{File, OutputStream}
import java.nio.ShortBuffer

import akka.actor.typed.ActorRef
import javafx.scene.image.Image
import org.bytedeco.javacv.Frame
import org.seekloud.geek.capture.core.CaptureManager

/**
  * User: TangYaruo
  * Date: 2019/8/28
  * Time: 12:19
  */
object Messages {

  case class LatestFrame(frame: Frame, ts: Long)  //采集到该帧画面的时间，不是播放时间戳

  case class LatestImage(image: Image, ts: Long)

  case class LatestSound(samples: ShortBuffer, ts: Long) //采集到声音样本的时间

  object EncoderType extends Enumeration {
    val STREAM, FILE, RTMP = Value
  }


  /*to user actor*/
  trait ReplyToCommand

  final case class CaptureStartSuccess(manager: ActorRef[CaptureManager.Command]) extends ReplyToCommand

  final case class CannotAccessSound(manager: ActorRef[CaptureManager.Command]) extends ReplyToCommand

  final case class CannotAccessImage(manager: ActorRef[CaptureManager.Command]) extends ReplyToCommand

  final case class CannotAccessDesktop(manager: ActorRef[CaptureManager.Command]) extends ReplyToCommand

  final case object CaptureStartFailed extends ReplyToCommand

  final case object StreamCannotBeEncoded extends ReplyToCommand

  final case object CannotSaveToFile extends ReplyToCommand

  final case class ImageRsp(latestImage: LatestImage) extends ReplyToCommand

  final case class SoundRsp(latestSound: LatestSound) extends ReplyToCommand

  final case object NoImage extends ReplyToCommand

  final case object NoSamples extends ReplyToCommand

  final case object ManagerStopped extends ReplyToCommand


  /*to manager*/
  type ReqCommand  = CaptureManager.Command

  final case object AskImage extends ReqCommand

  final case object AskSamples extends ReqCommand

  final case class StartEncodeStream(outputStream: OutputStream) extends ReqCommand

  final case object StopEncodeStream extends ReqCommand

  final case class StartEncodeFile(file: File) extends ReqCommand
  final case class StartEncodeRtmp(rtmpDes: String) extends ReqCommand

  final case object StopEncodeFile extends ReqCommand

  final case object StopMediaCapture extends ReqCommand


}
