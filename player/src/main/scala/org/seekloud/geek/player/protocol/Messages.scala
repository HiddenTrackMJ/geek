package org.seekloud.geek.player.protocol

import akka.actor.typed.ActorRef
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import org.seekloud.geek.player.core.ImageActor.ImageCmd
import org.seekloud.geek.player.core.PlayerGrabber
import org.seekloud.geek.player.core.PlayerManager.{MediaInfo, MediaSettings}
import org.seekloud.geek.player.core.SoundActor.SoundCmd


/**
  * User: TangYaruo
  * Date: 2019/8/28
  * Time: 14:26
  */
object Messages {

  trait RTCommand

  /* to videoPlayer*/

  final case object NoStreamToRecord extends RTCommand

  //播放grabber初始化成功
  final case class GrabberInitialed(playerGrabber: ActorRef[PlayerGrabber.MonitorCmd], mediaInfo: MediaInfo, mediaSettings: MediaSettings, gc: Option[GraphicsContext] = None) extends RTCommand

  //播放grabber初始化失败
  final case class GrabberInitFailed(playId: String, ex: Throwable) extends RTCommand

  //录制recorder初始化成功
  final case class RecorderInitialed(playId: String) extends RTCommand

  //录制recorder初始化失败
  final case class RecorderInitFailed(playId: String, ex: Throwable) extends RTCommand

  //录制已停止
  final case class RecordStopped() extends RTCommand

  //播放已停止
  final case object  StopVideoPlayer extends  RTCommand

  final case object PauseAsk extends RTCommand

  final case object  ContinueAsk extends  RTCommand


  /*playerGrabber to VideoPlayer*/

  final case class AddPicture(img: Image, timestamp: Long = -1L) extends ImageCmd with RTCommand

  final case class AddSamples(samples: Array[Byte], ts: Long) extends SoundCmd  with RTCommand

  final case class PictureFinish(resetFunc: Option[() => Unit] = None) extends ImageCmd with RTCommand

  final object SoundFinish extends SoundCmd with RTCommand






}
