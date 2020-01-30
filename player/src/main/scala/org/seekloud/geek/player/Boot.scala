package org.seekloud.geek.player

import akka.actor.typed.DispatcherSelector
import akka.actor.{ActorSystem, Scheduler}
import akka.dispatch.MessageDispatcher
import akka.stream.ActorMaterializer
import akka.util.Timeout
import javafx.scene.Group
import javafx.scene.media.{Media, MediaPlayer}
import javafx.scene.paint.Color
import javafx.stage.Stage
import javafx.scene.Scene
import javafx.scene.media.MediaView
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Author: zwq
  * Date: 2019/8/27
  * Time: 11:10
  */
object Boot {
  import org.seekloud.geek.player.common.AppSettings._

  implicit val system: ActorSystem = ActorSystem("geek", config)
  implicit val executor: MessageDispatcher = system.dispatchers.lookup("akka.actor.my-blocking-dispatcher")
  val blockingDispatcher: DispatcherSelector = DispatcherSelector.fromConfig("akka.actor.my-blocking-dispatcher")

  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val scheduler: Scheduler = system.scheduler
  implicit val timeout: Timeout = Timeout(20 seconds)
}

class Boot extends javafx.application.Application{

  import Boot._

  private[this] val log = LoggerFactory.getLogger(this.getClass)


  override def start(primaryStage: Stage): Unit = {
    val root: Group = new Group()

//    val media: Media = new Media("http://10.1.29.245:30389/geek/distributor/getRecord/1000008/1569213160185/record.mp4")
    val media: Media = new Media("http://ivi.bupt.edu.cn/hls/cctv1.m3u8")

    val player: MediaPlayer = new MediaPlayer(media)
    val view: MediaView = new MediaView(player)
    view.setFitHeight(640)
    view.setFitHeight(360)

    root.getChildren.add(view)
    val scene: Scene = new Scene(root, 800, 400, Color.BLACK)
    primaryStage.setScene(scene)
    primaryStage.show()

    player.play()
  }



}
