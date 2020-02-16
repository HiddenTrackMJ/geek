package org.seekloud.geek.client.controller


import akka.actor.typed.ActorRef
import com.jfoenix.controls.JFXSnackbar.SnackbarEvent
import com.jfoenix.controls.{JFXSnackbar, JFXSnackbarLayout, JFXSpinner, JFXTextField}
import javafx.fxml.FXML
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.image.ImageView
import javafx.scene.layout.{AnchorPane, VBox}
import javafx.util.Duration
import org.seekloud.geek.client.Boot
import org.seekloud.geek.client.component.WarningDialog
import org.seekloud.geek.client.core.RmManager
import org.seekloud.geek.client.utils.RMClient
import org.seekloud.geek.client.Boot.executor
import org.seekloud.geek.client.common.StageContext
import org.seekloud.geek.client.scene.HomeScene
import org.slf4j.LoggerFactory
import org.seekloud.geek.client.component.SnackBar

import scala.concurrent.Future
/**
  * User: hewro
  * Date: 2020/2/16
  * Time: 13:37
  * Description: 登录页面的控制器
  */
class GeekLoginController(
  rmManager: ActorRef[RmManager.RmCommand],
  context: StageContext,
) {

  @FXML private var testLabel: Label = _
  @FXML private var username:JFXTextField = _
  @FXML private var password:JFXTextField = _
  @FXML private var rootPane:AnchorPane = _

  private val log = LoggerFactory.getLogger(this.getClass)

  val waitingGif = new JFXSpinner()
  waitingGif.setPrefWidth(50)
  waitingGif.setPrefHeight(50)

  def initialize(): Unit = {
  }


  def loginBySelf() = {
    if (checkInfo){//信息不完整
      //弹框
      SnackBar.show(rootPane,"你看看是不是忘了填写完整信息了")

    }else{//
      showLoading()
      val r = RMClient.signIn(username.getText, password.getText) //用户名登录
      r.map {
        case Right(rsp) =>
          if (rsp.errCode == 0) {//登录成功
            rmManager ! RmManager.SignInSuccess(rsp.userInfo, rsp.roomInfo)
            RmManager.userInfo = rsp.userInfo

            Boot.addToPlatform {
              removeLoading()
              //显示登录后的用户界面
              showScene()
            }
          } else {//用户名或者密码错误
            log.error(s"sign in error: ${rsp.msg}")
            Boot.addToPlatform {
              removeLoading()
              SnackBar.show(rootPane,s"${rsp.msg}")
            }
          }
        case Left(e) =>
          log.error(s"sign in server error: $e")
          Boot.addToPlatform {
            removeLoading()
            WarningDialog.initWarningDialog(s"服务器错误: $e")
          }
      }
    }

  }

  def showScene(): Unit = {
    Boot.addToPlatform(
      //todo:跳转到用户界面
      context.switchScene(context,"")
    )
  }


  /**
    * 检查登录的信息是否完整
    */
  def checkInfo = {
    username.getText.trim == "" || password.getText().trim == ""
  }


  var hasWaitingGif = false

  def showLoading() = {
    Boot.addToPlatform {
      if (!hasWaitingGif) {
        waitingGif.setLayoutX(rootPane.getWidth / 2 - 25)
        waitingGif.setLayoutY(rootPane.getHeight / 2 - 25)
        rootPane.getChildren.addAll(waitingGif)
        hasWaitingGif = true
      }
    }
  }

  def removeLoading(): Unit = {
    Boot.addToPlatform {
      if (hasWaitingGif) {
        rootPane.getChildren.remove(waitingGif)
        hasWaitingGif = false
      }
    }
  }


}
