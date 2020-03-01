package org.seekloud.geek.client.core

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import javafx.scene.Scene
import org.seekloud.geek.client.Boot
import org.seekloud.geek.client.common.Constants.{DeviceStatus, HostStatus}
import org.seekloud.geek.client.common.{AppSettings, Routes, StageContext}
import org.seekloud.geek.client.component.{SnackBar, WarningDialog}
import org.seekloud.geek.client.controller.GeekHostController
import org.seekloud.geek.client.core.stream.LiveManager
import org.seekloud.geek.client.core.stream.LiveManager.ChangeCaptureOption
import org.seekloud.geek.client.utils.WsUtil
import org.seekloud.geek.player.sdk.MediaPlayer
import org.seekloud.geek.shared.ptcl.CommonProtocol._
import org.seekloud.geek.shared.ptcl.WsProtocol
import org.seekloud.geek.shared.ptcl.WsProtocol._
import org.seekloud.geek.shared.ptcl.WsProtocol.{AppointReq, CompleteMsgClient, ShieldReq, StopLive4ClientReq, StopLiveReq, WsMsgFront}
import org.slf4j.LoggerFactory

import scala.collection.mutable

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
  var userInfo: Option[UserInfo] = None //只包括登录成功后的返回信息，后续的房间相关信息需要从roomInfo里面去寻找
  var isStart: Boolean = false //当前用户有没有开启会议
  var roomInfo: Option[RoomInfo] = None
  var meetingListInfo:List[MeetingInfo] = Nil //存储用户登录当前会话中参加和发起的会议信息
  val userLiveIdMap: mutable.HashMap[String, (Long)] = mutable.HashMap.empty //liveid->(userid)，存储已经拉过流的键值对信息


  //对当前的用户立碑的position进行排序
  //当用户列表变化了，房主切换了，发言人切换了，都需要重新计算一次position
  def calUserListPosition() ={
//    log.info("重新计算成员的图像的顺序")
    var x = 1
    log.info("当前的发言状态：" + roomInfo.get.modeStatus)
    roomInfo.get.userList.foreach{
      user=>
        if (roomInfo.get.modeStatus == ModeStatus.FREE){
          if (user.isHost.get){
            user.position = 0
          }else{
            user.position = x
            x += 1
          }
        }else{//申请发言模式
          if(user.isAllow.get){
            user.position = 0
          }else{
            user.position = x
            x += 1
          }
        }
    }
    log.info("排序后的userlist:" + roomInfo.get.userList)

  }
  def getCurrentUserInfo(): UserInfo = {
    assert(userInfo.nonEmpty && roomInfo.nonEmpty)
    val user = roomInfo.get.userList.find(_.userId == userInfo.get.userId)
    if (user nonEmpty){
      user.get
    }else{
      userInfo.get
    }
  }

  def getUserInfo(userId:Long) = {
    roomInfo.get.userList.find(_.userId == userId)
  }

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
  final case class StartLiveSuccess(pull:String, push:String, userLiveCodeMap: Map[String, Long]) extends RmCommand
  final case class StartLive4ClientSuccess(push:String,userLiveCodeMap: Map[String, Long]) extends RmCommand
  final case object StopLive extends RmCommand
  final case object StopLiveSuccess extends RmCommand //房主停止推流了
  final case class StopLive4ClientSuccess(userId:Long) extends RmCommand
  final case object StopLiveFailed extends RmCommand
  final case object PullerStopped extends RmCommand

  final case class Comment(comment: WsProtocol.Comment) extends RmCommand
  final case class ChangePossession(req: WsProtocol.ChangePossessionReq) extends RmCommand

  final case object GetPackageLoss extends RmCommand
  final case class ChangeMicOption(userId:Long, need: Boolean) extends RmCommand
  final case class ChangeVideoOption(userId:Long, need: Boolean) extends RmCommand

  //ws链接
  final case class GetSender(sender: ActorRef[WsMsgFront]) extends RmCommand
  final case class Shield(req:ShieldReq) extends RmCommand
  final case class Appoint4HostReply(userId:Long,status:Boolean) extends RmCommand
  final case class Appoint4Client(userId:Long,userName:String, status:Boolean) extends RmCommand
  final case class Appoint4Host(userId:Long,status:Boolean) extends RmCommand //主持人关闭某个人发言或者不发言
  final case class Appoint(req:AppointReq) extends RmCommand

  def create(stageCtx: StageContext): Behavior[RmCommand] =
    Behaviors.setup[RmCommand] { ctx =>
      log.info(s"RmManager is starting...")
      implicit val stashBuffer: StashBuffer[RmCommand] = StashBuffer[RmCommand](Int.MaxValue)
      Behaviors.withTimers[RmCommand] { implicit timer =>
        //启动client同时启动player
        val mediaPlayer = new MediaPlayer(RmManager.roomInfo)
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
    hostStatus: Int = HostStatus.NOT_CONNECT, //0-直播，1-连线
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
            Boot.addToPlatform{
              SnackBar.show(hostController.centerPane,"连接成功")
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
              SnackBar.show(hostController.centerPane,"连接失败")
            }
          }
          val url = Routes.linkRoomManager(userInfo.get.userId, roomInfo.map(_.roomId).get)
          WsUtil.buildWebSocket(ctx, url, hostController, successFunc(), failureFunc())
          Behaviors.same


        case msg:ChangeMicOption =>
          //修改推流的设置
          val user = RmManager.roomInfo.get.userList.find(_.userId == msg.userId).get

          if (msg.userId == RmManager.userInfo.get.userId){
            log.info("修改自己的声音配置")
            if(msg.need){
              SnackBar.show(hostController.centerPane,"您的语音被主持人开启")
            }else{
              SnackBar.show(hostController.centerPane,"您的语音被主持人关闭")
            }
            hostController.micStatus = if(msg.need) DeviceStatus.ON else DeviceStatus.OFF
            hostController.updateMicUI()
            liveManager ! ChangeCaptureOption(msg.userId,user.isVideo.get,user.isMic.get,()=>Unit)
          }else{//关闭player的画面或者声音
            log.info(s"修改别人${msg.userId}的player画面配置")
            if (!msg.need){//关闭声音
              mediaPlayer.pauseSound(msg.userId.toString)
            }else{//开启声音
              mediaPlayer.continueSound(msg.userId.toString)
            }
          }

          Behaviors.same


        case msg:ChangeVideoOption =>
          val user = RmManager.roomInfo.get.userList.find(_.userId == msg.userId).get

          if (msg.userId == RmManager.userInfo.get.userId){
            log.info("修改自己的画面配置")
            if(msg.need){
              SnackBar.show(hostController.centerPane,"您的画面被主持人开启")
            }else{
              SnackBar.show(hostController.centerPane,"您的画面被主持人关闭")
            }
            hostController.videoStatus = if(msg.need) DeviceStatus.ON else DeviceStatus.OFF
            hostController.updateVideoUI()
            liveManager ! ChangeCaptureOption(msg.userId,user.isVideo.get,user.isMic.get,()=>Unit)
          }else{//关闭player的画面或者声音
            log.info(s"修改别人${msg.userId}的player画面配置")
            if (!msg.need){//关闭画面
              mediaPlayer.pauseImage(msg.userId.toString)
            }else{//开启画面
              mediaPlayer.continueImage(msg.userId.toString)
            }

          }




          Behaviors.same

        case msg: Shield=>
          //屏蔽某个人声音/图像
          sender.foreach( s=> s ! msg.req)
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
            if (RmManager.getCurrentUserInfo().isHost.get){//房主
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


        case StartLiveSuccess(_, push, userLiveCodeMap)=>
          log.info(s"StartLiveSuccess:开始会议")

          if (RmManager.isStart){
            userLiveCodeMap.filter{_._2 != -1}.filter(i => !RmManager.userLiveIdMap.contains(i._1)).foreach {
              u =>
                log.info(s"开始会议的信息： $u 自己的id :${RmManager.userInfo.get.userId}")
                //用户自己的流，因为自己的不需要拉流，直接是摄像头绘制
                if (u._2 == RmManager.userInfo.get.userId){

                  //1.开始推流
                  log.info(s"StartLiveSuccess:开始会议")
                  liveManager ! LiveManager.PushStream(push)

                  //更新会议室的状态
                  hostController.hostStatus = HostStatus.CONNECT
                  hostController.updateOffUI()

                  /*媒体画面模式更改*/
                  hostController.resetBack()
//                  liveManager ! LiveManager.SwitchMediaMode(isJoin = true, reset = hostController.resetBack)

                }else{
                  liveManager ! LiveManager.PullStream(u._1, u._2.toString, mediaPlayer, hostController, liveManager)
                }
                RmManager.userLiveIdMap.put(u._1,u._2)
            }
          }else{
            log.info("StartLiveSuccess:会议未开始")
          }

          Behaviors.same

        case Appoint4HostReply(userId, status)=>
          sender.foreach(_ ! AppointReq(RmManager.roomInfo.get.roomId,userId,status))
          Behaviors.same

        case Appoint4Client(userId, userName, status) =>
          sender.foreach(_ ! Appoint4ClientReq(RmManager.roomInfo.get.roomId,userId,userName,status))

          Behaviors.same

        case Appoint4Host(userId, status) =>
          sender.foreach(_ ! AppointReq(RmManager.roomInfo.get.roomId,userId,status))
          Behaviors.same


        case StartLive4ClientSuccess(push, userLiveCodeMap)=>

          log.info(s"StartLive4ClientSuccess:开始会议")
          if (RmManager.isStart){
            userLiveCodeMap.filter(_._2 != -1).filter(i => !RmManager.userLiveIdMap.contains(i._1)).foreach {
              u =>
                log.info(s"开始会议的信息： $u 自己的id :${RmManager.userInfo.get.userId}")
                if (u._2 == RmManager.userInfo.get.userId){
                  //更新会议室的状态
                  hostController.hostStatus = HostStatus.CONNECT
                  hostController.updateOffUI()

                  /*媒体画面模式更改*/
                  hostController.resetBack()
                  //liveManager ! LiveManager.SwitchMediaMode(isJoin = true, reset = hostController.resetBack)

                  //推流
                  //1.开始推流
                  log.info(s"StartLiveSuccess:自己开始会议")
                  liveManager ! LiveManager.PushStream(push)


                }else{
                  hostController.addServerMsg(CommentInfo(-1L, "", "", s"拉流的用户id：${u._2}", -1L))
                  liveManager ! LiveManager.PullStream(u._1, u._2.toString, mediaPlayer, hostController, liveManager)
                }
                RmManager.userLiveIdMap.put(u._1, u._2)
            }
          }else{
            log.info("StartLive4ClientSuccess:会议未开始")
          }

          Behaviors.same

        case msg: Comment =>
          sender.foreach( s=> s ! msg.comment)
          Behaviors.same

        case msg: ChangePossession =>
          sender.foreach( s=> s ! msg.req)
          Behaviors.same

        case GetPackageLoss =>
          liveManager ! LiveManager.GetPackageLoss
          Behaviors.same

        case StopLive =>
          liveManager ! LiveManager.StopPush
          if (RmManager.getCurrentUserInfo().isHost.get){//房主
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
          //房主/或者自己（不是房主）退出会议
          log.info("房主/或者自己（不是房主）退出会议")
          RmManager.isStart = false
          //停止推流
          liveManager ! LiveManager.StopPush
          /*背景改变*/
          hostController.hostStatus = HostStatus.NOT_CONNECT
          hostController.updateOffUI()
          /*媒体画面模式更改*/
//          liveManager ! LiveManager.SwitchMediaMode(isJoin = false, reset = hostController.resetBack)
          val user = RmManager.getCurrentUserInfo()
          liveManager ! ChangeCaptureOption(user.userId,user.isVideo.get,user.isMic.get,()=>Unit)

          //停止所有的拉流
          mediaPlayer.stopAll(hostController.resetBack)
          liveManager ! LiveManager.StopPull

          Boot.addToPlatform {
            SnackBar.show(hostController.centerPane,"停止会议成功!")
          }
          //当前的链接状态改为未连接
          hostBehavior(stageCtx,hostScene,hostController,liveManager,mediaPlayer,sender,hostStatus=HostStatus.NOT_CONNECT,joinAudience)


        case StopLive4ClientSuccess(userId) =>
          //某个成员停止推流了
          if (userId == RmManager.userInfo.get.userId){//是自己
            ctx.self ! StopLiveSuccess
          }else{
            //停止拉该成员的流
            mediaPlayer.stop(userId.toString,hostController.resetBack)
            log.info(s"停止 ${userId} 用户的拉流")
          }


          Behaviors.same

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
            mediaPlayer.stop(playId, ()=>Unit)
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
