package org.seekloud.geek.client.core

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer}
import org.seekloud.geek.client.common.{AppSettings, StageContext}
import org.seekloud.geek.client.controller.HomeController
import org.seekloud.geek.client.scene.HomeScene
import org.seekloud.geek.player.sdk.MediaPlayer
import org.seekloud.geek.shared.ptcl.CommonProtocol._
import org.slf4j.LoggerFactory

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
  def create(stageCtx: StageContext): Behavior[RmCommand] =
    Behaviors.setup[RmCommand] { ctx =>
      log.info(s"RmManager is starting...")
      implicit val stashBuffer: StashBuffer[RmCommand] = StashBuffer[RmCommand](Int.MaxValue)
      Behaviors.withTimers[RmCommand] { implicit timer =>
        Behaviors.receive[RmCommand]{
          (ctx, msg) =>
            msg match {
              case GetHomeItems(homeScene, homeController) =>
                idle(stageCtx,homeController)

              case _=>
                log.info("收到未知消息create")
                Behaviors.same
            }
        }
//        val mediaPlayer = new MediaPlayer()
//        mediaPlayer.init(isDebug = AppSettings.playerDebug, needTimestamp = AppSettings.needTimestamp)
//        val liveManager = ctx.spawn(LiveManager.create(ctx.self, mediaPlayer), "liveManager")
//        idle(stageCtx, liveManager, mediaPlayer)
      }
    }

  def idle(
    stageCtx: StageContext,
    homeController: HomeController
  ):Behavior[RmCommand] = {
    Behaviors.receive[RmCommand]{
      (ctx, msg) =>
        msg match {
          case SignInSuccess(userInfo, roomInfo)=>
            //todo 可以进行登录后的一些处理，比如创建临时文件等，这部分属于优化项
            Behaviors.same

          case Logout =>
            log.info(s"退出登录.")
            this.roomInfo = None
            this.userInfo = None
            homeController.showScene()
            Behaviors.same

          case _=>
            log.info("收到未知消息idle")
            Behaviors.same
        }
    }
  }

}
