package org.seekloud.geek.client.scene

import akka.actor.FSM.Failure
import com.sun.scenario.effect.light
import javafx.geometry.{Insets, Pos}
import javafx.scene.control.{Button, Label, Tooltip}
import javafx.scene.effect._
import javafx.scene.image.{Image, ImageView}
import javafx.scene.input.MouseEvent
import javafx.scene.layout.{BorderPane, HBox, StackPane, VBox}
import javafx.scene.paint.Color
import javafx.scene.text.{Font, Text}
import javafx.scene.{Group, Scene}
import org.seekloud.geek.client.common.{Constants, Pictures}
//import org.seekloud.geek.client.core.RmManager
import org.slf4j.LoggerFactory
import javafx.scene.control.Tooltip
import org.seekloud.geek.client.component.Common._

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * User: Arrow
  * Date: 2019/7/16
  * Time: 12:13
  *
  * 主页
  */

object HomeScene {

  trait HomeSceneListener {

    def liveCheck()

    def gotoRoomPage()

    def gotoLoginDialog(
                         userName: Option[String] = None,
                         pwd: Option[String] = None,
                         isToLive: Boolean = false,
                         isToWatch: Boolean = false)

    def gotoRegisterDialog()

    def logout()

    def editInfo()
  }

}

class HomeScene {

  import HomeScene._

  private[this] val log = LoggerFactory.getLogger(this.getClass)

  private val width = Constants.AppWindow.width * 0.9
  private val height = Constants.AppWindow.height * 0.75


  val waitingGif = new ImageView("img/waiting.gif")
  waitingGif.setFitHeight(50)
  waitingGif.setFitWidth(50)
  waitingGif.setLayoutX(width / 2 - 25)
  waitingGif.setLayoutY(height / 2 - 25)

  var topArea = addTopArea()
  val borderPane = new BorderPane()
  borderPane.setTop(topArea)
  borderPane.setCenter(addMiddleArea())
  borderPane.setBottom(addBottomArea())


  val group = new Group()
  group.getChildren.add(borderPane)

  private val scene = new Scene(group, width, height)
  scene.getStylesheets.add(
    this.getClass.getClassLoader.getResource("css/common.css").toExternalForm
  )

  def getScene: Scene = {
    topArea = addTopArea()
    borderPane.setTop(topArea)
    this.scene
  }

  var listener: HomeSceneListener = _

  def setListener(listener: HomeSceneListener): Unit = {
    this.listener = listener
  }

  //HBox是水平布局，意味着在top区域内是水平布局
  def addTopArea(): HBox = {
    val topHBox = new HBox()
    topHBox.setPadding(new Insets(30, 50, 20, 0))
    topHBox.setSpacing(20)
    topHBox.setAlignment(Pos.CENTER_RIGHT)

    def createIDcard: HBox = {
      //登陆后，确定userinfo不为空
      val header = Pictures.getPic(/*RmManager.userInfo.get.headImgUrl*/"666") //头像

      header.setFitHeight(35)
      header.setFitWidth(35)

      val userName = new Label(s"用户：${/*RmManager.userInfo.get.userName*/}")
      val userId = new Label(s"${/*RmManager.userInfo.get.userId*/}")

      //VBox 是竖直分布
      val userInfo = new VBox()
      userInfo.getChildren.addAll(userName, userId)
      userInfo.setSpacing(1) //内边距
      userInfo.setAlignment(Pos.CENTER_LEFT)

      val IDcard = new HBox() //把头像放在名字和id旁边
      IDcard.getChildren.addAll(header, userInfo)
      IDcard.setSpacing(5)
      IDcard.setAlignment(Pos.CENTER_LEFT)
      IDcard.setPadding(new Insets(2, 13, 2, 5))
      IDcard.getStyleClass.add("homeScene-IDcard")
      addBoxEffect(Left(IDcard)) //设置鼠标移上去会有阴影，移开没有阴影

      Tooltip.install(IDcard, new Tooltip("点击修改个人资料"))
      IDcard.addEventHandler(MouseEvent.MOUSE_CLICKED, (_: MouseEvent) => {
        listener.editInfo()
      })

      IDcard
    }

    if (false/*RmManager.userInfo.nonEmpty*/) { //如果已经登录
      val logoutIcon = new ImageView("img/logout.png")
      val logoutButton = new Button("注销", logoutIcon)
      logoutButton.setOnAction(_ => listener.logout())
      addButtonEffect(logoutButton)
      logoutButton.getStyleClass.add("homeScene-topBtn")

      topHBox.getChildren.addAll(createIDcard, logoutButton)

    }
    else {
      val loginIcon = new ImageView("img/login.png")
      loginIcon.setFitHeight(30)
      loginIcon.setFitWidth(30)
      val loginButton = new Button("登录", loginIcon)
      loginButton.setOnAction(_ => listener.gotoLoginDialog()) //按下按钮之后怎么出来弹窗的？
      addButtonEffect(loginButton)
      loginButton.getStyleClass.add("homeScene-topBtn")

      val registerIcon = new ImageView("img/register.png")
      registerIcon.setFitHeight(30)
      registerIcon.setFitWidth(30)
      val registerButton = new Button("注册", registerIcon)
      registerButton.setOnAction(_ => listener.gotoRegisterDialog())
      addButtonEffect(registerButton)
      registerButton.getStyleClass.add("homeScene-topBtn")

      topHBox.getChildren.addAll(loginButton, registerButton)
    }


    topHBox

  }

  def addMiddleArea(): StackPane = {
        val welcomeText = new Text("多人直播会议系统")
        welcomeText.getStyleClass.add("homeScene-text")

//    val welcomeText = new ImageView("img/welcomeText.png")
//    welcomeText.setFitHeight(61)
//    welcomeText.setFitWidth(690)

    val welcomeBg = new ImageView("img/welcomeBg3.png")

    welcomeBg.setFitWidth(width)
    welcomeBg.setFitHeight(height * 0.4)

    val stackPane = new StackPane() //居中且铺满的布局
    stackPane.getChildren.addAll(welcomeBg, welcomeText)
    stackPane
  }

  def addBottomArea(): HBox = {
    val liveIcon = new ImageView("img/live.png")
    liveIcon.setFitHeight(100)
    liveIcon.setFitWidth(100)
    val liveBtn = new Button("", liveIcon)
    liveBtn.setPrefSize(100, 100)
    liveBtn.getStyleClass.add("homeScene-bottomBtn")
    liveBtn.setOnAction(_ => listener.liveCheck())
    addButtonEffect(liveBtn)

    val liveText = new Text("我要直播")
    liveText.getStyleClass.add("homeScene-bottomText")

    val liveVBox = new VBox()
    liveVBox.getChildren.addAll(liveBtn, liveText)
    liveVBox.setSpacing(10)
    liveVBox.setAlignment(Pos.CENTER)

    val watchIcon = new ImageView("img/watch.png")
    watchIcon.setFitHeight(100)
    watchIcon.setFitWidth(100)
    val watchBtn = new Button("", watchIcon)
    watchBtn.getStyleClass.add("homeScene-bottomBtn")
    watchBtn.setOnAction(_ => listener.gotoRoomPage())
    addButtonEffect(watchBtn)

    val watchText = new Text("观看直播")
    watchText.getStyleClass.add("homeScene-bottomText")

    val watchVBox = new VBox()
    watchVBox.getChildren.addAll(watchBtn, watchText)
    watchVBox.setSpacing(10)
    watchVBox.setAlignment(Pos.CENTER)

    val bottomHBox = new HBox()
    bottomHBox.setSpacing(80)
    bottomHBox.getChildren.addAll(liveVBox, watchVBox)
    bottomHBox.setAlignment(Pos.BOTTOM_CENTER)
    bottomHBox.setPadding(new Insets(80, 0, 0, 0))
    bottomHBox
  }



}
