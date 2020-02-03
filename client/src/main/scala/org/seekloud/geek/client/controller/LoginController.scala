package org.seekloud.geek.client.controller

import java.security.MessageDigest

import javafx.geometry.{Insets, Pos}
import javafx.scene.control.ButtonBar.ButtonData
import javafx.scene.control._
import javafx.scene.layout.{GridPane, HBox, VBox}
import akka.actor.typed.ActorRef
import javafx.scene.Group
import javafx.scene.image.ImageView
import javafx.scene.text.{Font, Text}
import org.seekloud.geek.client.Boot
import org.seekloud.geek.client.common.StageContext
import org.seekloud.geek.client.Boot
import org.seekloud.geek.client.component.WarningDialog
import org.seekloud.geek.client.core.RmManager
import org.slf4j.LoggerFactory

/**
  * User: Arrow
  * Date: 2019/7/16
  * Time: 11:27
  */
class LoginController(
  context: StageContext,
  rmManager: ActorRef[RmManager.RmCommand]
) {
  private[this] val log = LoggerFactory.getLogger(this.getClass)

  //登录弹窗
  def loginDialog(): Option[(String, String, String)] = {
    val dialog = new Dialog[(String, String, String)]()
    dialog.setTitle("登录")

    val welcomeText = new Text("欢迎登录")
    welcomeText.setStyle("-fx-font: 35 KaiTi;-fx-fill: #333f50")
    val upBox = new HBox()
    upBox.setAlignment(Pos.TOP_CENTER)
    upBox.setPadding(new Insets(40, 200, 0, 200))
    upBox.getChildren.add(welcomeText)


    //userNameGrid
    val userNameIcon = new ImageView("img/userName.png")
    userNameIcon.setFitHeight(30)
    userNameIcon.setFitWidth(30)
    val userNameLabel = new Label("用户名:")
    userNameLabel.setFont(Font.font(18))
    val userNameField = new TextField("")

    val passwordIcon = new ImageView("img/passWord.png")
    passwordIcon.setFitHeight(30)
    passwordIcon.setFitWidth(30)
    val passWordLabel = new Label("密码:")
    passWordLabel.setFont(Font.font(18))
    val passWordField = new PasswordField()

    val userNameGrid = new GridPane //格子布局
    userNameGrid.setHgap(20)
    userNameGrid.setVgap(30)
    userNameGrid.add(userNameIcon, 0, 0)
    userNameGrid.add(userNameLabel, 1, 0)
    userNameGrid.add(userNameField, 2, 0)
    userNameGrid.add(passwordIcon, 0, 1)
    userNameGrid.add(passWordLabel, 1, 1)
    userNameGrid.add(passWordField, 2, 1)
    userNameGrid.setStyle("-fx-background-color:#d4dbe3;")
    userNameGrid.setPadding(new Insets(60, 20, 60, 20))



    //bottomBox
    val bottomBox = new VBox()
    bottomBox.getChildren.addAll(userNameGrid) //默认
    bottomBox.setAlignment(Pos.CENTER)
    //    bottomBox.setStyle("-fx-background-color:#d4dbe3;-fx-background-radius: 10")
    bottomBox.setPadding(new Insets(10, 100, 50, 100))


    val box = new VBox()
    box.getChildren.addAll(upBox, bottomBox)
    box.setAlignment(Pos.CENTER)
    box.setSpacing(30)
    box.setStyle("-fx-background-color:#f2f5fb")

    val confirmButton = new ButtonType("确定", ButtonData.OK_DONE)

    val group = new Group()
    group.getChildren.addAll(box)
    dialog.getDialogPane.getButtonTypes.add(confirmButton)
    dialog.getDialogPane.setContent(group)
    dialog.setResultConverter(dialogButton =>
      if (dialogButton == confirmButton) {
        //        log.debug(s"tb1selected:${tb1.isSelected},tb2selected:${tb2.isSelected},userName:${userNameField.getText()},userPwd：${passWordField.getText()},email:${emailField.getText()},emailPwd:${emailPassWordField.getText()}")
        if (userNameField.getText().nonEmpty && passWordField.getText().nonEmpty) {
          (userNameField.getText(), passWordField.getText(), "userName")
        } else {
          Boot.addToPlatform {
            WarningDialog.initWarningDialog("请填写完整信息！")
          }
          null
        }
      } else {
        null
      }
    )
    var loginInfo: Option[(String, String, String)] = None
    val rst = dialog.showAndWait()
    rst.ifPresent { a =>
      if (a._1 != null && a._2 != null && a._3 != null && a._1 != "" && a._2 != "" && a._3 != "")
        loginInfo = Some((a._1, a._2, a._3))
      else
        None
    }
    loginInfo
  }

  //  注册弹窗
  def registerDialog(): Option[(String, String)] = {
    val dialog = new Dialog[(String, String)]()
    dialog.setTitle("注册")

    val welcomeText = new Text("欢迎注册")
    welcomeText.setStyle("-fx-font: 35 KaiTi;-fx-fill: #333f50")
    val upBox = new HBox()
    upBox.setAlignment(Pos.TOP_CENTER)
    upBox.setPadding(new Insets(40, 200, 0, 200))
    upBox.getChildren.add(welcomeText)


    val userNameIcon = new ImageView("img/userName.png")
    userNameIcon.setFitHeight(30)
    userNameIcon.setFitWidth(30)
    val userNameLabel = new Label("用户名:")
    userNameLabel.setFont(Font.font(18))
    val userNameField = new TextField()

    val passWordIcon = new ImageView("img/passWord.png")
    passWordIcon.setFitHeight(30)
    passWordIcon.setFitWidth(30)
    val passWordLabel = new Label("密码:")
    passWordLabel.setFont(Font.font(18))
    val passWordField = new PasswordField()

    val passWordIcon1 = new ImageView("img/passWord.png")
    passWordIcon1.setFitHeight(30)
    passWordIcon1.setFitWidth(30)
    val passWordLabel1 = new Label("确认密码:")
    passWordLabel1.setFont(Font.font(18))
    val passWordField1 = new PasswordField()

    val grid = new GridPane
    grid.setHgap(20)
    grid.setVgap(30)
//    grid.add(emailIcon, 0, 0)
//    grid.add(emailLabel, 1, 0)
//    grid.add(emailField, 2, 0)
    grid.add(userNameIcon, 0, 0)
    grid.add(userNameLabel, 1, 0)
    grid.add(userNameField, 2, 0)
    grid.add(passWordIcon, 0, 1)
    grid.add(passWordLabel, 1, 1)
    grid.add(passWordField, 2, 1)
    grid.add(passWordIcon1, 0, 2)
    grid.add(passWordLabel1, 1, 2)
    grid.add(passWordField1, 2, 2)
    grid.setStyle("-fx-background-color:#d4dbe3;-fx-background-radius: 10")
    grid.setPadding(new Insets(60, 20, 60, 20))

    val bottomBox = new HBox()
    bottomBox.getChildren.add(grid)
    bottomBox.setAlignment(Pos.BOTTOM_CENTER)
    bottomBox.setPadding(new Insets(10, 100, 50, 100))

    val box = new VBox()
    box.getChildren.addAll(upBox, bottomBox)
    box.setAlignment(Pos.CENTER)
    box.setSpacing(30)
    box.setStyle("-fx-background-color:#f2f5fb")

    val confirmButton = new ButtonType("确定", ButtonData.OK_DONE)

    val group = new Group()
    group.getChildren.add(box)
    dialog.getDialogPane.getButtonTypes.add(confirmButton)
    dialog.getDialogPane.setContent(group)
    dialog.setResultConverter(dialogButton =>
      if (userNameField.getText().nonEmpty && passWordField.getText().nonEmpty) {
        if (passWordField.getText() == passWordField1.getText()) {
          if (dialogButton == confirmButton)
             (userNameField.getText(),passWordField.getText())
          else
            null
        } else {
          Boot.addToPlatform(
            WarningDialog.initWarningDialog("注册失败：两次输入密码不一致")
          )
          null
        }
      } else {
        Boot.addToPlatform(
          WarningDialog.initWarningDialog("输入不能为空！")
        )
        null
      }
    )
    var registerInfo: Option[(String, String)] = None
    val rst = dialog.showAndWait()
    rst.ifPresent { a =>
      if (a._1 != null && a._2 != null  && a._1 != "" && a._2 != "" )
        registerInfo = Some((a._1, a._2))
      else
        None
    }
    registerInfo
  }
}
