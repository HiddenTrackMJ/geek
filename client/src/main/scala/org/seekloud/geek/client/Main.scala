package org.seekloud.geek.client

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.{Failure, Success}

/**
  * Author: xgy
  * Date: 2020/1/31
  * Time: 20:17
  */

object Main {

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
