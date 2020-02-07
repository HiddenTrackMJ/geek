package org.seekloud.geek.client.core

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import org.seekloud.geek.client.Boot
import org.seekloud.geek.client.common.Constants.{AudienceStatus, HostStatus}
import org.seekloud.geek.client.common.{AppSettings, Routes, StageContext}
import org.seekloud.geek.client.component.WarningDialog
import org.seekloud.geek.client.controller.{HomeController, HostController}
import org.seekloud.geek.client.core.stream.LiveManager
import org.seekloud.geek.client.scene.{HomeScene, HostScene}
import org.seekloud.geek.player.sdk.MediaPlayer
import org.seekloud.geek.shared.client2Manager.websocket.AuthProtocol.{CompleteMsgClient, WsMsgFront}
import org.seekloud.geek.shared.ptcl.CommonProtocol._
import org.slf4j.LoggerFactory
import org.seekloud.geek.client.Boot.{executor, materializer, scheduler, system, timeout}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import org.seekloud.geek.client.core.collector.ClientCaptureActor
import org.seekloud.geek.client.core.player.VideoPlayer
import org.seekloud.geek.client.core.stream.LiveManager.{JoinInfo, PushStream, WatchInfo}
import org.seekloud.geek.client.utils.{RoomClient, WsUtil}
import org.seekloud.geek.player.protocol.Messages.AddPicture

import scala.collection.immutable

/**
 * User: hewro
 * Date: 2020/1/31
 * Time: 14:38
 * Description: 与服务器端的roomManager进行交互
 */
object RmManager {

  private val log = LoggerFactory.getLogger(this.getClass)
  sealed trait RmCommand

  //
  var userInfo: Option[UserInfo] = None
  var roomInfo: Option[RoomInfo] = None


  private[this] def switchBehavior(ctx: ActorContext[RmCommand],
    behaviorName: String,
    behavior: Behavior[RmCommand])
    (implicit stashBuffer: StashBuffer[RmCommand]) = {
    log.debug(s"${ctx.self.path} becomes $behaviorName behavior.")
    stashBuffer.unstashAll(ctx, behavior)
  }


  //拿到homeController 和 homeScreen
  final case class GetHomeItems(homeScene: HomeScene, homeController: HomeController) extends RmCommand
  final case class SignInSuccess(userInfo: Option[UserInfo] = None, roomInfo: Option[RoomInfo] = None) extends RmCommand
  final case object Logout extends RmCommand
  final case object GoToCreateAndJoinRoom extends RmCommand //进去创建会议的页面
//  final case object GoToJoinRoom extends RmCommand //进去加入会议的页面

  final case object HostWsEstablish extends RmCommand
  final case object BackToHome extends RmCommand
  final case object HostLiveReq extends RmCommand //请求开启会议
  final case object StopLive extends RmCommand
  final case object PullerStopped extends RmCommand

  final case object GetPackageLoss extends RmCommand

  //ws链接
  final case class GetSender(sender: ActorRef[WsMsgFront]) extends RmCommand


  def create(stageCtx: StageContext): Behavior[RmCommand] =
    Behaviors.setup[RmCommand] { ctx =>
      log.info(s"RmManager is starting...")
      implicit val stashBuffer: StashBuffer[RmCommand] = StashBuffer[RmCommand](Int.MaxValue)
      Behaviors.withTimers[RmCommand] { implicit timer =>
        //启动client同时启动player
        val mediaPlayer = new MediaPlayer()
        mediaPlayer.init(isDebug = AppSettings.playerDebug, needTimestamp = AppSettings.needTimestamp)
        val liveManager = ctx.spawn(LiveManager.create(ctx.self, mediaPlayer), "liveManager")
        idle(stageCtx, liveManager, mediaPlayer)
      }
    }

  def idle(
    stageCtx: StageContext,
    liveManager: ActorRef[LiveManager.LiveCommand],
    mediaPlayer: MediaPlayer,
    homeController: Option[HomeController] = None
  )(
    implicit stashBuffer: StashBuffer[RmCommand],
    timer: TimerScheduler[RmCommand]
  ):Behavior[RmCommand] = {
    Behaviors.receive[RmCommand]{
      (ctx, msg) =>
        msg match {

          case GetHomeItems(homeScene, homeController) =>
            idle(stageCtx,liveManager,mediaPlayer,Some(homeController))

          case GoToCreateAndJoinRoom =>
            val hostScene = new HostScene(stageCtx.getStage)
            val hostController = new HostController(stageCtx, hostScene, ctx.self)
            def callBack(): Unit = Boot.addToPlatform(hostScene.changeToggleAction())
            liveManager ! LiveManager.DevicesOn(hostScene.gc, callBackFunc = Some(callBack))
            //todo: 暂时不建立ws连接，用http请求
//            ctx.self ! HostWsEstablish
            Boot.addToPlatform {
              if (homeController != null) {
                homeController.get.removeLoading()
              }
              hostController.showScene()
            }
            switchBehavior(ctx, "hostBehavior", hostBehavior(stageCtx, homeController, hostScene, hostController, liveManager, mediaPlayer))


          case SignInSuccess(userInfo, roomInfo)=>
            //todo 可以进行登录后的一些处理，比如创建临时文件等，这部分属于优化项
            Behaviors.same



          case Logout =>
            log.info(s"退出登录.")
            this.roomInfo = None
            this.userInfo = None
            homeController.get.showScene()
            Behaviors.same

          case _=>
            log.info("收到未知消息idle")
            Behaviors.same
        }
    }
  }


  //已经进行会议的场景
  private def hostBehavior(
    stageCtx: StageContext,
    homeController: Option[HomeController] = None,
    hostScene: HostScene,
    hostController: HostController,
    liveManager: ActorRef[LiveManager.LiveCommand],
    mediaPlayer: MediaPlayer,
    sender: Option[ActorRef[WsMsgFront]] = None,
    hostStatus: Int = HostStatus.NOTCONNECT, //0-直播，1-连线
    joinAudience: Option[MemberInfo] = None //组员
  )(
    implicit stashBuffer: StashBuffer[RmCommand],
    timer: TimerScheduler[RmCommand]
  ): Behavior[RmCommand] =
    Behaviors.receive[RmCommand] { (ctx, msg) =>
      msg match {
        case HostWsEstablish =>
          //与roomManager建立ws
          assert(userInfo.nonEmpty && roomInfo.nonEmpty)

          def successFunc(): Unit = {
            //            hostScene.allowConnect()
            //            Boot.addToPlatform {
            //              hostController.showScene()
            //            }
          }
          def failureFunc(): Unit = {
            //            liveManager ! LiveManager.DeviceOff
            Boot.addToPlatform {
              WarningDialog.initWarningDialog("连接失败！")
            }
          }
          val url = Routes.linkRoomManager(userInfo.get.userId, roomInfo.map(_.roomId).get)
          WsUtil.buildWebSocket(ctx, url, hostController, successFunc(), failureFunc())
          Behaviors.same

        case HostLiveReq =>

//          RoomClient.startLive(RmManager.roomInfo.get.roomId).map({
//            rsp=>
//
//
//          })
          //1.开始推流
          log.info(s"开始会议")
          liveManager ! LiveManager.PushStream("rtmp://10.1.29.247:1935/live/1000_3")

          //2.开始拉流
          liveManager ! LiveManager.PullStream("",mediaPlayer,hostScene,liveManager)
          Behaviors.same


        case GetPackageLoss =>
          liveManager ! LiveManager.GetPackageLoss
          Behaviors.same

        case StopLive =>
          liveManager ! LiveManager.StopPush
          //todo: 进行http请求，停止推流
//          sender.foreach(_ ! HostStopPushStream(roomInfo.get.roomId))
          hostController.isLive = false
          Behaviors.same

        case BackToHome =>
//          timer.cancel(HeartBeat)
//          timer.cancel(PingTimeOut)
          sender.foreach(_ ! CompleteMsgClient)
          if (hostStatus == HostStatus.CONNECT) {//开启会议情况下
            //todo: 需要关闭player的显示
//            playManager ! PlayManager.StopPlay(roomInfo.get.roomId, hostScene.resetBack, joinAudience.map(_.userId))
//            val playId = joinAudience match {
//              case Some(joinAud) =>
//                Ids.getPlayId(audienceStatus = AudienceStatus.CONNECT, roomId = Some(roomInfo.get.roomId),audienceId = Some(joinAud.userId))
//              case None =>
//                Ids.getPlayId(audienceStatus = AudienceStatus.LIVE, roomId = Some(roomInfo.get.roomId))
//            }
            //停止服务器拉流显示到player上
//            mediaPlayer.stop(playId, hostScene.resetBack)
            liveManager ! LiveManager.StopPull
          }
          liveManager ! LiveManager.StopPush
          liveManager ! LiveManager.DeviceOff
          Boot.addToPlatform {
            hostScene.stopPackageLoss()
            homeController.foreach(_.showScene())
          }
          hostScene.stopPackageLoss()
          System.gc()
          switchBehavior(ctx, "idle", idle(stageCtx, liveManager, mediaPlayer, homeController))


        case PullerStopped =>
          //停止拉流，切换到显示自己的视频流中
          assert(userInfo.nonEmpty)
          log.info(s"停止会议了！")
          val userId = userInfo.get.userId

          Behaviors.same
        case _=>
          Behaviors.unhandled

      }}

}
