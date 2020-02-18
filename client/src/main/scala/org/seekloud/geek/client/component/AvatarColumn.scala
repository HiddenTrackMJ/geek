package org.seekloud.geek.client.component

import com.jfoenix.controls.JFXPopup.{PopupHPosition, PopupVPosition}
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.layout.{GridPane, Pane, VBox}
import org.seekloud.geek.client.component.Avatar
import org.seekloud.geek.shared.ptcl.CommonProtocol.UserInfo
import javafx.scene.layout.ColumnConstraints

/**
  * User: hewro
  * Date: 2020/2/17
  * Time: 21:35
  * Description: 用户信息栏 = 圆形头像+ 用户名+用户角色（主持人/普通成员）
  */
case class AvatarColumn(
  userInfo: UserInfo,
  width:Double
){


  def apply(): GridPane = {
    val gridPane = new GridPane()
//    gridPane.setAlignment()="LEFT"
//    GridPane.setHalignment()="CENTER"
    //用户头像
    gridPane.add(Avatar(30, userInfo.headImgUrl)(), 0, 0); // column=1 row=0

//    val width = gridPane.getPrefWidth
    (0 to 2).foreach{
      i=>
        println("当前序号" + i)
        val column = if (i == 1) {
          new ColumnConstraints(width/5 * 3)
        }else {
          new ColumnConstraints(width/5)
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
    val icon = RippleIcon("fas-microphone:16:white")()

    icon.setOnMouseClicked(_ =>{
      //todo 给当前用户静音


    })
    gridPane.add(icon, 2, 0)
    gridPane

  }

}
