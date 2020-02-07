package org.seekloud.geek.client.controller

import akka.actor.typed.ActorRef
import org.seekloud.geek.client.Boot
import org.seekloud.geek.client.common.{Constants, StageContext}
import org.seekloud.geek.client.component.WarningDialog
import org.seekloud.geek.client.core.RmManager
import org.seekloud.geek.client.scene.HostScene
import org.seekloud.geek.client.scene.HostScene.{AudienceListInfo, HostSceneListener}
import org.seekloud.geek.shared.client2Manager.websocket.AuthProtocol.WsMsgRm
import org.slf4j.LoggerFactory

/**
  * User: Arrow
  * Date: 2019/7/16
  * Time: 12:33
  */
class HostController(
  context: StageContext,
  hostScene: HostScene,
  rmManager: ActorRef[RmManager.RmCommand]
) {

  private[this] val log = LoggerFactory.getLogger(this.getClass)
  var isConnecting = false
  var isLive = false
  var likeNum: Int = 999

  def showScene(): Unit = {
    Boot.addToPlatform(
      if (RmManager.userInfo.nonEmpty && RmManager.roomInfo.nonEmpty) {
        context.switchScene(hostScene.getScene, title = s"${RmManager.userInfo.get.userName}的直播间-${RmManager.roomInfo.get.roomName}")
      } else {
        WarningDialog.initWarningDialog(s"无房间信息！")
      }
    )
  }

  hostScene.setListener(new HostSceneListener {
    override def startLive(): Unit = {//开始推流和拉流
      rmManager ! RmManager.HostLiveReq
    }

    override def stopLive(): Unit = {//停止推流和拉流
      rmManager ! RmManager.StopLive
    }

    override def modifyRoomInfo(name: Option[String], des: Option[String]): Unit = {//修改房间信息
//      rmManager ! RmManager.ModifyRoom(name, des)
    }

    override def changeRoomMode(isJoinOpen: Option[Boolean], aiMode: Option[Int], screenLayout: Option[Int]): Unit = {
//      rmManager ! RmManager.ChangeMode(isJoinOpen, aiMode, screenLayout)
    }

    override def audienceAcceptance(userId: Long, accept: Boolean, newRequest: AudienceListInfo): Unit = {
//      if (!isConnecting) {
//        rmManager ! RmManager.AudienceAcceptance(userId, accept)
//        hostScene.audObservableList.remove(newRequest)
//      } else {
//        if (isConnecting && !accept) {
//          rmManager ! RmManager.AudienceAcceptance(userId, accept)
//          hostScene.audObservableList.remove(newRequest)
//        } else {
//          Boot.addToPlatform {
//            WarningDialog.initWarningDialog(s"无法重复连线，请先断开当前连线。")
//          }
//        }
//      }
    }

    override def shutJoin(): Unit = {
//      rmManager ! RmManager.ShutJoin
    }

    override def gotoHomeScene(): Unit = {
      //回到首页
      rmManager ! RmManager.BackToHome
    }

    override def setFullScreen(): Unit = {
      if (!hostScene.isFullScreen) {
        hostScene.removeAllElement()

        context.getStage.setFullScreen(true)

        hostScene.liveImage.setWidth(context.getStageWidth)
        hostScene.liveImage.setHeight(context.getStageHeight)
//        hostScene.barrageCanvas.setWidth(context.getStageWidth)
//        hostScene.barrageCanvas.setHeight(context.getStageHeight)
        hostScene.statisticsCanvas.setWidth(context.getStageWidth)
        hostScene.statisticsCanvas.setHeight(context.getStageHeight)
        hostScene.gc.drawImage(hostScene.backImg, 0, 0, context.getStageWidth, context.getStageWidth)

        hostScene.isFullScreen = true
      }
    }

    override def exitFullScreen(): Unit = {
      if (hostScene.isFullScreen) {
        hostScene.liveImage.setWidth(Constants.DefaultPlayer.width)
        hostScene.liveImage.setHeight(Constants.DefaultPlayer.height)
//        hostScene.barrageCanvas.setWidth(Constants.DefaultPlayer.width)
//        hostScene.barrageCanvas.setHeight(Constants.DefaultPlayer.height)
        hostScene.statisticsCanvas.setWidth(Constants.DefaultPlayer.width)
        hostScene.statisticsCanvas.setHeight(Constants.DefaultPlayer.height)
        hostScene.gc.drawImage(hostScene.backImg, 0, 0, Constants.DefaultPlayer.width, Constants.DefaultPlayer.height)

        hostScene.addAllElement()
        context.getStage.setFullScreen(false)

        hostScene.isFullScreen = false
      }
    }

//    override def sendCmt(comment: Comment): Unit = {
////            log.debug(s"host send comment:$comment")
//      rmManager ! RmManager.SendComment(comment)
//    }

    override def changeOption(bit: Option[Int] = None, re: Option[String] = None, frameRate: Option[Int] = None, needImage: Boolean = true, needSound: Boolean = true): Unit = {
//      rmManager ! RmManager.ChangeOption(bit, re, frameRate, needImage, needSound)
    }

    override def recordOption(recordOrNot: Boolean, recordType: String, path: Option[String] = None): Unit = {

//      recordType match {
//        case "录制自己" => rmManager ! RmManager.RecordOption(recordOrNot, path)
//        case "录制别人" => path.foreach(i => rmManager ! RmManager.StartRecord(i))
//      }
    }

    override def ask4Loss(): Unit = {
      rmManager ! RmManager.GetPackageLoss
    }

  })


  //处理与后端roomManager的ws连接消息处理
  def wsMessageHandle(data: WsMsgRm): Unit = {
    data match {

//      case msg: HeatBeat =>
////        log.debug(s"heartbeat: ${msg.ts}")
//        rmManager ! HeartBeat
//
//      case msg: StartLiveRsp =>
////        log.debug(s"get StartLiveRsp: $msg")
//        if (msg.errCode == 0) {
//          rmManager ! RmManager.StartLive(msg.liveInfo.get.liveId, msg.liveInfo.get.liveCode)
//        } else {
//          Boot.addToPlatform {
//            WarningDialog.initWarningDialog(s"${msg.msg}")
//          }
//        }
//
//      case msg: ModifyRoomRsp =>
//        //若失败，信息改成之前的信息
////        log.debug(s"get ModifyRoomRsp: $msg")
//        if (msg.errCode == 0) {
//          //          log.debug(s"更改房间信息成功！")
//          Boot.addToPlatform {
//            WarningDialog.initWarningDialog("更改房间信息成功！")
//          }
//          // do nothing
//        } else {
//          log.debug(s"更改房间信息失败！原房间信息为：${hostScene.roomInfoMap}")
//          Boot.addToPlatform {
//            val roomName = hostScene.roomInfoMap(RmManager.roomInfo.get.roomId).head
//            val roomDes = hostScene.roomInfoMap(RmManager.roomInfo.get.roomId)(1)
//            hostScene.roomNameField.setText(roomName)
//            hostScene.roomDesArea.setText(roomDes)
//          }
//        }
//
//      case msg: ChangeModeRsp =>
//        if (msg.errCode != 0) {
//          Boot.addToPlatform {
//            WarningDialog.initWarningDialog("该项设置目前不可用！")
//          }
//        }
//
//      case msg: AudienceJoin =>
//        //将该条信息展示在host页面(TableView)
//        log.debug(s"Audience-${msg.userName} send join req.")
//        Boot.addToPlatform {
//          hostScene.updateAudienceList(msg.userId, msg.userName)
//        }
//
//
//      case msg: AudienceJoinRsp =>
//        if (msg.errCode == 0) {
//          //显示连线观众信息
//          rmManager ! RmManager.JoinBegin(msg.joinInfo.get)
//
//          Boot.addToPlatform {
//            if (!hostScene.tb3.isSelected) {
//              hostScene.tb3.setGraphic(hostScene.connectionIcon1)
//            }
//            hostScene.connectionStateText.setText(s"与${msg.joinInfo.get.userName}连线中")
//            hostScene.connectStateBox.getChildren.add(hostScene.shutConnectionBtn)
//            isConnecting = true
//          }
//
//        } else {
//          Boot.addToPlatform {
//            WarningDialog.initWarningDialog(s"观众加入出错:${msg.msg}")
//          }
//        }
//
//      case AudienceDisconnect =>
//        //观众断开，提醒主播，去除连线观众信息
//        rmManager ! RmManager.JoinStop
//        Boot.addToPlatform {
//          if (!hostScene.tb3.isSelected) {
//            hostScene.tb3.setGraphic(hostScene.connectionIcon1)
//          }
//          hostScene.connectionStateText.setText(s"目前状态：无连接")
//          hostScene.connectStateBox.getChildren.remove(hostScene.shutConnectionBtn)
//          isConnecting = false
//        }
//
//      case msg: RcvComment =>
//        //判断userId是否为-1，是的话当广播处理
////        log.info(s"receive comment msg: ${msg.userName}-${msg.comment}")
//        Boot.addToPlatform {
//          hostScene.commentBoard.updateComment(msg)
//          hostScene.barrage.updateBarrage(msg)
//        }
//
//      case msg: UpdateAudienceInfo =>
////        log.info(s"update audienceList.")
//        Boot.addToPlatform {
//          hostScene.watchingList.updateWatchingList(msg.AudienceList)
//        }
//
//
//      case msg: ReFleshRoomInfo =>
////        log.debug(s"host receive likeNum update: ${msg.roomInfo.like}")
//        likeNum = msg.roomInfo.like
//        Boot.addToPlatform {
//          hostScene.likeLabel.setText(likeNum.toString)
//        }
//
//      case HostStopPushStream2Client =>
//        Boot.addToPlatform {
//          WarningDialog.initWarningDialog("直播成功停止，已通知所有观众。")
//        }
//
//      case BanOnAnchor =>
//        Boot.addToPlatform {
//          WarningDialog.initWarningDialog("你的直播已被管理员禁止！")
//        }
//        rmManager ! RmManager.BackToHome

      case x =>
        log.warn(s"host recv unknown msg from rm: $x")
    }

  }

}
