package org.seekloud.geek.client.controller

import akka.actor.typed.ActorRef
import com.jfoenix.controls.{JFXListView, JFXTextArea}
import javafx.fxml.FXML
import javafx.scene.canvas.{Canvas, GraphicsContext}
import javafx.scene.control.Label
import javafx.scene.layout.{AnchorPane, Background, BackgroundFill, BorderPane, GridPane}
import javafx.scene.paint.Color
import org.kordamp.ikonli.javafx.FontIcon
import org.seekloud.geek.client.Boot
import org.seekloud.geek.client.common.Constants.{DeviceStatus, HostStatus}
import org.seekloud.geek.client.common.{Constants, StageContext}
import org.seekloud.geek.client.component._
import org.seekloud.geek.client.core.RmManager
import org.seekloud.geek.client.core.RmManager._
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
) extends CommonController{



  private val log = LoggerFactory.getLogger(this.getClass)



  var canvas: Canvas = _
  @FXML var centerPane: BorderPane = _

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
  @FXML private var commentInput: JFXTextArea = _
  @FXML private var rootPane: AnchorPane = _


  val commentList = List(
    CommentInfo(1,"何为","","大家新年好",1232132L)
  )

  var commentJList = new JFXListView[GridPane]

  var hostStatus: Int = HostStatus.NOT_CONNECT
  var micStatus: Int = DeviceStatus.NOT_READY //音频状态，false未开启
  var videoStatus: Int = DeviceStatus.NOT_READY //视频状态，false未开启摄像头

  var loading:Loading = _
  var gc: GraphicsContext = _


  //改变底部工具栏的图标和文字,
  //CaptureStartSuccess 摄像头启动成功的时候会执行这个
  def changeToggleAction(): Unit = {

    micStatus = DeviceStatus.ON
    videoStatus = DeviceStatus.ON

    updateVideoUI()
    updateMicUI()
  }

  def updateVideoUI() = {

    Boot.addToPlatform {
      videoStatus match {
        case DeviceStatus.NOT_READY =>
          video.setIconLiteral("fas-video")
          video.setIconColor(Color.GRAY)
          video_label.setText("摄像头准备中")


        case DeviceStatus.ON =>
          log.info("摄像头准备好了")
          video_label.setText("关闭摄像头")
          video.setIconLiteral("fas-video")
          video.setIconColor(Color.WHITE)


        case DeviceStatus.OFF =>
          video_label.setText("开启摄像头")
          video.setIconLiteral("fas-eye-slash")
          video.setIconColor(Color.WHITE)
      }
    }

  }

  def updateMicUI() = {

    Boot.addToPlatform{
      micStatus match {
        case DeviceStatus.NOT_READY =>
          mic.setIconLiteral("fas-microphone")
          mic.setIconColor(Color.GRAY)
          mic_label.setText("音频准备中")



        case DeviceStatus.ON =>
          log.info("设备准备好了")
          mic_label.setText("关闭音频")
          mic.setIconLiteral("fas-microphone")
          mic.setIconColor(Color.WHITE)


        case DeviceStatus.OFF =>
          mic_label.setText("开启音频")
          mic.setIconLiteral("fas-microphone-slash")
          mic.setIconColor(Color.WHITE)
      }
    }
  }


  def initialize(): Unit = {
    //按照比例设置高度
    val width = centerPane.getPrefWidth
    val height = width * Constants.DefaultPlayer.height / Constants.DefaultPlayer.width
    log.info("宽度" + width + "高度" + height )
    canvas = new Canvas(width,height)
    centerPane.setCenter(canvas)

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

    initToolbar()
  }


  def initToolbar() = {
    val toolbar = TopBar(s"会议厅（会议号：${RmManager.roomInfo.get.roomId}）", Color.BLACK, rootPane.getPrefWidth-10, 25, "host", context, rmManager)()
    rootPane.getChildren.add(toolbar)
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
        val pane = AvatarColumn(t,userListPane.getPrefWidth - 30,centerPane)()
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
        log.info("宽度：" + commentPane.getPrefWidth)
        commentJList.getItems.add(CommentColumn(commentPane.getPrefWidth - 30 ,t)())
    }
    commentJList.setPrefWidth(commentPane.getPrefWidth)
//    commentJList.
    commentJList.setPrefHeight(commentPane.getPrefHeight)
    commentJList.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, null, null)))
    commentPane.getChildren.add(commentJList)
  }



  //回到首页
  def gotoHomeScene(): Unit = {
    //回到首页
    rmManager ! RmManager.BackToHome
  }

  //发表评论
  def commentSubmit() = {
    //获得当前的评论消息，添加一个新的comment结构加入到jList中
    val content = commentInput.getText
    if (content.trim.replaceAll(" ","").replaceAll("\n","") ==""){
      //提示内容为空

    }else{
      val t = CommentInfo(RmManager.userInfo.get.userId,RmManager.userInfo.get.userName,RmManager.userInfo.get.headImgUrl,content,System.currentTimeMillis())
      addComment(t)
      commentInput.setText("")//清空输入框
    }
  }


  def addComment(t:CommentInfo) = {
    Boot.addToPlatform{
        commentJList.getItems.add(CommentColumn(commentPane.getPrefWidth - 30,t)())
    }
  }


  //接收处理与【后端发过来】的消息
  def wsMessageHandle(data: WsMsgRm): Unit = {
    data match {

      case _: HeatBeat =>
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
            SnackBar.show(centerPane,"停止会议成功")
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

  //点击音频按钮：根据设备的状态
  def toggleMic() = {
    micStatus match {
      case DeviceStatus.ON =>
        //todo 关闭音频
        micStatus = DeviceStatus.OFF


      case DeviceStatus.OFF =>
        //todo 开启音频
        micStatus = DeviceStatus.ON

      case _=>

    }
    updateMicUI()

  }

  //点击视频按钮：根据设备的状态
  def toggleVideo() = {
    videoStatus match {
      case DeviceStatus.ON =>
      //todo 关闭摄像头
        videoStatus = DeviceStatus.OFF

      case DeviceStatus.OFF =>
      // todo开启摄像头
        videoStatus = DeviceStatus.ON

      case _=>

    }

    updateVideoUI()

  }


  //点击会议开始按钮：根据会议的当前状态判断点击后执行的操作
  def toggleLive() = {
    hostStatus match {
      case HostStatus.NOT_CONNECT =>
        //开始会议
        rmManager ! HostLiveReq
        hostStatus = HostStatus.LOADING

//      case HostStatus.LOADING =>

      case HostStatus.CONNECT =>
        //结束会议
        rmManager ! StopLive
        hostStatus = HostStatus.NOT_CONNECT

      case _=>

    }
    updateOffUI()
  }

  def updateOffUI()={
    hostStatus match {
      case HostStatus.NOT_CONNECT =>

        Boot.addToPlatform{
          off_label.setText("开始会议")
          off.setIconColor(Color.GREEN)
          hostStatus = HostStatus.NOT_CONNECT
        }


      case HostStatus.LOADING =>

        Boot.addToPlatform{
          off_label.setText("连接中……")
          off.setIconColor(Color.GRAY)
          hostStatus = HostStatus.LOADING
        }

      case HostStatus.CONNECT =>
        //结束会议
        Boot.addToPlatform{
          //修改界面
          off_label.setText("结束会议")
          off.setIconColor(Color.RED)
          hostStatus = HostStatus.CONNECT
        }

    }
  }

  //开始会议后，界面可以做的一些修改，结束会议，界面需要做的一些修改
  def resetBack() = {

  }

  //禁音某人，只有支持人才可以进行该操作
  def muteOne()={


  }


  //转让主持人身份给某个人
  def transferLeader() = {

  }

}
