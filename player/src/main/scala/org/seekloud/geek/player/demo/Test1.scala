package org.seekloud.geek.player.demo

import java.io.{File, FileInputStream}

import javafx.scene.{Group, Scene}
import javafx.scene.canvas.Canvas
import javafx.stage.Stage
import akka.actor.typed.scaladsl.adapter._
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.layout.{HBox, VBox}
import org.seekloud.geek.player.sdk.MediaPlayer

import scala.language.postfixOps

/**
  * Author: zwq
  * Date: 2019/9/1
  * Time: 11:20
  *
  * 播放器自主播放：绘制画面 & 播放声音（用户提供 canvas） 示例：
  *
  *
  */
class Test1 extends javafx.application.Application{

  import MediaPlayer._

  override def start(primaryStage: Stage): Unit = {

    val mediaPlayer = MediaPlayer()
    mediaPlayer.init()

    /**
      * 播放窗口 1
      *
      */
    val canvas1 = new Canvas(500, 300)
    val gc1 = canvas1.getGraphicsContext2D
    val playId1 = "1"
    val videoPlayer1 = system.spawn(VideoPlayer.create(playId1, None, None), s"VideoPlayer-$playId1")

    /*buttons*/
    val startBtn1 = new Button("start1")
    val pauseBtn1 = new Button("pause1")
    val continueBtn1 = new Button("continue1")
    val stopBtn1 = new Button("stop1")
    startBtn1.setOnAction { _ =>
      val file = new File("D:\\gmzr.mp4")
      val inputStream = new FileInputStream(file)
      mediaPlayer.start(playId1, videoPlayer1, Right(inputStream), Some(gc1))
    }
    pauseBtn1.setOnAction(_ =>
      mediaPlayer.pause(playId1)
    )
    continueBtn1.setOnAction(_ =>
      mediaPlayer.continue(playId1)
    )
    stopBtn1.setOnAction(_ =>
      mediaPlayer.stop(playId1, ()=>Unit)
    )

    /**
      * 播放窗口 2
      *
      */
    val canvas2 = new Canvas(500, 300)
    val gc2 = canvas2.getGraphicsContext2D
    val playId2 = "2"
    val videoPlayer2 = system.spawn(VideoPlayer.create(playId2, None, None), s"VideoPlayer-$playId2")

    /*buttons*/
    val startBtn2 = new Button("Start2")
    val pauseBtn2 = new Button("pause2")
    val continueBtn2 = new Button("continue2")
    val stopBtn2 = new Button("stop2")
    startBtn2.setOnAction(_ =>
      mediaPlayer.start(playId2, videoPlayer2, Left("rtmp://58.200.131.2:1935/livetv/hunantv"), Some(gc2))
    )
    pauseBtn2.setOnAction(_ =>
      mediaPlayer.pause(playId2)
    )
    continueBtn2.setOnAction(_ =>
      mediaPlayer.continue(playId2)
    )
    stopBtn2.setOnAction(_ =>
      mediaPlayer.stop(playId2, ()=>Unit)
    )

    /**
      * Layout
      *
      */
    val playBox1 = new VBox(5)
    val buttonBox1 = new HBox(10)
    buttonBox1.getChildren.addAll(startBtn1, pauseBtn1, continueBtn1, stopBtn1)
    buttonBox1.setAlignment(Pos.CENTER)
    playBox1.getChildren.addAll(canvas1, buttonBox1)
    playBox1.setAlignment(Pos.CENTER)

    val playBox2 = new VBox(5)
    val buttonBox2 = new HBox(10)
    buttonBox2.getChildren.addAll(startBtn2, pauseBtn2, continueBtn2, stopBtn2)
    buttonBox2.setAlignment(Pos.CENTER)
    playBox2.getChildren.addAll(canvas2, buttonBox2)
    playBox2.setAlignment(Pos.CENTER)

    val hBox = new HBox(20)
    hBox.getChildren.addAll(playBox1, playBox2)


    val group = new Group()
    group.getChildren.add(hBox)
    val scene = new Scene(group)
    primaryStage.setScene(scene)
    primaryStage.show()


  }


}
