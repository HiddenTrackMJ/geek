package org.seekloud.geek.client.component

import com.jfoenix.controls.JFXSpinner
import javafx.scene.layout.Pane
import org.seekloud.geek.client.Boot
import org.slf4j.LoggerFactory

/**
  * User: hewro
  * Date: 2020/2/16
  * Time: 17:16
  * Description: 加载条-圆形
  */
case class Loading(
  root:Pane
){
  private val log = LoggerFactory.getLogger(this.getClass)

  var hasWaitingGif = false
  var waitingGif:JFXSpinner = _

  def build() = {
    log.info("dialog:build")
    waitingGif= new JFXSpinner()
    waitingGif.setPrefWidth(50)
    waitingGif.setPrefHeight(50)
    this
  }

  def showLoading() = {
    log.info("showLoading")
    Boot.addToPlatform {
      if (!hasWaitingGif) {
        waitingGif.setLayoutX(root.getWidth / 2 - 25)
        waitingGif.setLayoutY(root.getHeight / 2 - 25)
        root.getChildren.addAll(waitingGif)
        hasWaitingGif = true
      }
    }
  }

  def removeLoading(): Unit = {
    Boot.addToPlatform {
      if (hasWaitingGif) {
        root.getChildren.remove(waitingGif)
        hasWaitingGif = false
      }
    }
  }

}
