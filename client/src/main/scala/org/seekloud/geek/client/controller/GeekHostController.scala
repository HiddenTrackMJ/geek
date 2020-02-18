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
import org.seekloud.geek.client.common.Constants.HostStatus
import org.seekloud.geek.client.common.StageContext
import org.seekloud.geek.client.component.bubble.{BubbleSpec, BubbledLabel}
import org.seekloud.geek.client.component.{AvatarColumn, Loading, WarningDialog}
import org.seekloud.geek.client.core.RmManager
import org.seekloud.geek.client.core.RmManager.{HostLiveReq, StartLiveSuccess, StopLive, StopLiveFailed, StopLiveSuccess}
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


  val commentList = List(
    CommentInfo(2,"何为","","大家新年好",1232132L)
  )

  val commentJList = new JFXListView[BubbledLabel]

  var hostStatus = HostStatus.NOTCONNECT

  var loading:Loading = _
  var gc: GraphicsContext = _


  //改变底部工具栏的图标和文字,
  //CaptureStartSuccess 摄像头启动成功的时候会执行这个
  def changeToggleAction(): Unit = {
    mic_label.setText("关闭音频")
    mic.setIconColor(Color.WHITE)

    video_label.setText("关闭摄像头")
    video.setIconColor(Color.WHITE)
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

    commentList.foreach{
      t=>
        //创建一个AnchorPane
        val bl6 = new BubbledLabel
        bl6.setText(t.userName + ": " + t.content)
        if (t.userId == RmManager.userInfo.get.userId){
          bl6.setBackground(new Background(new BackgroundFill(Color.GREEN, null, null)))//自己消息是绿色的
          bl6.setBubbleSpec(BubbleSpec.FACE_RIGHT_CENTER);//自己消息在右侧
        }else{
          bl6.setBackground(new Background(new BackgroundFill(Color.WHITE, null, null)))
          bl6.setBubbleSpec(BubbleSpec.FACE_LEFT_BOTTOM);//别人的消息在左侧
        }
        bl6.getStyleClass.add("commentBubble")
    }
    commentJList.setPrefWidth(commentPane.getPrefWidth)
    commentJList.setPrefHeight(commentPane.getPrefHeight)
    commentJList.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, null, null)))
    commentPane.getChildren.add(commentJList)
  }


  def createCommentBubble = {

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
          rmManager ! StartLiveSuccess(msg.rtmp.serverUrl+msg.rtmp.stream,msg.rtmp.serverUrl+msg.selfCode)
        } else {
          Boot.addToPlatform {
            WarningDialog.initWarningDialog(s"${msg.msg}")
          }
        }

      case msg: StartLive4ClientRsp =>
        log.info(s"收到后端的开始会议消息")
        if (msg.errCode == 0) {
          //开始直播
          rmManager ! StartLiveSuccess(msg.rtmp.get.serverUrl+msg.rtmp.get.stream,msg.rtmp.get.serverUrl+msg.selfCode)
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


      case x =>
        log.warn(s"host recv unknown msg from rm: $x")
    }

  }


  //根据会议的当前状态判断点击后执行的操作
  def toggleLive() = {
    hostStatus match {
      case HostStatus.NOTCONNECT =>
        //开始会议
        rmManager ! HostLiveReq

        off_label.setText("连接中……")
        off.setIconColor(Color.GRAY)
        hostStatus = HostStatus.LOADING

      case HostStatus.LOADING =>

        //修改界面
        off_label.setText("结束会议")
        off.setIconColor(Color.RED)
        hostStatus = HostStatus.CONNECT


      case HostStatus.CONNECT =>
        //结束会议
        rmManager ! StopLive
        off_label.setText("开始会议")
        off.setIconColor(Color.GREEN)
        hostStatus = HostStatus.NOTCONNECT

    }

  }

  //开始会议后，界面可以做的一些修改，结束会议，界面需要做的一些修改
  def resetBack() = {

  }

}
