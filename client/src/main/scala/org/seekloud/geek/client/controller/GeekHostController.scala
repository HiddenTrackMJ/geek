package org.seekloud.geek.client.controller

import java.awt.TrayIcon.MessageType

import akka.actor.typed.ActorRef
import com.jfoenix.controls.JFXListView
import javafx.fxml.FXML
import javafx.scene.canvas.{Canvas, GraphicsContext}
import javafx.scene.control.Label
import javafx.scene.layout.{AnchorPane, Background, BackgroundFill, GridPane}
import javafx.scene.paint.Color
import org.kordamp.ikonli.javafx.FontIcon
import org.seekloud.geek.client.Boot
import org.seekloud.geek.client.common.StageContext
import org.seekloud.geek.client.component.bubble.BubbledLabel
import org.seekloud.geek.client.component.{AvatarColumn, Loading, WarningDialog}
import org.seekloud.geek.client.core.RmManager
import org.seekloud.geek.client.core.RmManager.{StartLive, StopLiveFailed, StopLiveSuccess}
import org.seekloud.geek.shared.ptcl.CommonProtocol.{CommentInfo, UserInfo}
import org.seekloud.geek.shared.ptcl.WsProtocol._
import org.slf4j.LoggerFactory
/**
  * User: hewro
  * Date: 2020/2/16
  * Time: 23:24
  * Description: 会议厅界面的控制器
  */
class GeekHostController(
  rmManager: ActorRef[RmManager.RmCommand],
  context: StageContext,
  initSuccess:Option[(GraphicsContext,Option[() => Unit]) => Unit] = None
) {



  private val log = LoggerFactory.getLogger(this.getClass)


  @FXML var canvas: Canvas = _
  @FXML private var centerPane: AnchorPane = _

  @FXML private var mic: FontIcon = _
  @FXML private var video: FontIcon = _
  @FXML private var off: FontIcon = _
  @FXML private var invite: FontIcon = _
  @FXML private var record: FontIcon = _

  @FXML private var mic_label: Label = _
  @FXML private var video_label: Label = _
  @FXML private var off_label: Label = _
  @FXML private var invite_label: Label = _
  @FXML private var record_label: Label = _
  @FXML private var userListPane: AnchorPane = _
  @FXML private var commentPane: AnchorPane = _




  var loading:Loading = _
  var gc: GraphicsContext = _


  //改变底部工具栏的图标和文字,
  //CaptureStartSuccess 摄像头启动成功的时候会执行这个
  def changeToggleAction(): Unit = {

  }


  def initialize(): Unit = {
    gc = canvas.getGraphicsContext2D
    loading = Loading(centerPane).build()

    if (initSuccess nonEmpty){
      //给liveManager ! LiveManager.DevicesOn(gc, callBackFunc = callBack)，
      // callBackFunc即changeToggleAction
      initSuccess.foreach(func => func(gc,Some(()=>changeToggleAction())))
    }

    //todo 根据用户类型锁定一些内容/按钮的事件修改
    initUserList()
    initCommentList()
  }


  //更新界面
  def initUserList() = {
    //后续从服务器端的ws链接中获取和更新
    val userList = List(
      UserInfo(1L, "何为", ""),
      UserInfo(2L, "秋林会", ""),
      UserInfo(3L, "薛甘愿", ""),
    )

    val jList = new JFXListView[GridPane]
    userList.foreach{
      t=>
        //创建一个AnchorPane
        val pane = AvatarColumn(t,userListPane.getPrefWidth - 30)()
        pane.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, null, null)))
        jList.getItems.add(pane)
    }
    jList.setPrefWidth(userListPane.getPrefWidth)
    jList.setPrefHeight(userListPane.getPrefHeight)
    jList.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, null, null)))
    userListPane.getChildren.add(jList)

  }

  def initCommentList() = {
    //后续从服务器端的ws链接中获取和更新
    val commentList = List(
      CommentInfo(1L,"何为","","大家新年好",1232132L)
    )

    val jList = new JFXListView[BubbledLabel]
    commentList.foreach{
      t=>
        //创建一个AnchorPane
        val bl6 = new BubbledLabel
        bl6.setText(t.userName + ": " + t.content)
        bl6.setBackground(new Background(new BackgroundFill(Color.WHITE, null, null)))
        bl6.getStyleClass.add("commentBubble")
        jList.getItems.add(bl6)
    }
    jList.setPrefWidth(commentPane.getPrefWidth)
    jList.setPrefHeight(commentPane.getPrefHeight)
    jList.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, null, null)))
    commentPane.getChildren.add(jList)
  }




  //回到首页
  def gotoHomeScene(): Unit = {
    //回到首页
    rmManager ! RmManager.BackToHome
  }

  //发表评论
  def commentSubmit() = {

  }



  //接收处理与【后端发过来】的消息
  def wsMessageHandle(data: WsMsgRm): Unit = {
    data match {

      case msg: HeatBeat =>
      //        log.debug(s"heartbeat: ${msg.ts}")
      //        rmManager ! HeartBeat

      case msg: StartLiveRsp =>
        //房主收到的消息
        log.debug(s"get StartLiveRsp: $msg")
        if (msg.errCode == 0) {
          //开始直播
          rmManager ! StartLive(msg.rtmp.serverUrl+msg.rtmp.stream,msg.rtmp.serverUrl+msg.selfCode)
        } else {
          Boot.addToPlatform {
            WarningDialog.initWarningDialog(s"${msg.msg}")
          }
        }

      case msg: StartLive4ClientRsp =>
        log.info(s"收到后端的开始会议消息")
        if (msg.errCode == 0) {
          //开始直播
          rmManager ! StartLive(msg.rtmp.get.serverUrl+msg.rtmp.get.stream,msg.rtmp.get.serverUrl+msg.selfCode)
        } else {
          Boot.addToPlatform {
            WarningDialog.initWarningDialog(s"${msg.msg}")
          }
        }


      case msg: StopLiveRsp =>
        if (msg.errCode == 0){
          Boot.addToPlatform {
            WarningDialog.initWarningDialog("停止会议成功！")
          }
          log.info(s"普通用户停止推流成功")
          rmManager ! StopLiveSuccess
        }else{
          rmManager ! StopLiveFailed
        }



      case msg: StopLive4ClientRsp =>
        if (msg.errCode == 0){
          log.info("普通用户停止推流成功")
          rmManager ! StopLiveSuccess
        }else{
          rmManager ! StopLiveFailed
        }





      //
      //      case msg: ModifyRoomRsp =>
      //        //若失败，信息改成之前的信息
      ////        log.debug(s"get ModifyRoomRsp: $msg")
      //        if (msg.errCode == 0) {
      //          //          log.debug(s"更改房间信息成功！")
      //          Boot.addToPlatform {
      //            WarningDialog.initWarningDialog("更改房间信息成功！")
      //          }
      //          // do nothing
      //        } else {
      //          log.debug(s"更改房间信息失败！原房间信息为：${hostScene.roomInfoMap}")
      //          Boot.addToPlatform {
      //            val roomName = hostScene.roomInfoMap(RmManager.roomInfo.get.roomId).head
      //            val roomDes = hostScene.roomInfoMap(RmManager.roomInfo.get.roomId)(1)
      //            hostScene.roomNameField.setText(roomName)
      //            hostScene.roomDesArea.setText(roomDes)
      //          }
      //        }
      //
      //      case msg: ChangeModeRsp =>
      //        if (msg.errCode != 0) {
      //          Boot.addToPlatform {
      //            WarningDialog.initWarningDialog("该项设置目前不可用！")
      //          }
      //        }
      //
      //      case msg: AudienceJoin =>
      //        //将该条信息展示在host页面(TableView)
      //        log.debug(s"Audience-${msg.userName} send join req.")
      //        Boot.addToPlatform {
      //          hostScene.updateAudienceList(msg.userId, msg.userName)
      //        }
      //
      //
      //      case msg: AudienceJoinRsp =>
      //        if (msg.errCode == 0) {
      //          //显示连线观众信息
      //          rmManager ! RmManager.JoinBegin(msg.joinInfo.get)
      //
      //          Boot.addToPlatform {
      //            if (!hostScene.tb3.isSelected) {
      //              hostScene.tb3.setGraphic(hostScene.connectionIcon1)
      //            }
      //            hostScene.connectionStateText.setText(s"与${msg.joinInfo.get.userName}连线中")
      //            hostScene.connectStateBox.getChildren.add(hostScene.shutConnectionBtn)
      //            isConnecting = true
      //          }
      //
      //        } else {
      //          Boot.addToPlatform {
      //            WarningDialog.initWarningDialog(s"观众加入出错:${msg.msg}")
      //          }
      //        }
      //
      //      case AudienceDisconnect =>
      //        //观众断开，提醒主播，去除连线观众信息
      //        rmManager ! RmManager.JoinStop
      //        Boot.addToPlatform {
      //          if (!hostScene.tb3.isSelected) {
      //            hostScene.tb3.setGraphic(hostScene.connectionIcon1)
      //          }
      //          hostScene.connectionStateText.setText(s"目前状态：无连接")
      //          hostScene.connectStateBox.getChildren.remove(hostScene.shutConnectionBtn)
      //          isConnecting = false
      //        }
      //
      //      case msg: RcvComment =>
      //        //判断userId是否为-1，是的话当广播处理
      ////        log.info(s"receive comment msg: ${msg.userName}-${msg.comment}")
      //        Boot.addToPlatform {
      //          hostScene.commentBoard.updateComment(msg)
      //          hostScene.barrage.updateBarrage(msg)
      //        }
      //
      //      case msg: UpdateAudienceInfo =>
      ////        log.info(s"update audienceList.")
      //        Boot.addToPlatform {
      //          hostScene.watchingList.updateWatchingList(msg.AudienceList)
      //        }
      //
      //
      //      case msg: ReFleshRoomInfo =>
      ////        log.debug(s"host receive likeNum update: ${msg.roomInfo.like}")
      //        likeNum = msg.roomInfo.like
      //        Boot.addToPlatform {
      //          hostScene.likeLabel.setText(likeNum.toString)
      //        }
      //
      //      case HostStopPushStream2Client =>
      //        Boot.addToPlatform {
      //          WarningDialog.initWarningDialog("直播成功停止，已通知所有观众。")
      //        }
      //
      //      case BanOnAnchor =>
      //        Boot.addToPlatform {
      //          WarningDialog.initWarningDialog("你的直播已被管理员禁止！")
      //        }
      //        rmManager ! RmManager.BackToHome

      case x =>
        log.warn(s"host recv unknown msg from rm: $x")
    }

  }


  //开始会议后，界面可以做的一些修改
  def resetBack() = {

  }

}
