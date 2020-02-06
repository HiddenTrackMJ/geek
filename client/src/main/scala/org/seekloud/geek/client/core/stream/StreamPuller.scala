package org.seekloud.geek.client.core.stream

import java.nio.ByteBuffer
import java.nio.channels.{Channels, Pipe}

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import org.seekloud.geek.client.common.Constants.AudienceStatus
import org.seekloud.geek.client.core.stream.LiveManager.{JoinInfo, WatchInfo}
import org.seekloud.geek.client.scene.HostScene
import org.seekloud.geek.client.utils.rtpClient.PullStreamClient
import org.seekloud.geek.client.core.player.VideoPlayer
import org.seekloud.geek.player.sdk.MediaPlayer
import org.seekloud.geek.shared.rtp.Protocol
import org.seekloud.geek.shared.rtp.Protocol.{PullStreamReady, PullStreamReqSuccess}
import org.slf4j.LoggerFactory

import scala.concurrent.duration._

/**
  * User: TangYaruo
  * Date: 2019/8/20
  * Time: 13:41
  * Description: 推流
  */
object StreamPuller {

  private val log = LoggerFactory.getLogger(this.getClass)

  type PullCommand = Protocol.Command

  case class PackageLossInfo(lossScale60: Double, lossScale10: Double, lossScale2: Double)

  case class BandWidthInfo(bandWidth60s: Double, bandWidth10s: Double, bandWidth2s: Double)

  final case class InitRtpClient(pullClient: PullStreamClient) extends PullCommand

  final case object PullStartTimeOut extends PullCommand

  final case object GetLossAndBand extends PullCommand

  final case object PullStream extends PullCommand

  final case object PullTimeOut extends PullCommand

  final case object StopPull extends PullCommand

  final case object StopSelf extends PullCommand

  private case class TimeOut(msg: String) extends PullCommand

  private final case object BehaviorChangeKey


  private final case class SwitchBehavior(
    name: String,
    behavior: Behavior[PullCommand],
    durationOpt: Option[FiniteDuration] = None,
    timeOut: TimeOut = TimeOut("busy time error")
  ) extends PullCommand

  private[this] def switchBehavior(ctx: ActorContext[PullCommand],
    behaviorName: String, behavior: Behavior[PullCommand], durationOpt: Option[FiniteDuration] = None, timeOut: TimeOut = TimeOut("busy time error"))
    (implicit stashBuffer: StashBuffer[PullCommand],
      timer: TimerScheduler[PullCommand]) = {
    timer.cancel(BehaviorChangeKey)
    durationOpt.foreach(timer.startSingleTimer(BehaviorChangeKey, timeOut, _))
    stashBuffer.unstashAll(ctx, behavior)
  }


  def create(
    liveId: String,
    parent: ActorRef[LiveManager.LiveCommand],
    mediaPlayer: MediaPlayer,
    joinInfo: Option[JoinInfo],
    watchInfo: Option[WatchInfo],
    hostScene: Option[HostScene]
  ): Behavior[PullCommand] =
    Behaviors.setup[PullCommand] { ctx =>
      log.info(s"StreamPuller-$liveId is starting.")
      implicit val stashBuffer: StashBuffer[PullCommand] = StashBuffer[PullCommand](Int.MaxValue)
      Behaviors.withTimers[PullCommand] { implicit timer =>
        init(liveId, parent,mediaPlayer,joinInfo,watchInfo,hostScene)
      }

    }

  private def init(
    liveId: String,
    parent: ActorRef[LiveManager.LiveCommand],
    mediaPlayer: MediaPlayer,
    joinInfo: Option[JoinInfo],
    watchInfo: Option[WatchInfo],
    hostScene: Option[HostScene],
    pullClient: Option[PullStreamClient] = None
  )(
    implicit timer: TimerScheduler[PullCommand],
    stashBuffer: StashBuffer[PullCommand]
  ): Behavior[PullCommand] =
    Behaviors.receive[PullCommand] { (ctx, msg) =>
      Behaviors.unhandled
      msg match {
        case msg: InitRtpClient =>
          log.info(s"StreamPuller-$liveId init rtpClient.")
          msg.pullClient.pullStreamStart()
          timer.startSingleTimer(PullStartTimeOut, PullStartTimeOut, 5.seconds)
//          hostScene.foreach(_.startPackageLoss())
          init(liveId, parent, mediaPlayer, joinInfo, watchInfo, hostScene, Some(msg.pullClient))

        case PullStreamReady =>
          log.info(s"StreamPuller-$liveId ready for pull.")
          timer.cancel(PullStartTimeOut)
          ctx.self ! PullStream
          Behaviors.same

        case PullStartTimeOut =>
          pullClient.foreach(_.getClientId())
          timer.startSingleTimer(PullStartTimeOut, PullStartTimeOut, 5.seconds)
          Behaviors.same
//
        case PullStream =>
          log.info(s"StreamPuller-$liveId PullStream.")
          pullClient.foreach(_.pullStreamData(List(liveId)))
          timer.startSingleTimer(PullTimeOut, PullTimeOut, 30.seconds)
          Behaviors.same
//
        case msg: PullStreamReqSuccess =>
          log.info(s"StreamPuller-$liveId PullStream-${msg.liveIds} success.")
          timer.cancel(PullTimeOut)
          val mediaPipe = Pipe.open() // server -> sink -> source -> client
          val sink = mediaPipe.sink()
          val source = mediaPipe.source()
          sink.configureBlocking(false)
          val inputStream = Channels.newInputStream(source)

          if (watchInfo.nonEmpty) {
            log.info("拉流请求成功PullStreamReqSuccess")
            val playId = s"roomId${watchInfo.get.roomId}"
            mediaPlayer.setTimeGetter(playId, pullClient.get.getServerTimestamp)
            val videoPlayer = ctx.spawn(VideoPlayer.create(playId, None, None), s"videoPlayer$playId")

            //todo 播放流的内容
            mediaPlayer.start(playId, videoPlayer, Right(inputStream), Some(watchInfo.get.gc), None)
          }
          stashBuffer.unstashAll(ctx, pulling(liveId, parent, pullClient.get, mediaPlayer, sink, hostScene))

        case GetLossAndBand =>
          pullClient.foreach{ p =>
            val info = {
              p.getPackageLoss().map(i => i._1 -> PackageLossInfo(i._2.lossScale60, i._2.lossScale10, i._2.lossScale2))
            }

            val bandInfo = p.getBandWidth().map(i => i._1 -> BandWidthInfo(i._2.bandWidth60s, i._2.bandWidth10s, i._2.bandWidth2s))
//            hostScene.foreach(_.drawPackageLoss(info, bandInfo))
          }
          Behaviors.same
//
//        case PullStreamPacketLoss =>
//          log.info(s"StreamPuller-$liveId PullStreamPacketLoss.")
//          timer.startSingleTimer(PullStream, PullStream, 30.seconds)
//          Behaviors.same
//
//        case msg: NoStream =>
//          log.info(s"No stream ids: ${msg.liveIds}")
//          if (msg.liveIds.contains(liveId)) {
//            log.info(s"Stream-$liveId unavailable now, try later.")
//            timer.startSingleTimer(PullStream, PullStream, 30.seconds)
//          }
//          Behaviors.same
//
//        case PullTimeOut =>
//          log.info(s"StreamPuller-$liveId pull timeout, try again.")
//          ctx.self ! PullStream
//          Behaviors.same
//
//        case StopPull =>
//          log.info(s"StreamPuller-$liveId stopped in init.")
//          parent ! LiveManager.PullerStopped
//          Behaviors.stopped
//
        case x =>
          log.warn(s"unhandled msg in init: $x")
          stashBuffer.stash(x)
          Behaviors.same
      }
    }
  private object ENSURE_STOP_PULL


  //拉流状态
  private def pulling(
    liveId: String,
    parent: ActorRef[LiveManager.LiveCommand],
    pullClient: PullStreamClient,
    mediaPlayer: MediaPlayer,
    //    joinInfo: Option[JoinInfo],
    mediaSink: Pipe.SinkChannel,
    hostScene: Option[HostScene]
  )(
    implicit timer: TimerScheduler[PullCommand],
    stashBuffer: StashBuffer[PullCommand]
  ): Behavior[PullCommand] =
    Behaviors.receive[PullCommand] { (ctx, msg) =>
      msg match {
//        case msg: PullStreamData =>
//          if (msg.data.nonEmpty) {
//            try {
////              log.debug(s"StreamPuller-$liveId pull-${msg.data.length}.")
//              mediaSink.write(ByteBuffer.wrap(msg.data))
//              //              log.debug(s"StreamPuller-$liveId  write success.")
//              ctx.self ! SwitchBehavior("pulling", pulling(liveId, parent, pullClient, mediaPlayer, mediaSink, audienceScene, hostScene))
//            } catch {
//              case ex: Exception =>
//                log.warn(s"sink write pulled data error: $ex. Stop StreamPuller-$liveId")
//                ctx.self ! StopPull
//            }
//          } else {
//            log.debug(s"StreamPuller-$liveId pull null.")
//            ctx.self ! SwitchBehavior("pulling", pulling(liveId, parent, pullClient, mediaPlayer, mediaSink, audienceScene, hostScene))
//          }
//          busy(liveId, parent, pullClient)
//
//        case GetLossAndBand =>
//          val info = pullClient.getPackageLoss().map(i => i._1 -> PackageLossInfo(i._2.lossScale60, i._2.lossScale10, i._2.lossScale2))
//          val bandInfo = pullClient.getBandWidth().map(i => i._1 -> BandWidthInfo(i._2.bandWidth60s, i._2.bandWidth10s, i._2.bandWidth2s))
//          audienceScene.foreach(_.drawPackageLoss(info, bandInfo))
//          hostScene.foreach(_.drawPackageLoss(info, bandInfo))
//          Behaviors.same
//
//        case StopPull =>
//          log.info(s"StreamPuller-$liveId is stopping.")
//          timer.startPeriodicTimer(ENSURE_STOP_PULL,StopPull,2000.milliseconds)
//          try pullClient.close()
//          catch {
//            case  e: Exception =>
//              log.info(s"StreamPuller-$liveId close error: $e")
//          }
//          Behaviors.same
//
//        case CloseSuccess =>
//          log.info(s"StreamPuller-$liveId stopped.")
//          parent ! LiveManager.PullerStopped
//          timer.cancel(ENSURE_STOP_PULL)
//          Behaviors.stopped
//
//        case msg: StreamStop =>
//          log.info(s"Pull stream-${msg.liveId} thread has been closed.")
//          parent ! LiveManager.PullerStopped
//          Boot.addToPlatform {
//            WarningDialog.initWarningDialog("播放中的流已被关闭!")
//            hostScene.foreach(_.listener.shutJoin())
//            audienceScene.foreach(a => a.listener.quitJoin(a.getRoomInfo.roomId))
//          }
//          Behaviors.stopped
//
//        case PullStream =>
//          Behaviors.same

        case x =>
          log.warn(s"unknown msg in pulling: $x")
          Behaviors.unhandled
      }
    }
//
//
//  private def busy(
//    liveId: String,
//    parent: ActorRef[LiveManager.LiveCommand],
//    pullClient: PullStreamClient
//  )
//    (
//      implicit stashBuffer: StashBuffer[PullCommand],
//      timer: TimerScheduler[PullCommand]
//    ): Behavior[PullCommand] =
//    Behaviors.receive[PullCommand] { (ctx, msg) =>
//      msg match {
//        case SwitchBehavior(name, b, durationOpt, timeOut) =>
//          switchBehavior(ctx, name, b, durationOpt, timeOut)
//
//        case TimeOut(m) =>
//          log.debug(s"${ctx.self.path} is time out when busy, msg=$m")
//          Behaviors.stopped
//
//        case StopPull =>
//          log.info(s"StreamPuller-$liveId is stopping.")
//          try pullClient.close()
//          catch {
//            case  e: Exception =>
//              log.info(s"StreamPuller-$liveId close error: $e")
//          }
//          Behaviors.same
//
//        case CloseSuccess =>
//          log.info(s"StreamPuller-$liveId stopped.")
//          parent ! LiveManager.PullerStopped
//          Behaviors.stopped
//
//        case x =>
//          stashBuffer.stash(x)
//          Behavior.same
//
//      }
//    }

}
