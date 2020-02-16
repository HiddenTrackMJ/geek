package org.seekloud.geek.client.controller

import javafx.fxml.FXML
import javafx.scene.control.Label
import org.seekloud.geek.client.component.{Loading, WarningDialog}
import org.seekloud.geek.client.core.RmManager
import org.seekloud.geek.client.utils.{RMClient, RoomClient}
import org.seekloud.geek.shared.ptcl.CommonProtocol.RoomInfo
import org.seekloud.geek.shared.ptcl.RoomProtocol.RoomUserInfo
import org.slf4j.LoggerFactory
import akka.actor.typed.ActorRef
import org.seekloud.geek.client.Boot.executor



/**
  * User: hewro
  * Date: 2020/2/16
  * Time: 16:36
  * Description: 用户登录后的界面的控制器
  */
class GeekUserController(
  rmManager: ActorRef[RmManager.RmCommand]
) {
  private val log = LoggerFactory.getLogger(this.getClass)

  @FXML private var username: Label = _

  var loading:Loading = _


  def initialize(): Unit = {
  //显示用户信息
    if (RmManager.userInfo.isEmpty){
      //取消所有的点击事件
      //回退到首页

    }else{
      username.setText(RmManager.userInfo.get.userName)
    }
  }

  /**
    * 发起会议
    */
  def createRoom(): Unit = {
    if (RmManager.userInfo.nonEmpty) {
      //创建房间
      log.info("创建房间")
      loading.showLoading()
      val userId = RmManager.userInfo.get.userId
      val roomName = s"$userId 的会议间"
      val roomDesc = "大家好才是真的好"
      RoomClient.createRoom(userId,RoomUserInfo(userId,roomName,roomDesc)).map{
        case Right(rsp) =>
          RmManager.roomInfo = Some(RoomInfo(rsp.roomId,roomName,roomDesc,userId,RmManager.userInfo.get.userName,"",observerNum=0))
          //当前用户是房主
          loading.removeLoading()
          RmManager.userInfo.get.isHost = Some(true)
          RmManager.userInfo.get.pushStream = Some(RMClient.getPushStream(rsp.liveCode))
          rmManager ! RmManager.GoToCreateAndJoinRoom

        case Left(error: Error) =>
          log.error(s"创建房间错误$error")
          WarningDialog.initWarningDialog(s"网络请求错误")
      }

    } else {//跳转到登录界面

//      gotoLoginDialog(isToLive = true)
    }
  }

  def showLoginScene() = {

  }

}
