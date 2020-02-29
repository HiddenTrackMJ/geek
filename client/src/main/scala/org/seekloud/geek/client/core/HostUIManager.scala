package org.seekloud.geek.client.core

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import javafx.scene.layout.GridPane
import org.seekloud.geek.client.Boot
import org.seekloud.geek.client.common.Constants.AllowStatus
import org.seekloud.geek.client.common.StageContext
import org.seekloud.geek.client.controller.GeekHostController
import org.seekloud.geek.client.core.stream.LiveManager
import org.seekloud.geek.shared.ptcl.CommonProtocol.ModeStatus
import org.slf4j.LoggerFactory

/**
  * User: hewro
  * Date: 2020/2/28
  * Time: 23:33
  * Description: 
  */
object HostUIManager{

  private val log = LoggerFactory.getLogger(this.getClass)


  trait HostUICommand

  case class UpdateUserListPaneUI(list: List[GridPane] = Nil) extends HostUICommand
  case class UpdateModeUI() extends HostUICommand

  def create(stageCtx: StageContext,host:GeekHostController): Behavior[HostUICommand] =
    Behaviors.setup[HostUICommand] { _ =>
      log.info(s"HostUIManager is starting...")
      Behaviors.receive[HostUICommand] { (_, msg) =>
        msg match {
          case msg: UpdateUserListPaneUI=>
            log.info("收到createUserListPane")
            Boot.addToPlatform{
              host.userJList.getItems.removeAll(host.userJList.getItems)
              host.userJList.getItems.addAll(msg.list:_*)
            }
            Behaviors.same

          case msg: UpdateModeUI =>

            log.info("收到UpdateModeUI消息")
            Boot.addToPlatform{
              //根据当前所有用户的发言状态，如果没有在申请发言，则为自由发言状态，反之为申请发言状态
              log.info("updateModeUI")
              if (RmManager.roomInfo.get.modeStatus == ModeStatus.ASK){
                //当前是申请发言状态
                host.mode_label.setText("申请发言")

              }else{
                //当前是自由发言状态
                host.mode_label.setText("自由发言")
              }

              //根据isAllow修改当前用户的allowButton的状态
              if (RmManager.getCurrentUserInfo().isAllow.get){
                host.allowStatus = AllowStatus.ALLOW
              }else{
                host.allowStatus = AllowStatus.NOT_ALLOW
              }
            }

            host.updateAllowUI()
            Behaviors.same
          case x =>
            log.info(s"收到未知消息:$x")

            Behaviors.same
        }
      }
    }

}
