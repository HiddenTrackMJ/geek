package org.seekloud.geek.client.core.stream

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior, DispatcherSelector}
import javafx.scene.canvas.GraphicsContext
import org.seekloud.geek.capture.sdk.MediaCapture
import org.seekloud.geek.client.core.RmManager
import org.seekloud.geek.player.sdk.MediaPlayer
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * User: Arrow
  * Date: 2019/7/19
  * Time: 12:25
  *
  * client 向服务器端进行推拉流鉴权及控制
  *
  */
object LiveManager {

  private val log = LoggerFactory.getLogger(this.getClass)
//  private var validHost = clientHost

  val dispatcher: DispatcherSelector = DispatcherSelector.fromConfig("akka.actor.my-blocking-dispatcher")

  case class JoinInfo(roomId: Long, audienceId: Long, gc: GraphicsContext)

  case class WatchInfo(roomId: Long, gc: GraphicsContext)

  sealed trait LiveCommand

  final case object GetPackageLoss extends LiveCommand

  private case class ChildDead[U](name: String, childRef: ActorRef[U]) extends LiveCommand

  final case class DevicesOn(gc: GraphicsContext, isJoin: Boolean = false, callBackFunc: Option[() => Unit] = None) extends LiveCommand

  final case object DeviceOff extends LiveCommand

  final case class SwitchMediaMode(isJoin: Boolean, reset: () => Unit) extends LiveCommand

//  final case class ChangeMediaOption(bit: Option[Int], re: Option[String], frameRate: Option[Int],
//    needImage: Boolean = true, needSound: Boolean = true, reset: () => Unit) extends LiveCommand with CaptureActor.CaptureCommand

//  final case class RecordOption(recordOrNot: Boolean, path: Option[String] = None, reset: () => Unit)  extends LiveCommand with CaptureActor.CaptureCommand

  final case class PushStream(liveId: String, liveCode: String) extends LiveCommand

  final case object InitRtpFailed extends LiveCommand

  final case object StopPush extends LiveCommand

  final case class Ask4State(reply: ActorRef[Boolean]) extends LiveCommand

//  final case class PullStream(liveId: String, joinInfo: Option[JoinInfo] = None, watchInfo: Option[WatchInfo] = None, audienceScene: Option[AudienceScene] = None, hostScene: Option[HostScene] = None) extends LiveCommand

  final case object StopPull extends LiveCommand

  final case object PusherStopped extends LiveCommand

  final case object PullerStopped extends LiveCommand

  private object PUSH_RETRY_TIMER_KEY

  private object PULL_RETRY_TIMER_KEY


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
//    captureActor: Option[ActorRef[CaptureActor.CaptureCommand]] = None,
    streamPusher: Option[ActorRef[StreamPusher.PushCommand]] = None,
    streamPuller: Option[(String, ActorRef[StreamPuller.PullCommand])] = None,
    mediaCapture: Option[MediaCapture] = None,
    isStart: Boolean,
    isRegular: Boolean
  )(
    implicit stashBuffer: StashBuffer[LiveCommand],
    timer: TimerScheduler[LiveCommand]
  ): Behavior[LiveCommand] =
    Behaviors.receive[LiveCommand] { (ctx, msg) =>
      Behaviors.unhandled
//      msg match {
//        case msg: DevicesOn =>
//          val captureActor = getCaptureActor(ctx, msg.gc, msg.isJoin, msg.callBackFunc)
//          val mediaCapture = MediaCapture(captureActor, debug = AppSettings.captureDebug, needTimestamp = AppSettings.needTimestamp)
////          mediaCapture.setImageHeight(AppSettings.d_h)
////          mediaCapture.setImageWidth(AppSettings.d_w)
//          captureActor ! CaptureActor.GetMediaCapture(mediaCapture)
//
//          mediaCapture.start()
//          idle(parent, mediaPlayer, Some(captureActor), streamPusher, streamPuller, Some(mediaCapture), isStart = isStart, isRegular = isRegular)
//
//        case DeviceOff =>
//          captureActor.foreach(_ ! CaptureActor.StopCapture)
//          idle(parent, mediaPlayer, None, streamPusher, streamPuller, isStart = isStart, isRegular = isRegular)
//
//
//
//        case msg: SwitchMediaMode =>
//          captureActor.foreach(_ ! CaptureActor.SwitchMode(msg.isJoin, msg.reset))
//          Behaviors.same
//
//        case msg: ChangeMediaOption =>
//          captureActor.foreach(_ ! msg)
//          Behaviors.same
//
//        case msg: RecordOption =>
//          captureActor.foreach(_ ! msg)
//          Behaviors.same
//
//        case msg: PushStream =>
//          log.debug(s"prepare push stream.")
//          assert(captureActor.nonEmpty)
//          if (streamPusher.isEmpty) {
//            val pushChannel = new PushChannel
//            val pusher = getStreamPusher(ctx, msg.liveId, msg.liveCode, captureActor.get)
//            RtpUtil.initIpPool()
//            validHost = clientHostQueue.dequeue()
//            val rtpClient = new PushStreamClient(AppSettings.host, NetUtil.getFreePort, pushChannel.serverPushAddr, pusher,AppSettings.rtpServerDst)
//            mediaCapture.foreach(_.setTimeGetter(rtpClient.getServerTimestamp))
//            pusher ! StreamPusher.InitRtpClient(rtpClient)
//            idle(parent, mediaPlayer, captureActor, Some(pusher), streamPuller, isStart = isStart, isRegular = isRegular)
//          } else {
//            log.info(s"waiting for old pusher stop.")
//            ctx.self ! StopPush
//            timer.startSingleTimer(PUSH_RETRY_TIMER_KEY, msg, 100.millis)
//            Behaviors.same
//          }
//
//        case InitRtpFailed =>
//          ctx.self ! StopPush
//          Behaviors.same
//
//        case StopPush =>
//          log.info(s"LiveManager stop pusher!")
//          streamPusher.foreach {
//            pusher =>
//              log.info(s"stopping pusher...")
//              pusher ! StreamPusher.StopPush
//          }
//          Behaviors.same
//
//        case msg: PullStream =>
//          if (streamPuller.isEmpty) {
//            val pullChannel = new PullChannel
//            val puller = getStreamPuller(ctx, msg.liveId, mediaPlayer, msg.joinInfo, msg.watchInfo, msg.audienceScene, msg.hostScene)
//            val rtpClient = new PullStreamClient(AppSettings.host, NetUtil.getFreePort, pullChannel.serverPullAddr, puller, AppSettings.rtpServerDst)
//            puller ! StreamPuller.InitRtpClient(rtpClient)
//            idle(parent, mediaPlayer, captureActor, streamPusher, Some((msg.liveId, puller)), isStart = true, isRegular = isRegular)
//          } else {
//            log.info(s"waiting for old puller-${streamPuller.get._1} stop.")
//            ctx.self ! StopPull
//            timer.startSingleTimer(PULL_RETRY_TIMER_KEY, msg, 100.millis)
//            Behaviors.same
//          }
//
//        case GetPackageLoss =>
//          streamPuller.foreach { s =>
//            s._2 ! StreamPuller.GetLossAndBand
//          }
//          Behaviors.same
//
//        case StopPull =>
//          log.info(s"LiveManager stop puller")
//          streamPuller.foreach {
//            puller =>
//              log.info(s"stopping puller-${puller._1}")
//              puller._2 ! StreamPuller.StopPull
//          }
//          Behaviors.same
//
//        case PusherStopped =>
//          log.info(s"LiveManager got pusher stopped.")
//          idle(parent, mediaPlayer, captureActor, None, streamPuller, isStart = isStart, isRegular = isRegular)
//
//        case PullerStopped =>
//          log.info(s"LiveManager got puller stopped.")
//          if(isRegular) parent ! RmManager.PullerStopped
//          idle(parent, mediaPlayer, captureActor, streamPusher, None, isStart = false, isRegular = false)
//
//        case Ask4State(reply) =>
//          reply ! isStart
//          idle(parent, mediaPlayer, captureActor, streamPusher, streamPuller, isStart = isStart, isRegular = true)
//
//        case ChildDead(child, childRef) =>
//          log.debug(s"LiveManager unWatch child-$child")
//          ctx.unwatch(childRef)
//          Behaviors.same
//
//        case x =>
//          log.warn(s"unknown msg in idle: $x")
//          Behaviors.unhandled
//      }
    }

//  private def getCaptureActor(
//    ctx: ActorContext[LiveCommand],
//    gc: GraphicsContext,
//    isJoin: Boolean,
//    callBackFunc: Option[() => Unit],
//    frameRate: Int = 30
//  ) = {
//    val childName = s"captureActor-${System.currentTimeMillis()}"
//    ctx.child(childName).getOrElse {
//      val actor = ctx.spawn(CaptureActor.create(frameRate, gc, isJoin, callBackFunc), childName)
//      ctx.watchWith(actor, ChildDead(childName, actor))
//      actor
//    }.unsafeUpcast[CaptureActor.CaptureCommand]
//  }
//
//  private def getStreamPusher(
//    ctx: ActorContext[LiveCommand],
//    liveId: String,
//    liveCode: String,
//    //    mediaActor: ActorRef[MediaActor.MediaCommand]
//    captureActor: ActorRef[CaptureActor.CaptureCommand]
//  ) = {
//    val childName = s"streamPusher-$liveId"
//    ctx.child(childName).getOrElse {
//      val actor = ctx.spawn(StreamPusher.create(liveId, liveCode, ctx.self, captureActor), childName)
//      ctx.watchWith(actor, ChildDead(childName, actor))
//      actor
//    }.unsafeUpcast[StreamPusher.PushCommand]
//  }

//  private def getStreamPuller(
//    ctx: ActorContext[LiveCommand],
//    liveId: String,
//    mediaPlayer: MediaPlayer,
//    joinInfo: Option[JoinInfo],
//    watchInfo: Option[WatchInfo],
//    audienceScene : Option[AudienceScene],
//    hostScene: Option[HostScene]
//  ) = {
//    val childName = s"streamPuller-$liveId"
//    ctx.child(childName).getOrElse {
//      val actor = ctx.spawn(StreamPuller.create(liveId, ctx.self, mediaPlayer, joinInfo, watchInfo, audienceScene, hostScene), childName)
//      ctx.watchWith(actor, ChildDead(childName, actor))
//      actor
//    }.unsafeUpcast[StreamPuller.PullCommand]
//  }


}
