package org.seekloud.geek.client.controller

import java.io._

import akka.actor.typed.ActorRef
import javafx.geometry.{Insets, Pos}
import javafx.scene.Group
import javafx.scene.control.ButtonBar.ButtonData
import javafx.scene.control.{ButtonType, Dialog, Label, PasswordField, TextField}
import javafx.scene.image.ImageView
import javafx.scene.layout.{GridPane, HBox, VBox}
import javafx.scene.text.{Font, Text}
import org.seekloud.geek.client.Boot
import org.seekloud.geek.client.common.{Constants, StageContext}
import org.seekloud.geek.client.component.WarningDialog
import org.seekloud.geek.client.core.RmManager
import org.seekloud.geek.client.scene.HomeScene
import org.seekloud.geek.client.scene.HomeScene.HomeSceneListener
import org.seekloud.geek.client.utils.{RMClient, RoomClient}
import org.seekloud.geek.shared.ptcl.CommonProtocol.{RoomInfo, UserInfo}
import org.slf4j.LoggerFactory
import org.seekloud.geek.client.Boot.executor
import org.seekloud.geek.shared.ptcl.RoomProtocol.RoomUserInfo

import scala.concurrent.Future

/**
  * User: Arrow
  * Date: 2019/7/16
  * Time: 12:32
  * 整个home界面的管理,包括管理下面的homeScren，loginController，editController和rmManager
  */
class HomeController(
  context: StageContext,
  homeScene: HomeScene,
  loginController: LoginController,
  editController: EditController,
  rmManager: ActorRef[RmManager.RmCommand]) {

  private[this] val log = LoggerFactory.getLogger(this.getClass)
  var hasWaitingGif = false


  homeScene.setListener(new HomeSceneListener {
    override def liveCheck(): Unit = {
      if (RmManager.userInfo.nonEmpty) {
        //创建房间
        log.info("创建房间")
        showLoading()
        val userId = RmManager.userInfo.get.userId
        val roomName = s"$userId 的会议间"
        val roomDesc = "大家好才是真的好"
        RoomClient.createRoom(userId,RoomUserInfo(userId,roomName,roomDesc)).map{
          case Right(rsp) =>
            RmManager.roomInfo = Some(RoomInfo(rsp.roomId,roomName,roomDesc,userId,RmManager.userInfo.get.userName,"",observerNum=0))
            //当前用户是房主
            removeLoading()
            RmManager.userInfo.get.isHost = Some(true)
            RmManager.userInfo.get.pushStream = Some(RMClient.getPushStream(rsp.liveCode))
            rmManager ! RmManager.GoToCreateAndJoinRoom

          case Left(error: Error) =>
            log.error(s"创建房间错误$error")
            WarningDialog.initWarningDialog(s"网络请求错误")
        }

      } else {
        gotoLoginDialog(isToLive = true)
      }
    }

    override def gotoRoomPage(): Unit = {
      if (RmManager.userInfo.nonEmpty) {
        //显示加入会议的弹窗
        goToJoinRoomDialog()
      } else {
        gotoLoginDialog(isToLive = true)
      }

    }

    override def gotoLoginDialog(
      userName: Option[String] = None,
      pwd: Option[String] = None,
      isToLive: Boolean,
      isToWatch: Boolean
    ): Unit = {
      // 弹出登陆窗口
      val userInfo = loginController.loginDialog()
      if (userInfo.nonEmpty) {
        loginBySelf(userInfo, isToLive, isToWatch)
      }
    }

    override def gotoRegisterDialog(): Unit = {
      //弹出注册窗口
      val signUpInfo = loginController.registerDialog()
      if (signUpInfo.nonEmpty) {
        registerUser(signUpInfo)
      }
    }

    //注销
    override def logout(): Unit = {
      rmManager ! RmManager.Logout

    }

    //修改用户信息
    override def editInfo(): Unit = {

    }

    override def goToJoinRoomDialog(): Unit = {
      val roomId = joinRoomDialog()
      if (roomId.nonEmpty){
        //查询该会议号的房间信息
        joinRoom(RmManager.userInfo.get.userId.toString,roomId.get)
      }

    }
  })

  def showScene(): Unit = {
    Boot.addToPlatform(
      context.switchScene(homeScene.getScene, title = "geek云会议")
    )
  }

  def showLoading(): Unit = {
    Boot.addToPlatform {
      if (!hasWaitingGif) {
        homeScene.group.getChildren.add(homeScene.waitingGif)
        hasWaitingGif = true
      }
    }
  }

  def removeLoading(): Unit = {
    Boot.addToPlatform {
      if (hasWaitingGif) {
        homeScene.group.getChildren.remove(homeScene.waitingGif)
        hasWaitingGif = false
      }
    }
  }


  def joinRoomDialog() = {
    val dialog = new Dialog[(String)]()
    dialog.setTitle("填写需要加入的会议号")

    val welcomeText = new Text("加入会议")
    welcomeText.setStyle("-fx-font: 35 KaiTi;-fx-fill: #333f50")
    val upBox = new HBox()
    upBox.setAlignment(Pos.TOP_CENTER)
    upBox.setPadding(new Insets(40, 200, 0, 200))
    upBox.getChildren.add(welcomeText)


    //userNameGrid
    val roomIdIcon = new ImageView("img/userName.png")
    roomIdIcon.setFitHeight(30)
    roomIdIcon.setFitWidth(30)
    val roomIdLabel = new Label("会议号:")
    roomIdLabel.setFont(Font.font(18))
    val roomIdField = new TextField("")



    val roomId = new GridPane //格子布局
    roomId.setHgap(20)
    roomId.setVgap(30)
    roomId.add(roomIdIcon, 0, 0)
    roomId.add(roomIdLabel, 1, 0)
    roomId.add(roomIdField, 2, 0)
    roomId.setStyle("-fx-background-color:#d4dbe3;")
    roomId.setPadding(new Insets(60, 20, 60, 20))



    //bottomBox
    val bottomBox = new VBox()
    bottomBox.getChildren.addAll(roomId) //默认
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
        if (roomIdField.getText().nonEmpty) {
          (roomIdField.getText())
        } else {
          Boot.addToPlatform {
            WarningDialog.initWarningDialog("请填写会议号！")
          }
          null
        }
      } else {
        null
      }
    )
    val rst = dialog.showAndWait()
    var info:Option[String] = None
    rst.ifPresent { a =>
      if (a!= "" && a!=null)
        info = Some(a)
      else
        None
    }
    info
  }
  /**
    * 使用用户自己输入的信息登录
    */
  def loginBySelf(userInfo: Option[(String, String, String)], isToLive: Boolean, isToWatch: Boolean): Future[Unit] = {
    showLoading()
    val r = RMClient.signIn(userInfo.get._1, userInfo.get._2) //用户名登录
    r.map {
      case Right(rsp) =>
        if (rsp.errCode == 0) {//登录成功
          rmManager ! RmManager.SignInSuccess(rsp.userInfo, rsp.roomInfo)
          RmManager.userInfo = rsp.userInfo

          //todo 跳转到其他页面
//          if (isToLive) {
////            rmManager ! RmManager.GoToLive
//          } else {
//            if (isToWatch) {
////              rmManager ! RmManager.GoToRoomHall
//            } else {
//              Boot.addToPlatform {
//                removeLoading()
//                showScene()
//              }
//            }
//          }
          Boot.addToPlatform {
            removeLoading()
            showScene()
          }
        } else {//用户名或者密码错误
          log.error(s"sign in error: ${rsp.msg}")
          Boot.addToPlatform {
            removeLoading()
            WarningDialog.initWarningDialog(s"${rsp.msg}")
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


  /**
   * 注册用户
   * @param signUpInfo
   */
  def registerUser(signUpInfo:Option[(String,String)]) = {
    showLoading()

    RMClient.signUp(signUpInfo.get._1.toString, signUpInfo.get._2.toString).map {
      case Right(signUpRsp) =>
        if (signUpRsp.errCode == 0) {
          removeLoading()
          Boot.addToPlatform {
            WarningDialog.initWarningDialog("注册成功！")
          }
        } else {
          log.error(s"sign up error: ${signUpRsp.msg}")
          removeLoading()
          Boot.addToPlatform {
            WarningDialog.initWarningDialog(s"${signUpRsp.msg}")
          }
        }
      case Left(error) =>
        log.error(s"sign up server error:$error")
        removeLoading()
        Boot.addToPlatform {
          WarningDialog.initWarningDialog(s"验证超时！")
        }
    }

  }


  def joinRoom(userId:String,roomId:String)={
    //http请求
    showLoading()
    RoomClient.joinRoom(roomId.toLong,userId.toLong).map {
      case Right(rsp) =>
        if (rsp.rtmp.nonEmpty){
          //修改用户信息不是房主
          val roomUser = rsp.rtmp.get.roomUserInfo
          //todo: 房主的用户名的信息没有，也没有当前的房间参与人数
          RmManager.roomInfo = Some(RoomInfo(roomId.toLong,roomUser.roomName,roomUser.des,roomUser.userId,"路人甲",observerNum = 1))
          RmManager.userInfo.get.isHost = Some(false)
          //跳转到视频页面
          rmManager ! RmManager.GoToCreateAndJoinRoom
        }else{
          WarningDialog.initWarningDialog(s"没有该房间号")
        }

      case Left(error) =>
        //请求失败
        log.error(s"加入房间错误：$error")
        WarningDialog.initWarningDialog(s"网络错误")
    }
  }
}
