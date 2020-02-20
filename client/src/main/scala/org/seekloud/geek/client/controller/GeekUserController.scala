package org.seekloud.geek.client.controller

import javafx.fxml.FXML
import javafx.scene.control.Label
import org.seekloud.geek.client.component.{InputDialog, Loading, SnackBar, TopBar, WarningDialog}
import org.seekloud.geek.client.core.RmManager
import org.seekloud.geek.client.utils.{RMClient, RoomClient, TimeUtil}
import org.seekloud.geek.shared.ptcl.CommonProtocol.{MeetingInfo, RoomInfo}
import org.seekloud.geek.shared.ptcl.RoomProtocol.RoomUserInfo
import org.slf4j.LoggerFactory
import akka.actor.typed.ActorRef
import javafx.scene.image.ImageView
import javafx.scene.layout.{AnchorPane, VBox}
import org.seekloud.geek.client.Boot.executor
import org.seekloud.geek.client.common.StageContext
import com.jfoenix.controls.JFXListView
import javafx.geometry.Insets
import javafx.scene.paint.Color


/**
  * User: hewro
  * Date: 2020/2/16
  * Time: 16:36
  * Description: 用户登录后的界面的控制器
  */
class GeekUserController(
  rmManager: ActorRef[RmManager.RmCommand],
  context: StageContext,
) extends CommonController {
  private val log = LoggerFactory.getLogger(this.getClass)

  @FXML private var username: Label = _
  @FXML private var rootPane: AnchorPane = _
  @FXML private var contentPane: AnchorPane = _
  @FXML private var empty: ImageView = _

  var loading:Loading = _


  def initialize(): Unit = {
    loading = Loading(rootPane).build()

    log.info("initialize GeekUserController")
    //根据内存的信息显示会议列表
    showMeetingList()

    //显示用户信息
    if (RmManager.userInfo.isEmpty){
      //取消所有的点击事件
      //回退到首页
    }else{
      username.setText(RmManager.userInfo.get.userName)
    }
    initToolbar()
  }

  def initToolbar() = {
    val toolbar = TopBar("geek云会议", Color.TRANSPARENT, rootPane.getPrefWidth, 30, "user", context, rmManager)()
    rootPane.getChildren.add(toolbar)
  }


  def showMeetingList() = {

    val list = RmManager.meetingListInfo
//    val list = List(
//      MeetingInfo("会议1","1003",1581920012628L),
//      MeetingInfo("会议2","1004",1581920012628L)
//    )

    if (list nonEmpty) {
      //空layout不显示
      empty.setVisible(false)
      empty.setManaged(false)

      val jList = new JFXListView[VBox]
      list.foreach{
        t=>
          //创建一个AnchorPane
          val pane = new VBox()
          pane.getChildren.addAll(new Label(s"名称: ${t.name}"),new Label(s"参与时间: ${TimeUtil.timeStamp2DetailDate(t.time)}"))
          pane.setPadding(new Insets(10,10,10,10))
          jList.getItems.add(pane)
      }
      jList.getStyleClass.add("mylistview")
      jList.setPrefWidth(contentPane.getPrefWidth)
      jList.setPrefHeight(contentPane.getPrefHeight)
      contentPane.getChildren.add(jList)
    }else{
      empty.setVisible(true)
      empty.setManaged(true)
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
          //添加到历史记录列表
          RmManager.meetingListInfo = RmManager.meetingListInfo :+ MeetingInfo(roomName,rsp.roomId.toString,System.currentTimeMillis())


        case Left(error) =>
          log.error(s"创建房间错误$error")
          loading.removeLoading()
          SnackBar.show(rootPane,"网络请求错误")
//          WarningDialog.initWarningDialog(s"网络请求错误")
      }

    } else {//跳转到登录界面
//      gotoLoginDialog(isToLive = true)
    }
  }

  def joinRoom() = {
    if (RmManager.userInfo.nonEmpty) {
      //创建房间
      log.info("加入房间")
      val roomId = InputDialog(stage = context.getStage).build()
      if (roomId.nonEmpty && roomId.get.trim.replaceAll(" ","").replaceAll("\n","") !=""){
        //跳转到会议室的界面
        loading.showLoading()

        RoomClient.joinRoom(roomId.get.toLong,RmManager.userInfo.get.userId).map {
          case Right(rsp) =>
            loading.removeLoading()
            if (rsp.rtmp.nonEmpty){
              //修改用户信息不是房主
              val roomUser = rsp.rtmp.get.roomUserInfo
              //todo: 房主的用户名的信息没有，也没有当前的房间参与人数
              RmManager.roomInfo = Some(RoomInfo(roomId.get.toLong,roomUser.roomName,roomUser.des,roomUser.userId,"路人甲",observerNum = 1))
              RmManager.userInfo.get.isHost = Some(false)
              //跳转到视频页面
              rmManager ! RmManager.GoToCreateAndJoinRoom

              //添加到历史记录列表
              RmManager.meetingListInfo = RmManager.meetingListInfo :+ MeetingInfo(roomUser.roomName,roomId.get,System.currentTimeMillis())


            }else{
              WarningDialog.initWarningDialog(s"没有该房间号")
            }

          case Left(error) =>
            //请求失败
            loading.removeLoading()
            log.error(s"加入房间错误：$error")
            WarningDialog.initWarningDialog(s"网络错误")
        }
      }else{
        //出现了错误
//        SnackBar.show(rootPane,"")
      }
    }else{
      SnackBar.show(rootPane,"你还没有登录")
    }
  }
}
