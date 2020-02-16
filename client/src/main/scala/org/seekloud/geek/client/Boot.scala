package org.seekloud.geek.client

import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.{ActorRef, DispatcherSelector}
import akka.actor.{ActorSystem, Scheduler}
import akka.dispatch.MessageDispatcher
import akka.stream.ActorMaterializer
import akka.util.Timeout
import javafx.application.Platform
import javafx.stage.Stage
import org.seekloud.geek.client.common.StageContext
import org.seekloud.geek.client.core.{NetImageProcessor, RmManager, SceneManager}
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.language.postfixOps
/**
 * Author: Jason
 * Date: 2020/1/16
 * Time: 13:33
 */

object Boot {

  import org.seekloud.geek.client.common.AppSettings._

  implicit val system: ActorSystem = ActorSystem("geek", config)
  implicit val executor: MessageDispatcher = system.dispatchers.lookup("akka.actor.my-blocking-dispatcher")
  val blockingDispatcher: DispatcherSelector = DispatcherSelector.fromConfig("akka.actor.my-blocking-dispatcher")

  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val scheduler: Scheduler = system.scheduler
  implicit val timeout: Timeout = Timeout(20 seconds)

  val netImageProcessor: ActorRef[NetImageProcessor.Command] = system.spawn(NetImageProcessor.create(), "netImageProcessor")


  def addToPlatform(fun: => Unit): Unit = {
    Platform.runLater(() => fun)
  }

}


class Boot extends javafx.application.Application {

  import Boot._

  private[this] val log = LoggerFactory.getLogger(this.getClass)



  override def start(primaryStage: Stage): Unit = {
    //隐藏默认的标题栏
//    primaryStage.initStyle(StageStyle.TRANSPARENT)
    val context = new StageContext(primaryStage)
    val rmManager = system.spawn(RmManager.create(context), "rmManager")
    SceneManager.showLoginScene(context,rmManager)

    primaryStage.setOnCloseRequest(event => {
      println("OnCloseRequest...")
      System.exit(0)
    })

  }

}
