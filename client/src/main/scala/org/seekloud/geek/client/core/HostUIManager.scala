package org.seekloud.geek.client.core

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import javafx.scene.layout.GridPane
import org.seekloud.geek.client.common.StageContext
import org.seekloud.geek.client.controller.GeekHostController
import org.seekloud.geek.client.core.stream.LiveManager
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

  def create(stageCtx: StageContext,host:GeekHostController): Behavior[HostUICommand] =
    Behaviors.setup[HostUICommand] { _ =>
      log.info(s"HostUIManager is starting...")
      Behaviors.receive[HostUICommand] { (_, msg) =>
        msg match {
          case msg: UpdateUserListPaneUI=>
            log.info("收到createUserListPane")
//            Boot.addToPlatform{
//              host.userJList.getItems.removeAll(host.userJList.getItems)
//              host.userJList.getItems.addAll(msg.list:_*)
//            }
            Behaviors.same

          case x =>
            log.info(s"收到未知消息:$x")

            Behaviors.same
        }
      }
    }

}
