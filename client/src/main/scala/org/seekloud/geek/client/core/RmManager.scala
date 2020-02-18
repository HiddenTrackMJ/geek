package org.seekloud.geek.client.core

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import javafx.scene.Scene
import org.seekloud.geek.client.Boot
import org.seekloud.geek.client.common.Constants.HostStatus
import org.seekloud.geek.client.common.{AppSettings, Routes, StageContext}
import org.seekloud.geek.client.component.WarningDialog
import org.seekloud.geek.client.controller.{GeekHostController, GeekUserController}
import org.seekloud.geek.client.core.stream.LiveManager
import org.seekloud.geek.client.utils.WsUtil
import org.seekloud.geek.player.sdk.MediaPlayer
import org.seekloud.geek.shared.ptcl.CommonProtocol._
import org.seekloud.geek.shared.ptcl.WsProtocol
import org.seekloud.geek.shared.ptcl.WsProtocol.{CompleteMsgClient, StopLive4ClientReq, StopLiveReq, WsMsgFront}
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
  var meetingListInfo:List[MeetingInfo] = Nil //存储用户登录当前会话中参加和发起的会议信息


  private[this] def switchBehavior(ctx: ActorContext[RmCommand],
    behaviorName: String,
    behavior: Behavior[RmCommand])
    (implicit stashBuffer: StashBuffer[RmCommand]) = {
    log.debug(s"${ctx.self.path} becomes $behaviorName behavior.")
    stashBuffer.unstashAll(ctx, behavior)
  }


  //拿到homeController 和 homeScreen
  final case class SignInSuccess(userInfo: Option[UserInfo] = None, roomInfo: Option[RoomInfo] = None) extends RmCommand
  final case object Logout extends RmCommand
  final case object GoToCreateAndJoinRoom extends RmCommand //进去创建会议的页面
//  final case object GoToJoinRoom extends RmCommand //进去加入会议的页面

  final case object HostWsEstablish extends RmCommand
  final case object BackToHome extends RmCommand
  final case object HostLiveReq extends RmCommand //请求开启会议
  final case class StartLiveSuccess(pull:String, push:String) extends RmCommand
  final case object StopLive extends RmCommand
  final case object StopLiveSuccess extends RmCommand
  final case object StopLiveFailed extends RmCommand
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
  )(
    implicit stashBuffer: StashBuffer[RmCommand],
    timer: TimerScheduler[RmCommand]
  ):Behavior[RmCommand] = {
    Behaviors.receive[RmCommand]{
      (ctx, msg) =>
        msg match {

          case GoToCreateAndJoinRoom =>
            val hostController = new GeekHostController(ctx.self,stageCtx,Some((gc,callBack)=>{
              liveManager ! LiveManager.DevicesOn(gc, callBackFunc = callBack)
            }))
            val hostScene = stageCtx.createSceneByFxml(hostController,"scene/geek-host.fxml")

            //建立ws连接
            ctx.self ! HostWsEstablish
            Boot.addToPlatform {
              //显示会议厅页面
              stageCtx.showScene(scene = hostScene)
            }
            switchBehavior(ctx, "hostBehavior", hostBehavior(stageCtx, hostScene, hostController, liveManager, mediaPlayer))

          case SignInSuccess(userInfo, roomInfo)=>
            //todo 可以进行登录后的一些处理，比如创建临时文件等，这部分属于优化项
            Behaviors.same



          case Logout =>
            log.info(s"退出登录.")
            this.roomInfo = None
            this.userInfo = None
            //todo: 回退到登录界面
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
    hostScene: Scene,
    hostController: GeekHostController,
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
            Boot.addToPlatform {
              WarningDialog.initWarningDialog("连接成功！")
            }
            //            hostScene.allowConnect()
            //            Boot.addToPlatform {
            //              hostController.showScene()
            //            }
            sender.get ! CompleteMsgClient
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

        case msg: GetSender =>
          //添加给后端发消息的对象sender
          log.info("获取到后端消息对象")
//          msg.sender ! WsProtocol.Test("I'm telling you")
          hostBehavior(stageCtx, hostScene, hostController, liveManager, mediaPlayer, Some(msg.sender), hostStatus)


        case HostLiveReq =>
          //ws请求服务器获取拉流地址
          log.info("ws-client:请求开始会议")
          if (sender nonEmpty){
            if (RmManager.userInfo.get.isHost.get){//房主
              sender.get ! WsProtocol.StartLiveReq(RmManager.roomInfo.get.roomId)
            }else{
              sender.get ! WsProtocol.StartLive4ClientReq(RmManager.roomInfo.get.roomId,RmManager.userInfo.get.userId)
            }
          }else{
            Boot.addToPlatform {
              WarningDialog.initWarningDialog("与ws连接失败，请退出重新创建/进入房间！")
            }
          }


          Behaviors.same


        case StartLiveSuccess(pull, push)=>

          //更新会议室的状态
          hostController.toggleLive()

          //1.开始推流
          log.info(s"开始会议")
          liveManager ! LiveManager.PushStream(push)

          //2.开始拉流：
          RmManager.userInfo.get.pullStream = Some(pull)
          liveManager ! LiveManager.PullStream(RmManager.userInfo.get.pullStream.get,mediaPlayer,hostController,liveManager)
          Behaviors.same


        case GetPackageLoss =>
          liveManager ! LiveManager.GetPackageLoss
          Behaviors.same

        case StopLive =>
          liveManager ! LiveManager.StopPush
          if (RmManager.userInfo.get.isHost.get){//房主
            log.info("房主停止推流")
            if (sender nonEmpty){
              sender.get ! StopLiveReq(RmManager.roomInfo.get.roomId)
            }else{
              log.info("出现逻辑问题，sender为空")
            }

          }else{//普通成员
            log.info("普通用户停止推流")
            sender.get ! StopLive4ClientReq(RmManager.roomInfo.get.roomId,RmManager.userInfo.get.userId)
          }


          Behaviors.same

        case StopLiveSuccess =>
          //房主/普通组员均一样
          /*背景改变*/
          hostController.toggleLive()
          /*媒体画面模式更改*/
          liveManager ! LiveManager.SwitchMediaMode(isJoin = false, reset = hostController.resetBack)

          if (hostStatus == HostStatus.CONNECT) {//开启会议情况下
            val playId = RmManager.roomInfo.get.roomId.toString
            //停止服务器拉流显示到player上
            mediaPlayer.stop(playId, hostController.resetBack)
            liveManager ! LiveManager.StopPull
          }
          Boot.addToPlatform {
            WarningDialog.initWarningDialog("停止会议成功！")
          }
          //当前的链接状态改为未连接
          hostBehavior(stageCtx,hostScene,hostController,liveManager,mediaPlayer,sender,hostStatus=HostStatus.NOTCONNECT,joinAudience)

        case StopLiveFailed =>

          Boot.addToPlatform {
            WarningDialog.initWarningDialog("停止会议失败！")
          }

          Behaviors.same


        case BackToHome =>

          sender.foreach(_ ! CompleteMsgClient)//断开ws连接
          if (hostStatus == HostStatus.CONNECT) {//开启会议情况下
            //需要关闭player的显示
            //停止服务器拉流显示到player上
            val playId = RmManager.roomInfo.get.roomId.toString
            mediaPlayer.stop(playId, hostController.resetBack)
            liveManager ! LiveManager.StopPull
          }
          liveManager ! LiveManager.StopPush
          liveManager ! LiveManager.DeviceOff

          Boot.addToPlatform {
            //返回user界面
            SceneManager.showUserScene(stageCtx,ctx.self)
          }
          System.gc()
          switchBehavior(ctx, "idle", idle(stageCtx, liveManager, mediaPlayer))


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
