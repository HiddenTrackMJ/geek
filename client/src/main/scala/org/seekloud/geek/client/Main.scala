package org.seekloud.geek.client

import akka.actor.typed.{ActorRef, DispatcherSelector}
import akka.actor.typed.scaladsl.adapter._
import akka.actor.{ActorSystem, Scheduler}
import akka.dispatch.MessageDispatcher
import akka.stream.ActorMaterializer
import akka.util.Timeout
import javafx.application.Platform
import javafx.scene.text.Font
import javafx.scene.control.Button
import javafx.event.Event
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.scene.Scene
import javafx.scene.layout.StackPane
import javafx.stage.Stage
import jdk.internal.dynalink.support.BottomGuardingDynamicLinker
import org.seekloud.geek.client.common.StageContext
import org.seekloud.geek.client.controller.{HomeController, LoginController}
import org.seekloud.geek.client.scene.HomeScene
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

/**
  * Author: xgy
  * Date: 2020/1/31
  * Time: 20:17
  */

object Main {
  import org.seekloud.geek.client.utils.RoomClient.getRoomInfo

  def main(args: Array[String]): Unit = {
//    getRoomInfo(100,"sss")
    val f = Future{5}
  f  andThen{
    case Failure(e) =>println("sss1"+e)
    case Success(e) =>println("sss2"+e)
  }

  }

}



class Main {



}




//简单的client前端demo
/*class Main extends javafx.application.Application{
  println("jk")
     def start(primaryStage: Stage): Unit = {
      val btn = new Button("d")
      btn.setOnAction(_=>println("sasaf")
      )

      val root = new StackPane()
      root.getChildren().add(btn)
      val scene = new Scene(root, 300, 250)

      primaryStage.setTitle("hello soew")
      primaryStage.setScene(scene)
      primaryStage.show()


      val stopAction: EventHandler[ActionEvent] = (e: ActionEvent) => {
        println("sff")
      }

      println("Hello")
    }

}*/
