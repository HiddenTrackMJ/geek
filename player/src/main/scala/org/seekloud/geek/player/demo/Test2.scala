package org.seekloud.geek.player.demo

import javafx.scene.{Group, Scene}
import javafx.scene.canvas.{Canvas, GraphicsContext}
import javafx.stage.Stage
import akka.actor.typed.scaladsl.adapter._
import javafx.scene.control.Button
import javafx.scene.layout.HBox
import org.seekloud.geek.player.protocol.Messages
import org.seekloud.geek.player.protocol.Messages.AddPicture
import org.seekloud.geek.player.sdk.MediaPlayer

import scala.collection.immutable
import scala.language.postfixOps

/**
  * Author: zwq
  * Date: 2019/9/1
  * Time: 11:20
  *
  * 不需要播放器自主播放，播放器只输出图像和音频数据  示例：
  *
  *
  */
class Test2 extends javafx.application.Application{

  import MediaPlayer._

  override def start(primaryStage: Stage): Unit = {

    val playId = "zwq"

    //定义 imageQueue 和 samplesQueue，用来接收图像和音频数据
    val imageQueue = immutable.Queue[AddPicture]()
    val samplesQueue = immutable.Queue[Array[Byte]]()

    val liveImage = new Canvas(640  , 480)
    val gc: GraphicsContext = liveImage.getGraphicsContext2D
    //创建 videoPlayer，传入 imageQueue 和 samplesQueue
    val videoPlayer = system.spawn(VideoPlayer.create(playId, Some(imageQueue), Some(samplesQueue)), s"VideoPlayer-$playId")


    val mediaActor = MediaPlayer()
    mediaActor.init()


    val startBtn = new Button("start")
    //39.105.16.162
    startBtn.setOnAction(_ => mediaActor.start(playId,videoPlayer,Left("rtmp://10.1.29.247:1935/live/1000_3"),Some(gc),None)) // 开始输出

    val pauseBtn = new Button("pause")
    pauseBtn.setOnAction(_ => mediaActor.pause(playId))  // 暂停输出

    val continueBtn = new Button("continue")
    continueBtn.setOnAction(_ => mediaActor.continue(playId))  // 继续输出

    val stopBtn = new Button("stop")
    stopBtn.setOnAction(_ => mediaActor.stop(playId, ()=> Unit))         // 停止输出

    val group = new Group()
    val btnBox = new HBox()


    btnBox.getChildren.addAll(startBtn, pauseBtn, continueBtn, stopBtn)
    group.getChildren.addAll( liveImage, btnBox)
    val scene = new Scene(group)
    primaryStage.setScene(scene)
    primaryStage.show()


  }


}
