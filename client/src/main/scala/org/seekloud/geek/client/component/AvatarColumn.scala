package org.seekloud.geek.client.component

import akka.actor.typed.ActorRef
import javafx.geometry.Insets
import javafx.scene.control.Label
import javafx.scene.layout.{ColumnConstraints, GridPane, Pane, VBox}
import org.seekloud.geek.client.common.Constants.HostOperateIconType
import org.seekloud.geek.client.core.RmManager
import org.seekloud.geek.shared.ptcl.CommonProtocol.UserInfo
import org.slf4j.LoggerFactory

/**
  * User: hewro
  * Date: 2020/2/17
  * Time: 21:35
  * Description: 用户信息栏 = 圆形头像+ (用户名+用户角色)（主持人/普通成员） + 摄像头开关+声音开关+角色切换开关
  */
case class AvatarColumn(
  userInfo: UserInfo,
  width:Double,
  rootPane:Pane,
  updateFunc: ()=>Unit,
  toggleMic: ()=>Unit,
  toggleVideo: ()=>Unit,
  updateAllowUI: ()=>Unit,
  rmManager: ActorRef[RmManager.RmCommand]
){

  private val log = LoggerFactory.getLogger(this.getClass)

  def apply(): GridPane = {
    val gridPane = new GridPane()
    //用户头像
    gridPane.add(Avatar(30, userInfo.headImgUrl)(), 0, 0); // column=1 row=0

    (0 to 5).foreach{
      i=>
        val column = if (i == 1) {
          new ColumnConstraints(width/8 * 3)
        }else if (i==0){
          new ColumnConstraints(width/7 + 5)
        }else{
          new ColumnConstraints(width/7 - 10)
        }
        gridPane.getColumnConstraints.add(column)
    }
    //中间的用户信息
    val vBox = new VBox(5)

    val userName = if (RmManager.userInfo.get.userId == userInfo.userId) {
      new Label(s"${userInfo.userName}(我)")
    }else {
      new Label(userInfo.userName)
    }
    userName.getStyleClass.add("username")
    val userLevel = new Label(if (userInfo.isHost.get)  "主持人" else "参会者")
    userLevel.getStyleClass.add("userLevel")
    vBox.getChildren.addAll(userName,userLevel)
    vBox.setPadding(new Insets(3,0,0,10))
    gridPane.add(vBox, 1, 0)


    //用户操作按钮，4个按钮，声音、摄像头、发言模式、房主切换
    //根据声音开启状态显示不同图标
    val icon = HostOperateIcon("fas-microphone:16:white","fas-microphone-slash:16:white","取消静音","静音",
      userInfo.isMic.get,userInfo,rootPane,
      ()=>Unit,()=>Unit,HostOperateIconType.MIC,rmManager = rmManager)()


    gridPane.add(icon, 2, 0)


    //控制某个用户的视频消息
    val videoIcon = HostOperateIcon("fas-video:16:white","fas-eye-slash:16:white","关闭视频","开启视频",
      userInfo.isVideo.get,userInfo,rootPane,
      ()=>Unit,()=>Unit,HostOperateIconType.VIDEO,rmManager = rmManager)()


    gridPane.add(videoIcon, 3, 0)

    //控制某个用户的发言情况
    val speakIcon = HostOperateIcon("fas-hand-paper:16:green","fas-hand-paper:16:white","指定发言","取消指定发言",
      userInfo.isAllow.get,userInfo,rootPane,
      ()=>updateAllowUI(),()=>Unit,HostOperateIconType.ALLOW,false,rmManager = rmManager)()

    gridPane.add(speakIcon, 4, 0)


    //根据用户身份显示不同的图标，普通用户 user-o
    val user = HostOperateIcon("fas-user-circle:16:#fab726","fas-user:16:white","指定为主持人","指定为主持人",
      userInfo.isHost.get,userInfo,rootPane,
      ()=>Unit,()=>Unit,HostOperateIconType.HOST,rmManager = rmManager)()


    gridPane.add(user, 5, 0)

    gridPane


  }

}
