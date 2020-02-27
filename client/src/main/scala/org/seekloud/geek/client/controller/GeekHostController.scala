package org.seekloud.geek.client.controller

import akka.actor.typed.ActorRef
import com.jfoenix.controls.{JFXListView, JFXRippler, JFXTextArea}
import javafx.animation.AnimationTimer
import javafx.fxml.FXML
import javafx.scene.canvas.{Canvas, GraphicsContext}
import javafx.scene.control.Label
import javafx.scene.layout._
import javafx.scene.paint.Color
import org.kordamp.ikonli.javafx.FontIcon
import org.seekloud.geek.client.Boot
import org.seekloud.geek.client.common.Constants.{AllowStatus, CommentType, DeviceStatus, HostStatus}
import org.seekloud.geek.client.common.{Constants, StageContext}
import org.seekloud.geek.client.component._
import org.seekloud.geek.client.core.RmManager
import org.seekloud.geek.client.core.RmManager._
import org.seekloud.geek.shared.ptcl.CommonProtocol.{CommentInfo, UserInfo}
import org.seekloud.geek.shared.ptcl.WsProtocol
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

  //发言模式相关
  @FXML private var mode_label: Label = _ //当前发言状态
  @FXML private var allow_label: Label = _ //用户自己的发言状态
  @FXML private var allow_button: JFXRippler = _ //申请发言按钮
  @FXML private var allow_icon: FontIcon = _ //用户的申请发言图标


  val commentList = List(
    CommentInfo(1,"何为","","大家新年好",1232132L)
  )

  var commentJList = new JFXListView[GridPane]
  var userJList = new JFXListView[GridPane]

  var hostStatus: Int = HostStatus.NOT_CONNECT
  var allowStatus: Int =AllowStatus.NOT_ALLOW
  var micStatus: Int = DeviceStatus.NOT_READY //音频状态，false未开启
  var videoStatus: Int = DeviceStatus.NOT_READY //视频状态，false未开启摄像头
  var recordStatus: Int = DeviceStatus.OFF //录制状态，一开始未录制

  var loading:Loading = _
  var gc: GraphicsContext = _

  //定时器,定时修改直播时间和录制时长
  private val animationTimer = new AnimationTimer() {
    override def handle(now: Long): Unit = {

      writeLiveTime()
      writeRecordTime()

    }
  }


  //申请发言按钮点击
  def allowClick()= {
    //非主持人才可以申请发言
    if (!RmManager.getCurrentUserInfo().isHost.get){
      allowStatus match {

        case AllowStatus.NOT_ALLOW =>
          //申请发言
          //todo ws消息发送请求
          allowStatus = AllowStatus.ASKING


        case AllowStatus.ASKING =>
          //

        case AllowStatus.ALLOW =>
          //停止发言
          //todo ws消息发送请求
          allowStatus = AllowStatus.NOT_ALLOW
      }
    }else{//提示消息：房主可以直接在成员列表中点击手掌图标指定某人发言（包括自己）
      SnackBar.show(centerPane,"房主可以直接在成员列表中点击「手掌」图标指定某人发言（包括自己）")
    }
    updateAllowUI()
  }

  def updateAllowUI() = {
    if (RmManager.getCurrentUserInfo().isHost.get){
      //房主将这个按钮透明度降低
      allow_button.setOpacity(0.3)
    }else{
      allow_button.setOpacity(1)
    }
    allowStatus match {
      case AllowStatus.NOT_ALLOW =>
        allow_label.setText("申请发言")
        allow_icon.setIconColor(Color.WHITE)
      case AllowStatus.ASKING =>
        allow_label.setText("等待房主同意")
        allow_icon.setIconColor(Color.GRAY)
      case AllowStatus.ALLOW =>
        allow_label.setText("停止发言")
        allow_icon.setIconColor(Color.GREEN)
    }
  }

  //更新当前的模式状态的UI
  def updateModeUI() = {
    //根据当前所有用户的发言状态，如果没有在申请发言，则为自由发言状态，反之为申请发言状态
//    println(RmManager.roomInfo.get.userList)
    if (RmManager.roomInfo.get.userList.exists(_.isAllow.get == true)){
      //当前是申请发言状态
      mode_label.setText("申请发言")
    }else{
      //当前是自由发言状态
      mode_label.setText("自由发言")
    }

    if (RmManager.getCurrentUserInfo().isAllow.get){
      allowStatus = AllowStatus.ALLOW
    }else{
      allowStatus = AllowStatus.NOT_ALLOW
    }
    updateAllowUI()
  }


  //改变底部工具栏的图标和文字,
  //CaptureStartSuccess 摄像头启动成功的时候会执行这个
  def changeToggleAction(): Unit = {

    micStatus = DeviceStatus.ON
    videoStatus = DeviceStatus.ON

    updateVideoUI()
    updateMicUI()
    //启动定时器
    animationTimer.start()

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

  def updateRecordUI() = {
    Boot.addToPlatform {
      recordStatus match {
        case DeviceStatus.NOT_READY =>
          record.setIconLiteral("fas-stop-circle")
          record.setIconColor(Color.GRAY)
          record_label.setText("权限不足")


        case DeviceStatus.ON =>
          log.info("摄像头准备好了")
          record.setIconLiteral("fas-stop-circle")
          record.setIconColor(Color.RED)
          record_label.setText("录制中……")


        case DeviceStatus.OFF =>
          record.setIconLiteral("fas-stop-circle")
          record.setIconColor(Color.WHITE)
          record_label.setText("录制")
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


    //后续从服务器端的ws链接中获取和更新
    RmManager.roomInfo.get.userList = List(
      UserInfo(2L, "hewro", "",isHost=Some(true)),
      UserInfo(3L, "秋林会", ""),
      UserInfo(4L, "薛甘愿", ""),
    )

    initUserList()
    initCommentList()
    addServerMsg(CommentInfo(-1L,"","","欢迎加入",1L))
    initToolbar()
    updateAllowUI()

  }


  def initToolbar() = {
    val toolbar = TopBar(s"会议名称：${RmManager.roomInfo.get.roomName} 会议号：${RmManager.roomInfo.get.roomId}", Color.BLACK,  Color.WHITE,rootPane.getPrefWidth, 10, "host", context, rmManager)()
    rootPane.getChildren.add(toolbar)
  }

  //初始化创建
  def initUserList() = {
    val paneList = createUserListPane()
    userJList.getItems.addAll(paneList:_*)
    userJList.setPrefWidth(userListPane.getPrefWidth)
    userJList.setPrefHeight(userListPane.getPrefHeight)
    userJList.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, null, null)))
    userListPane.getChildren.add(userJList)
  }


  def createUserListPane():List[GridPane] = {
    val userList = RmManager.roomInfo.get.userList

    userList.map{
      t=>
        //创建一个AnchorPane
        val pane = AvatarColumn(t,userListPane.getPrefWidth - 20,centerPane,
          ()=>{updateUserList()},()=>toggleMic(),()=>toggleVideo(),()=>updateModeUI(),rmManager
        )()
        pane.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, null, null)))
        pane
    }
  }

  //更新整个list
  def updateUserList():Unit = {
    val paneList = createUserListPane()
    Boot.addToPlatform{
      //修改整个list
      userJList.getItems.removeAll(userJList.getItems)
      userJList.getItems.addAll(paneList:_*)
    }

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




  //todo 发表评论,ws每收到一条消息就给我发一条消息
  def commentSubmit() = {
    //获得当前的评论消息，添加一个新的comment结构加入到jList中
    val content = commentInput.getText
    if (content.trim.replaceAll(" ","").replaceAll("\n","") ==""){
      //提示内容为空
    }else{
      val t = CommentInfo(RmManager.userInfo.get.userId,RmManager.userInfo.get.userName,RmManager.userInfo.get.headImgUrl,content,System.currentTimeMillis())
      addComment(t)
      rmManager ! RmManager.Comment(WsProtocol.Comment(RmManager.userInfo.get.userId, RmManager.roomInfo.get.roomId, content))
      commentInput.setText("")//清空输入框
    }
  }

  //添加系统消息
  def addServerMsg(t:CommentInfo) = {
    commentJList.getItems.add(CommentColumn(commentPane.getPrefWidth - 30,t,CommentType.SERVER)())
  }

  def addComment(t:CommentInfo) = {
    Boot.addToPlatform{
        commentJList.getItems.add(CommentColumn(commentPane.getPrefWidth - 30,t)())
    }
  }



  //当userList数据更新，需要更新的界面
  def updateWhenUserList() = {

    updateUserList()
    updateModeUI()
    updateAllowUI()
  }

  //接收处理与【后端发过来】的消息
  def wsMessageHandle(data: WsMsgRm): Unit = {
    data match {

      case _: HeatBeat =>
      //        log.debug(s"heartbeat: ${msg.ts}")
      //        rmManager ! HeartBeat

      case msg: RcvComment =>
        addComment(CommentInfo(msg.userId, msg.userName, RmManager.userInfo.get.headImgUrl, msg.comment, System.currentTimeMillis()))

      case msg: GetRoomInfoRsp =>
        println("GetRoomInfoRsp"+msg)
        RmManager.roomInfo.get.userList = msg.info.userList
        //更新界面
        updateWhenUserList()

      case msg: ChangePossessionRsp =>
        //改变成员列表中的

        val origHost = RmManager.roomInfo.get.userList.find(_.isHost.get == true).get
        origHost.isHost = Some(false)
        val user = RmManager.roomInfo.get.userList.find(_.userId == msg.userId)
        if (user nonEmpty){
          user.get.isHost = Some(true)
        }
        //更新界面
        updateWhenUserList()

      case msg: StartLiveRsp =>
        //房主收到的消息
        log.debug(s"get StartLiveRsp: $msg")
        if (msg.errCode == 0) {
          //开始直播
          rmManager ! StartLiveSuccess(msg.rtmp.serverUrl+msg.rtmp.stream,msg.rtmp.serverUrl+msg.selfCode,  msg.userLiveCodeMap.map(i => (msg.rtmp.serverUrl + i._1, i._2)))
        } else {
          Boot.addToPlatform {
            WarningDialog.initWarningDialog(s"${msg.msg}")
          }
        }

      case msg: StartLive4ClientRsp =>
        log.info(s"收到后端的开始会议消息")
        if (msg.errCode == 0) {
          //开始直播
          rmManager ! StartLive4ClientSuccess(msg.userLiveCodeMap.map(i => (msg.rtmp.get.serverUrl + i._1, i._2)))
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

      case msg:ShieldRsp =>
        log.info("收到ShieldRsp")
        val user = RmManager.roomInfo.get.userList.find(_.userId == msg.userId)
        if (user nonEmpty){
          //更新设备状态
          user.get.isVideo = Some(msg.isImage)
          user.get.isMic = Some(msg.isAudio)
        }
        //更新界面
        updateWhenUserList()
        // 如果被封禁的userId是自己，需要修改底部功能条的样式并且调整摄像头、音频设置的关闭开启
        if (msg.userId == RmManager.userInfo.get.userId){
          if ((micStatus==DeviceStatus.ON && !msg.isAudio) || (micStatus==DeviceStatus.OFF && msg.isAudio)){
            toggleMic()
          }

          if ((videoStatus==DeviceStatus.ON && !msg.isImage) || (videoStatus==DeviceStatus.OFF && msg.isImage)){
            toggleVideo()
          }
        }





      case x =>
        log.warn(s"host recv unknown msg from rm: $x")
    }

  }


  //点击录制按钮
  def toggleRecord() = {
    recordStatus match {
      case DeviceStatus.OFF =>
        //todo 开始录制
        recordStatus = DeviceStatus.ON
        //开始时间
        startRecTime = System.currentTimeMillis()

      case DeviceStatus.ON =>
        //todo 取消录制
        recordStatus = DeviceStatus.OFF

    }

    updateRecordUI()

  }
  //点击音频按钮：根据设备的状态
  def toggleMic() = {
    micStatus match {
      case DeviceStatus.ON =>
        //todo 关闭音频
        micStatus = DeviceStatus.OFF
        RmManager.getCurrentUserInfo().isMic = Some(false)


      case DeviceStatus.OFF =>
        //todo 开启音频
        micStatus = DeviceStatus.ON
        RmManager.getCurrentUserInfo().isMic = Some(true)


      case _=>

    }
    updateMicUI()
    updateUserList()

  }

  //点击视频按钮：根据设备的状态
  def toggleVideo() = {
    videoStatus match {
      case DeviceStatus.ON =>
        //todo 关闭摄像头
        videoStatus = DeviceStatus.OFF
        RmManager.getCurrentUserInfo().isVideo = Some(false)


      case DeviceStatus.OFF =>
        // todo开启摄像头
        videoStatus = DeviceStatus.ON
        RmManager.getCurrentUserInfo().isVideo = Some(true)

      case _=>

    }

    updateVideoUI()
    updateUserList()

  }

  protected var startLiveTime: Long = 0L  //开始直播的时间
  protected var startRecTime: Long = 0L   //开始录像的时间


  //点击会议开始按钮：根据会议的当前状态判断点击后执行的操作
  def toggleLive() = {
    hostStatus match {
      case HostStatus.NOT_CONNECT =>
        //开始会议
        rmManager ! HostLiveReq
        hostStatus = HostStatus.LOADING

        startLiveTime = System.currentTimeMillis()

//      case HostStatus.LOADING =>

      case HostStatus.CONNECT =>
        //结束会议
        rmManager ! StopLive
        hostStatus = HostStatus.NOT_CONNECT

      case _=>

    }
    updateOffUI()
  }

  //邀请别人
  def inviteOne() = {
    SnackBar.show(centerPane,s"会议号：${RmManager.roomInfo.get.roomId},发给你的小伙伴吧！")
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
          off_label.setText("结束")
          off.setIconColor(Color.RED)
          hostStatus = HostStatus.CONNECT
        }

    }
  }


  def writeRecordTime() = {
    recordStatus match {
      case DeviceStatus.ON =>
        val recTime = System.currentTimeMillis() - startRecTime
        val hours = recTime / 3600000
        val minutes = (recTime % 3600000) / 60000
        val seconds = (recTime % 60000) / 1000
        record_label.setText(s"录制中，时长：${hours.toInt}:${minutes.toInt}:${seconds.toInt}")
      case _=>
      //          updateRecordUI()
    }
  }

  def writeLiveTime() = {
    hostStatus match{
      case HostStatus.CONNECT =>
        val liveTime = System.currentTimeMillis() - startLiveTime
        val hours = liveTime / 3600000
        val minutes = (liveTime % 3600000) / 60000
        val seconds = (liveTime % 60000) / 1000
        off_label.setText(s"结束，时长：${hours.toInt}:${minutes.toInt}:${seconds.toInt}")
      case _ =>
      //          updateVideoUI()
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
