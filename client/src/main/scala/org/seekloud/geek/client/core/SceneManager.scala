package org.seekloud.geek.client.core

import akka.actor.typed.ActorRef
import org.seekloud.geek.client.Boot
import org.seekloud.geek.client.common.StageContext
import org.seekloud.geek.client.controller.{GeekLoginController, GeekUserController}

/**
  * User: hewro
  * Date: 2020/2/16
  * Time: 20:02
  * Description: 管理各个页面的切换
  */
object SceneManager {

  def showLoginScene(context: StageContext,rmManager:ActorRef[RmManager.RmCommand])= {
    Boot.addToPlatform{
      val controller = new GeekLoginController(rmManager,context)
      context.switchScene(context,controller,"scene/geek-login.fxml")
    }
  }

  def showUserScene(context: StageContext,rmManager:ActorRef[RmManager.RmCommand]): Unit = {
    Boot.addToPlatform{
      val controller = new GeekUserController(rmManager,context)
      context.switchScene(context,controller,"scene/geek-user.fxml")
    }
  }


}
