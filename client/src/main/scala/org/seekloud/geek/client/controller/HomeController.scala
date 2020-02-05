package org.seekloud.geek.client.controller

import java.io._

import akka.actor.typed.ActorRef
import org.seekloud.geek.client.Boot
import org.seekloud.geek.client.common.{Constants, StageContext}
import org.seekloud.geek.client.component.WarningDialog
import org.seekloud.geek.client.core.RmManager
import org.seekloud.geek.client.scene.HomeScene
import org.seekloud.geek.client.scene.HomeScene.HomeSceneListener
import org.seekloud.geek.client.utils.RMClient
import org.seekloud.geek.shared.ptcl.CommonProtocol.{RoomInfo, UserInfo}
import org.slf4j.LoggerFactory

import org.seekloud.geek.client.Boot.executor

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
        if (RmManager.roomInfo.isEmpty){
          RmManager.roomInfo = Some(RoomInfo(10001,"","",RmManager.userInfo.get.userId,RmManager.userInfo.get.userName,"",""))
        }
        rmManager ! RmManager.GoToCreateRoom
      } else {
        gotoLoginDialog(isToLive = true)
      }
    }

    override def gotoRoomPage(): Unit = {
//      rmManager ! RmManager.GoToRoomHall
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
//      val editInfo = editController.editDialog()
//      if (editInfo.nonEmpty) {
//        log.debug("start changeUserName...")
//        if (editInfo.get._3 != RmManager.userInfo.get.userName) {
////          RMClient.changeUserName(RmManager.userInfo.get.userId, editInfo.get._3).map {
////            case Right(rsp) =>
////              if (rsp.errCode == 0) {
////                rmManager ! RmManager.ChangeUserName(editInfo.get._3)
////                log.debug(s"changeUserName success.")
////              } else {
////                log.error(s"changeUserName error: ${rsp.msg},errCode:${rsp.errCode}")
////                Boot.addToPlatform {
////                  WarningDialog.initWarningDialog(s"${rsp.msg}")
////                }
////              }
////            case Left(error) =>
////              log.error(s"upload header server error:$error")
////              Boot.addToPlatform {
////                WarningDialog.initWarningDialog(s"服务器出错: $error")
////              }
////          }
//        }
//        if (editInfo.get._1 != null) {
//          log.debug("start uploading header...")
////          RMClient.uploadImg(editInfo.get._1, RmManager.userInfo.get.userId, CommonInfo.ImgType.headImg).map {
////            case Right(imgChangeRsp) =>
////              if (imgChangeRsp.errCode == 0) {
////                val headerUrl = imgChangeRsp.url
////                rmManager ! RmManager.ChangeHeader(headerUrl)
////                log.debug(s"upload header success,url:$headerUrl")
////              } else {
////                log.error(s"upload header error: ${imgChangeRsp.msg},errCode:${imgChangeRsp.errCode}")
////                Boot.addToPlatform {
////                  WarningDialog.initWarningDialog(s"${imgChangeRsp.msg}")
////
////                }
////              }
////            case Left(error) =>
////              log.error(s"upload header server error:$error")
////              Boot.addToPlatform {
////                WarningDialog.initWarningDialog(s"服务器出错: $error")
////              }
////          }
//        }
//        if (editInfo.get._2 != null) {
//          log.debug(s"start uploading cover...")
////          RMClient.uploadImg(editInfo.get._2, RmManager.userInfo.get.userId, CommonInfo.ImgType.coverImg).map {
////            case Right(imgChangeRsp) =>
////              if (imgChangeRsp.errCode == 0) {
////                val coverUrl = imgChangeRsp.url
////                rmManager ! RmManager.ChangeCover(coverUrl)
////                log.debug(s"upload cover success,url:$coverUrl")
////              } else {
////                log.error(s"upload cover error: ${imgChangeRsp.msg}")
////                Boot.addToPlatform {
////                  WarningDialog.initWarningDialog(s"${imgChangeRsp.msg}")
////
////                }
////              }
////            case Left(error) =>
////              log.error(s"upload cover server error:$error")
////              Boot.addToPlatform {
////                WarningDialog.initWarningDialog(s"服务器出错: $error")
////              }
////          }
//        }
//      }

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
          RmManager.roomInfo = rsp.roomInfo
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
}
