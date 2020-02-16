package org.seekloud.geek.client.component

import com.jfoenix.controls.JFXSpinner
import javafx.scene.layout.Pane
import org.seekloud.geek.client.Boot

/**
  * User: hewro
  * Date: 2020/2/16
  * Time: 17:16
  * Description: 加载条-圆形
  */
case class Loading(
  root:Pane
){

  var hasWaitingGif = false
  val rootPane: Pane = root
  val waitingGif = new JFXSpinner()
  waitingGif.setPrefWidth(50)
  waitingGif.setPrefHeight(50)

  def showLoading() = {
    Boot.addToPlatform {
      if (!hasWaitingGif) {
        waitingGif.setLayoutX(rootPane.getWidth / 2 - 25)
        waitingGif.setLayoutY(rootPane.getHeight / 2 - 25)
        rootPane.getChildren.addAll(waitingGif)
        hasWaitingGif = true
      }
    }
  }

  def removeLoading(): Unit = {
    Boot.addToPlatform {
      if (hasWaitingGif) {
        rootPane.getChildren.remove(waitingGif)
        hasWaitingGif = false
      }
    }
  }

}
