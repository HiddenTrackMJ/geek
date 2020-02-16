package org.seekloud.geek.client.core.stream

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior, DispatcherSelector}
import javafx.scene.canvas.GraphicsContext
import org.seekloud.geek.capture.protocol.Messages.EncoderType
import org.seekloud.geek.capture.sdk.{DeviceUtil, MediaCapture}
import org.seekloud.geek.client.Boot.executor
import org.seekloud.geek.client.common.AppSettings
import org.seekloud.geek.client.controller.GeekHostController
import org.seekloud.geek.client.core.RmManager
import org.seekloud.geek.client.core.RmManager.roomInfo
import org.seekloud.geek.client.core.collector.ClientCaptureActor
import org.seekloud.geek.client.core.collector.ClientCaptureActor.{StartEncode, StopEncode}
import org.seekloud.geek.client.core.player.VideoPlayer
import org.seekloud.geek.client.scene.HostScene
import org.seekloud.geek.client.utils.GetAllPixel
import org.seekloud.geek.player.protocol.Messages.AddPicture
import org.seekloud.geek.player.sdk.MediaPlayer
import org.slf4j.LoggerFactory

import scala.collection.immutable
import scala.language.postfixOps

/**
  * User: Arrow
  * Date: 2019/7/19
  * Time: 12:25
  *
  * client 向服务器端进行推拉流鉴权及控制player的显示
  *
  */
object LiveManager {

  private val log = LoggerFactory.getLogger(this.getClass)

  val dispatcher: DispatcherSelector = DispatcherSelector.fromConfig("akka.actor.my-blocking-dispatcher")

  case class JoinInfo(roomId: Long, audienceId: Long, gc: GraphicsContext)

  case class WatchInfo(roomId: Long, gc: GraphicsContext)

  sealed trait LiveCommand

  final case object GetPackageLoss extends LiveCommand

  private case class ChildDead[U](name: String, childRef: ActorRef[U]) extends LiveCommand

  final case class DevicesOn(gc: GraphicsContext, isJoin: Boolean = false, callBackFunc: Option[() => Unit] = None) extends LiveCommand

  final case object DeviceOff extends LiveCommand

  //切换模式，isJoin为false显示本地摄像头的内容，true显示拉流的内容
  final case class SwitchMediaMode(isJoin: Boolean, reset: () => Unit) extends LiveCommand

  final case class ChangeMediaOption(bit: Option[Int], re: Option[String], frameRate: Option[Int],
    needImage: Boolean = true, needSound: Boolean = true, reset: () => Unit) extends LiveCommand with ClientCaptureActor.CaptureCommand


  final case class PushStream(rtmp:String) extends LiveCommand

  final case object InitRtpFailed extends LiveCommand

  final case object StopPush extends LiveCommand

  final case class Ask4State(reply: ActorRef[Boolean]) extends LiveCommand

  final case class PullStream(stream: String,mediaPlayer: MediaPlayer,hostController:GeekHostController,liveManager: ActorRef[LiveManager.LiveCommand]) extends LiveCommand

  final case object StopPull extends LiveCommand

  final case object PusherStopped extends LiveCommand

  final case object PullerStopped extends LiveCommand






  def create(parent: ActorRef[RmManager.RmCommand], mediaPlayer: MediaPlayer): Behavior[LiveCommand] =
    Behaviors.setup[LiveCommand] { ctx =>
      log.info(s"LiveManager is starting...")
      implicit val stashBuffer: StashBuffer[LiveCommand] = StashBuffer[LiveCommand](Int.MaxValue)
      Behaviors.withTimers[LiveCommand] { implicit timer =>
        idle(parent, mediaPlayer, isStart = false, isRegular = false)
      }
    }


  private def idle(
    parent: ActorRef[RmManager.RmCommand],
    mediaPlayer: MediaPlayer,
    captureActor: Option[ActorRef[ClientCaptureActor.CaptureCommand]] = None,
    mediaCapture: Option[MediaCapture] = None,
    isStart: Boolean,
    isRegular: Boolean
  )(
    implicit stashBuffer: StashBuffer[LiveCommand],
    timer: TimerScheduler[LiveCommand]
  ): Behavior[LiveCommand] =
    Behaviors.receive[LiveCommand] { (ctx, msg) =>
      Behaviors.unhandled
      msg match {
        case msg: DevicesOn =>
          val captureActor = getCaptureActor(ctx, msg.gc, msg.isJoin, msg.callBackFunc)
          val mediaCapture = MediaCapture(captureActor, debug = AppSettings.captureDebug, needTimestamp = AppSettings.needTimestamp)
          val availableDevices = GetAllPixel.getAllDevicePixel()
          var pixel = (640,360)
          if(availableDevices.nonEmpty && !availableDevices.contains("640x360")){
            pixel = DeviceUtil.parseImgResolution(availableDevices.max)
          }
          log.info("availableDevices pixels: "+availableDevices)
//          mediaCapture.setAudioCodec(avcodec.AV_CODEC_ID_MP2)
//          mediaCapture.setVideoCodec(avcodec.AV_CODEC_ID_MPEG2VIDEO)
          mediaCapture.setImageWidth(pixel._1)
          mediaCapture.setImageHeight(pixel._2)
          captureActor ! ClientCaptureActor.GetMediaCapture(mediaCapture)
          mediaCapture.start()
          idle(parent, mediaPlayer, Some(captureActor),  Some(mediaCapture), isStart = isStart, isRegular = isRegular)

        case DeviceOff =>
          captureActor.foreach(_ ! ClientCaptureActor.StopCapture)
          idle(parent, mediaPlayer, None, isStart = isStart, isRegular = isRegular)

//
//
        case msg: SwitchMediaMode =>
          captureActor.foreach(_ ! ClientCaptureActor.SwitchMode(msg.isJoin, msg.reset))
          Behaviors.same
//
//
        case msg: PushStream =>
          log.debug(s"推流地址：${msg.rtmp}")
          captureActor.get ! StartEncode(msg.rtmp)
          Behaviors.same

        case InitRtpFailed =>
          ctx.self ! StopPush
          Behaviors.same

        case StopPush =>
          log.info(s"LiveManager stop pusher!")
          captureActor.get ! StopEncode(EncoderType.RTMP)
          Behaviors.same

        case msg: PullStream =>

          log.info(s"拉流地址：${msg.stream}")
          /*背景改变*/
//          msg.hostScene.reset Back()

          /*媒体画面模式更改*/
          msg.liveManager ! LiveManager.SwitchMediaMode(isJoin = true, reset = msg.hostController.resetBack)

          /*拉取观众的rtp流并播放*/

          //定义 imageQueue 和 samplesQueue，用来接收图像和音频数据
          val imageQueue = immutable.Queue[AddPicture]()
          val samplesQueue = immutable.Queue[Array[Byte]]()

          //直接启动播放器，拉流并播放到画布上
          val playId = RmManager.roomInfo.get.roomId.toString
          val videoPlayer = ctx.spawn(VideoPlayer.create(playId,Some(imageQueue),Some(samplesQueue)), s"videoPlayer$playId")

          mediaPlayer.start(playId,videoPlayer,Left(msg.stream),Some(msg.hostController.gc),None)

          Behaviors.same

        //
//
        case StopPull =>
          log.info(s"LiveManager stop puller")

          Behaviors.same
//
        case PusherStopped =>
          log.info(s"LiveManager got pusher stopped.")
          idle(parent, mediaPlayer, captureActor, isStart = isStart, isRegular = isRegular)

        case PullerStopped =>
          log.info(s"LiveManager got puller stopped.")
          if(isRegular) parent ! RmManager.PullerStopped
          idle(parent, mediaPlayer, captureActor, isStart = false, isRegular = false)


        case x =>
          log.warn(s"unknown msg in idle: $x")
          Behaviors.unhandled
      }
    }

  private def getCaptureActor(
    ctx: ActorContext[LiveCommand],
    gc: GraphicsContext,
    isJoin: Boolean,
    callBackFunc: Option[() => Unit],
    frameRate: Int = 30
  ) = {
    val childName = s"captureActor-${System.currentTimeMillis()}"
    ctx.child(childName).getOrElse {
      val actor = ctx.spawn(ClientCaptureActor.create(frameRate, gc, isJoin, callBackFunc), childName)
      ctx.watchWith(actor, ChildDead(childName, actor))
      actor
    }.unsafeUpcast[ClientCaptureActor.CaptureCommand]
  }



}
