package org.seekloud.geek.client.controller

import akka.actor.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.adapter._
import com.jfoenix.controls.{JFXListView, JFXRippler, JFXTextArea}
import javafx.animation.AnimationTimer
import javafx.fxml.FXML
import javafx.scene.canvas.{Canvas, GraphicsContext}
import javafx.scene.control.Label
import javafx.scene.image.Image
import javafx.scene.layout._
import javafx.scene.paint.Color
import org.kordamp.ikonli.javafx.FontIcon
import org.seekloud.geek.client.Boot
import org.seekloud.geek.client.common.AppSettings.config
import org.seekloud.geek.client.common.Constants.{AllowStatus, CommentType, DeviceStatus, HostStatus}
import org.seekloud.geek.client.common.{Constants, StageContext}
import org.seekloud.geek.client.component._
import org.seekloud.geek.client.core.HostUIManager.{HostUICommand, UpdateModeUI, UpdateUserListPaneUI}
import org.seekloud.geek.client.core.RmManager._
import org.seekloud.geek.client.core.{HostUIManager, RmManager}
import org.seekloud.geek.player.util.GCUtil
import org.seekloud.geek.shared.ptcl.CommonProtocol.{CommentInfo, ModeStatus}
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

  implicit val system: ActorSystem = ActorSystem("geek", config)


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
  @FXML var mode_label: Label = _ //当前发言状态
  @FXML private var allow_label: Label = _ //用户自己的发言状态
  @FXML private var allow_button: JFXRippler = _ //申请发言按钮
  @FXML private var allow_icon: FontIcon = _ //用户的申请发言图标


  private var hostUIManager:ActorRef[HostUICommand] = _
  val commentList = List(

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
//      writeRecordTime()

    }
  }

  //申请发言按钮点击
  def allowClick()= {
    //非主持人才可以申请发言
    if (!RmManager.getCurrentUserInfo().isHost.get){
      if (RmManager.isStart){
        allowStatus match {

          case AllowStatus.NOT_ALLOW =>
            //申请发言
            rmManager ! Appoint4Client(RmManager.userInfo.get.userId,RmManager.userInfo.get.userName,true)
            allowStatus = AllowStatus.ASKING


          case AllowStatus.ASKING =>
          //没有操作等待后端发消息

          case AllowStatus.ALLOW =>
            //停止发言
            rmManager ! Appoint4Client(RmManager.userInfo.get.userId,RmManager.userInfo.get.userName,false)
            allowStatus = AllowStatus.NOT_ALLOW
        }
      }else{
        SnackBar.show(centerPane,"您没有开启会议，无法申请发言")
      }

    }else{//提示消息：房主可以直接在成员列表中点击手掌图标指定某人发言（包括自己）
      SnackBar.show(centerPane,"房主可以直接在成员列表中点击「手掌」图标指定某人发言（包括自己）")
    }
    updateAllowUI()
  }

  def updateAllowUI() = {
    Boot.addToPlatform{

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
  }

  //更新当前的模式状态的UI
  def updateModeUI() = {


    val originModeStatus = RmManager.roomInfo.get.modeStatus


    if (RmManager.roomInfo.get.userList.exists(_.isAllow.get == true)){
      RmManager.roomInfo.get.modeStatus = ModeStatus.ASK
    }else{
      RmManager.roomInfo.get.modeStatus = ModeStatus.FREE
    }

    if (originModeStatus != RmManager.roomInfo.get.modeStatus){
      RmManager.roomInfo.get.modeStatus match {
        case ModeStatus.ASK =>
          //给非发言人的其他用户静音
          RmManager.roomInfo.get.userList.filter(!_.isAllow.get).map(t=>(t.userId,t.isVideo)).foreach{
            m=>
            rmManager ! Shield(ShieldReq(isForced = true,RmManager.roomInfo.get.roomId,m._1,isImage = m._2.get,isAudio = false))
          }

          SnackBar.show(centerPane,"当前会议切换到「申请发言模式」")
        case ModeStatus.FREE =>
          //给所有人开启开启语音
          RmManager.roomInfo.get.userList.map(t=>(t.userId,t.isVideo)).foreach{
            m=>
              rmManager ! Shield(ShieldReq(isForced = true,RmManager.roomInfo.get.roomId,m._1,isImage = m._2.get,isAudio = true))
          }
          SnackBar.show(centerPane,"当前会议切换到「自由发言模式」")
      }
    }
    RmManager.calUserListPosition()//先计算发言的状态，再重新计算用户在界面中的顺序

    hostUIManager ! UpdateModeUI()

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
//          log.info("摄像头准备好了")
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
    hostUIManager = system.spawn(HostUIManager.create(context,GeekHostController.this),"hostUIManager")
    if (initSuccess nonEmpty){
      //给liveManager ! LiveManager.DevicesOn(gc, callBackFunc = callBack)，
      // callBackFunc即changeToggleAction
      initSuccess.foreach(func => func(gc,Some(()=>changeToggleAction())))
    }

    //todo 根据用户类型锁定一些内容/按钮的事件修改


    //后续从服务器端的ws链接中获取和更新
    RmManager.roomInfo.get.userList = List()

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
    //todo 优化性能
    val paneList = createUserListPane()
    hostUIManager ! UpdateUserListPaneUI(paneList)

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




  //发表评论,ws每收到一条消息就给我发一条消息
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
    Boot.addToPlatform{
      commentJList.getItems.add(CommentColumn(commentPane.getPrefWidth - 30,t,CommentType.SERVER)())
    }
  }

  def addComment(t:CommentInfo) = {
    Boot.addToPlatform{
        commentJList.getItems.add(CommentColumn(commentPane.getPrefWidth - 30,t)())
    }
  }



  //当userList数据更新，需要更新的界面
  def updateWhenUserList() = {
    log.info("updateWhenUserList 更新")
    updateUserList()
    updateModeUI()
  }

  //接收处理与【后端发过来】的消息
  def wsMessageHandle(data: WsMsgRm): Unit = {
    data match {

      case msg: HeatBeat =>
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
        SnackBar.show(centerPane,s"${msg.userName}成为新的主持人")

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
        log.info(s"get StartLive4ClientRsp: $msg")
        if (msg.errCode == 0) {
          //开始直播
          rmManager ! StartLive4ClientSuccess(msg.rtmp.get.serverUrl+msg.selfCode,msg.userLiveCodeMap.map(i => (msg.rtmp.get.serverUrl + i._1, i._2)))
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
          log.info(s"房主停止推流成功")
          rmManager ! StopLiveSuccess
        }else{
          rmManager ! StopLiveFailed
        }



      case msg: StopLive4ClientRsp =>
        if (msg.errCode == 0){
          log.info("普通用户停止推流成功")
          rmManager ! StopLive4ClientSuccess(msg.userId)
        }else{
          rmManager ! StopLiveFailed
        }

      case msg:Appoint4ClientReq=>
        log.info(s"Appoint4ClientReq:$msg")
        log.info(s"host: ${RmManager.getCurrentUserInfo().isHost.get},req Status: ${msg.status}")
        if (RmManager.getCurrentUserInfo().isHost.get && msg.status){//自己是主持人而且是请求发言
          ConfirmDialog(context.getStage,s"${msg.userName} 用户请求发言","您可以选择同意或者拒绝","同意","拒绝",Some(
            ()=>{
              //取消掉当前的发言人
              RmManager.roomInfo.get.userList.filter(_.isAllow.get==true).map(_.userId).foreach{
                rmManager ! Appoint4Host(_,status = false)
              }
              //给后端发送同意
              rmManager ! Appoint4HostReply(msg.userId,status = true)
            }
          ),Some(
            ()=>{
              //给后端发送拒绝
              rmManager ! Appoint4HostReply(msg.userId,status = false)
            }
          )).show()
        }

      case msg:AppointRsp =>
        //修改当前会议的发言状态和用户的发言状态
        log.info(s"AppointRsp:$msg")
        val user = RmManager.getUserInfo(msg.userId)
        if (user nonEmpty){
          user.get.isAllow = Some(msg.status)
          if (msg.status){

            SnackBar.show(centerPane,s"${msg.userName}成为发言人")
          }else{
            SnackBar.show(centerPane,s"${msg.userName}取消成为发言人")
          }
          //发言人切换后，界面需要刷新一下
          updateWhenUserList()
        }else{
          //不需要修改，可能这个用户已经退出会议厅了
        }

      case msg:ShieldRsp =>
        log.info(s"收到ShieldRsp:$msg")
        val user = RmManager.roomInfo.get.userList.find(_.userId == msg.userId)
        log.info(s"当前video状态:${user.get.isVideo.get},返回的video状态:${msg.isImage}")
        if (!user.get.isVideo.get && msg.isImage){//开启用户的视频
          //自己或者别的用户交给rm统一处理
          user.get.isVideo = Some(true)
          rmManager ! ChangeVideoOption(msg.userId,true)
        }
        if (user.get.isVideo.get && !msg.isImage){//关闭用户的视频
          user.get.isVideo = Some(false)
          rmManager ! ChangeVideoOption(msg.userId,false)
        }

        if (!user.get.isMic.get && msg.isAudio){//开启用户的声音
          user.get.isMic = Some(true)
          rmManager ! ChangeMicOption(msg.userId,true)
        }
        if (user.get.isMic.get && !msg.isAudio){//关闭用户的声音
          user.get.isMic = Some(false)
          rmManager ! ChangeMicOption(msg.userId,false)
        }


        //更新界面
        updateWhenUserList()


      case _:HostCloseRoom=>
      case HostCloseRoom =>
        log.info(s"receive：HostCloseRoom")
        if (!RmManager.getCurrentUserInfo().isHost.get){//自己不是主持人，主持人退出了会出现一个弹窗
          ConfirmDialog(context.getStage,s"主持人退出房间","您即将退出房间","确定","确定",Some(
            ()=>{
              rmManager ! BackToHome
            }
          ),Some(
            ()=>{
              rmManager ! BackToHome
            }
          )).show()
        }


      case x =>
        log.warn(s"host recv unknown msg from rm: $x")
    }

  }


  //点击录制按钮
  def toggleRecord() = {
    //显示关于弹窗
    ConfirmDialog(
      context.getStage,
      "关于geek",
      "qhx小组：\n" +
        "邱林辉\n" +
        "何炜\n" +
        "薛淦元\n\n" +
        "make with love",
      isCanClose = true
    ).show()

//    recordStatus match {
//      case DeviceStatus.OFF =>
//        //todo 开始录制
//        recordStatus = DeviceStatus.ON
//        //开始时间
//        startRecTime = System.currentTimeMillis()
//
//      case DeviceStatus.ON =>
//        //todo 取消录制
//        recordStatus = DeviceStatus.OFF
//
//    }

//    updateRecordUI()

  }
  //点击音频按钮：根据设备的状态
  def toggleMic() = {
    micStatus match {
      case DeviceStatus.ON =>
        //关闭音频
        log.info("关闭音频")
        micStatus = DeviceStatus.OFF
        RmManager.getCurrentUserInfo().isMic = Some(false)
        rmManager ! Shield(ShieldReq(isForced = false,RmManager.roomInfo.get.roomId,RmManager.userInfo.get.userId,
          isImage = if (videoStatus == DeviceStatus.ON) true else false,isAudio = false))

      case DeviceStatus.OFF =>
        //开启音频
        log.info("开启音频")
        micStatus = DeviceStatus.ON
        RmManager.getCurrentUserInfo().isMic = Some(true)
        rmManager ! Shield(ShieldReq(isForced = false,RmManager.roomInfo.get.roomId,RmManager.userInfo.get.userId,
          isImage = if (videoStatus == DeviceStatus.ON) true else false,isAudio = true))


      case _=>

    }
    updateMicUI()
//    updateUserList()

  }

  //点击视频按钮：根据设备的状态
  def toggleVideo() = {
    videoStatus match {
      case DeviceStatus.ON =>
        // 关闭摄像头
        log.info("关闭摄像头")
//        videoStatus = DeviceStatus.OFF
//        RmManager.getCurrentUserInfo().isVideo = Some(false)
        rmManager ! Shield(ShieldReq(isForced = false,RmManager.roomInfo.get.roomId,RmManager.userInfo.get.userId,
          isImage = false ,isAudio = if (micStatus == DeviceStatus.ON) true else false))

      case DeviceStatus.OFF =>
        // 开启摄像头
        log.info("开启摄像头")
//        videoStatus = DeviceStatus.ON
//        RmManager.getCurrentUserInfo().isVideo = Some(true)
        rmManager ! Shield(ShieldReq(isForced = false,RmManager.roomInfo.get.roomId,RmManager.userInfo.get.userId,
          isImage = true ,isAudio = if (micStatus == DeviceStatus.ON) true else false))

      case _=>

    }

    updateVideoUI()
//    updateUserList()

  }

  protected var startLiveTime: Long = 0L  //开始直播的时间
  protected var startRecTime: Long = 0L   //开始录像的时间


  //点击会议开始按钮：根据会议的当前状态判断点击后执行的操作
  def toggleLive() = {
    hostStatus match {
      case HostStatus.NOT_CONNECT =>
        //开始会议
        RmManager.isStart = true
        rmManager ! HostLiveReq
        hostStatus = HostStatus.LOADING

        startLiveTime = System.currentTimeMillis()

//      case HostStatus.LOADING =>

      case HostStatus.CONNECT =>
        //结束会议
        RmManager.isStart = false
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
  //开始会议后/停止某个人拉流，界面可以做的一些修改
  def resetBack() = {
    log.info("resetBack")
    //大背景改成黑色的
    Boot.addToPlatform{
      gc.drawImage(new Image("scene/img/bg.jpg"),0,0,gc.getCanvas.getWidth,gc.getCanvas.getHeight)
      //画5个框等待加入的框（只画这个位置没有人的位置）
      val needPositions = List.range(0,5) diff RmManager.roomInfo.get.userList.map(_.position)
      needPositions.foreach(GCUtil.draw(gc,new Image("img/join.png"),_,isNeedClear = true))

    }
  }


  //禁音某人，只有支持人才可以进行该操作
  def muteOne()={


  }


  //转让主持人身份给某个人
  def transferLeader() = {

  }

}
