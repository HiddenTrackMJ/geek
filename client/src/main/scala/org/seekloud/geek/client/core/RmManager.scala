package org.seekloud.geek.client.core

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import org.seekloud.geek.client.Boot
import org.seekloud.geek.client.common.Constants.HostStatus
import org.seekloud.geek.client.common.{AppSettings, Routes, StageContext}
import org.seekloud.geek.client.component.WarningDialog
import org.seekloud.geek.client.controller.{HomeController, HostController}
import org.seekloud.geek.client.core.stream.LiveManager
import org.seekloud.geek.client.scene.{HomeScene, HostScene}
import org.seekloud.geek.player.sdk.MediaPlayer
import org.seekloud.geek.shared.client2Manager.websocket.AuthProtocol.WsMsgFront
import org.seekloud.geek.shared.ptcl.CommonProtocol._
import org.slf4j.LoggerFactory
import org.seekloud.geek.client.Boot.{executor, materializer, scheduler, system, timeout}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}

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
  final case object GoToCreateRoom extends RmCommand //进去创建会议的页面
  final case object HostWsEstablish extends RmCommand
  final case object BackToHome extends RmCommand


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

          case GoToCreateRoom =>
            val hostScene = new HostScene(stageCtx.getStage)
            val hostController = new HostController(stageCtx, hostScene, ctx.self)

            def callBack(): Unit = Boot.addToPlatform(hostScene.changeToggleAction())

            liveManager ! LiveManager.DevicesOn(hostScene.gc, callBackFunc = Some(callBack))
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

          case BackToHome =>
            log.info("back to home.")
            Boot.addToPlatform {
              homeController.foreach(_.showScene())
            }
            System.gc()
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


  private def hostBehavior(
    stageCtx: StageContext,
    homeController: Option[HomeController] = None,
    hostScene: HostScene,
    hostController: HostController,
    liveManager: ActorRef[LiveManager.LiveCommand],
    mediaPlayer: MediaPlayer,
    sender: Option[ActorRef[WsMsgFront]] = None,
    hostStatus: Int = HostStatus.LIVE, //0-直播，1-连线
    joinAudience: List[MemberInfo] = Nil//组员
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
//          val url = Routes.linkRoomManager(userInfo.get.userId, userInfo.get.token, roomInfo.map(_.roomId).get)
//          buildWebSocket(ctx, url, Right(hostController), successFunc(), failureFunc())
          Behaviors.same
      }}

}
