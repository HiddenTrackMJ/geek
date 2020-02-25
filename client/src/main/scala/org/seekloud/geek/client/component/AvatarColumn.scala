package org.seekloud.geek.client.component

import javafx.geometry.Insets
import javafx.scene.control.Label
import javafx.scene.layout.{ColumnConstraints, GridPane, Pane, VBox}
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
  updateFunc: ()=>Unit
){

  private val log = LoggerFactory.getLogger(this.getClass)

  def apply(): GridPane = {
    val gridPane = new GridPane()
//    gridPane.setAlignment()="LEFT"
//    GridPane.setHalignment()="CENTER"
    //用户头像
    gridPane.add(Avatar(30, userInfo.headImgUrl)(), 0, 0); // column=1 row=0

//    val width = gridPane.getPrefWidth
    (0 to 4).foreach{
      i=>
//        println("1当前序号" + i)
        val column = if (i == 1) {
          new ColumnConstraints(width/7 * 3)
        }else if (i==0){
          new ColumnConstraints(width/7 + 5)
        }else{
          new ColumnConstraints(width/7 - 5)
        }
        gridPane.getColumnConstraints.add(column)
    }
    //中间的用户信息
    val vBox = new VBox(5)
    val userName = new Label(userInfo.userName)
    userName.getStyleClass.add("username")
    val userLevel = new Label(if (userInfo.isHost.get)  "主持人" else "参会者")
    userLevel.getStyleClass.add("userLevel")
    vBox.getChildren.addAll(userName,userLevel)
    vBox.setPadding(new Insets(3,0,0,10))
    gridPane.add(vBox, 1, 0)
//    gridPane.setPrefWidth(width)

    //用户操作按钮
    //根据声音开启状态显示不同图标
    val r = if(userInfo.isMic.get) RippleIcon(List("fas-microphone:16:white"))()else RippleIcon(List("fas-microphone-slash:16:white"))()
    val icon = r._1


    icon.setOnMouseClicked(_ =>{
      //todo 发ws消息
      if (userInfo.isMic.get){
        SnackBar.show(rootPane,"静音" + userInfo.userName)
      }else{
        SnackBar.show(rootPane,"取消静音" + userInfo.userName)
      }

      //修改内存中该用户的静音状态
      userInfo.isMic = Some(!userInfo.isMic.get)

      //修改界面
      updateFunc()

    })


    gridPane.add(icon, 2, 0)


    //控制某个用户的视频消息
    val v = if(userInfo.isVideo.get) RippleIcon(List("fas-video:16:white"))()else RippleIcon(List("fas-eye-slash:16:white"))()
    val videoIcon = v._1


    videoIcon.setOnMouseClicked(_ =>{
      //todo 发ws消息
      if (userInfo.isVideo.get){
        SnackBar.show(rootPane,"关闭视频" + userInfo.userName)
      }else{
        SnackBar.show(rootPane,"显示视频" + userInfo.userName)
      }

      //修改内存中该用户的静音状态
      userInfo.isVideo = Some(!userInfo.isVideo.get)

      //修改界面
      updateFunc()

    })


    gridPane.add(videoIcon, 3, 0)


    //根据用户身份显示不同的图标，普通用户 user-o
    val r2 =
      if(userInfo.isHost.get) RippleIcon(List("fas-user-circle:16:#fab726"))()
      else  RippleIcon(List("fas-user:16:white"))()
    val user = r2._1
    val userSpan = r2._2

    userSpan.setOnMouseClicked(_=>{
      //修改用户的身份信息
      //把当前用户设置为主持人，其他用户设置为非主持人
      val origHost = RmManager.roomInfo.get.userList.find(_.isHost.get == true)
      if (origHost nonEmpty){
        origHost.get.isHost = Some(false)
        userInfo.isHost = Some(true)
        //修改整个Jlist的界面，回调controller里面的方法
        updateFunc()
      }else{
        //
        log.info("当前数据有误，成员列表中没有房主")

      }
    })

    gridPane.add(user, 4, 0)

    gridPane


  }

}
