package org.seekloud.geek.client.common


import javafx.scene.Scene
import javafx.stage.Stage

/**
 * Author: Jason
 * Date: 2020/1/16
 * Time: 13:36
 */



class StageContext(stage: Stage) {

  def getStage: Stage = stage

  def getStageWidth: Double = stage.getWidth

  def getStageHeight: Double = stage.getHeight

  def isFullScreen: Boolean = stage.isFullScreen


  def switchScene(scene: Scene, title: String = "geek", resize: Boolean = false, fullScreen: Boolean = false, isSetOffX: Boolean = false): Unit = {
    //    stage.centerOnScreen()
    stage.setScene(scene)
    stage.sizeToScene()
    stage.setResizable(resize)
    stage.setTitle(title)
    stage.setFullScreen(fullScreen)
    if (isSetOffX) {
      stage.setX(0)
      stage.setY(0)
    }
    stage.show()
  }


}

