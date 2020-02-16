package org.seekloud.geek.client.common


import javafx.fxml.FXMLLoader
import javafx.scene.{Parent, Scene}
import javafx.stage.Stage
import org.seekloud.geek.client.controller.GeekLoginController

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


  def switchScene(context: StageContext,controller: Any = null,scenefxml:String, title: String = "geek", resize: Boolean = false, fullScreen: Boolean = false, isSetOffX: Boolean = false): Unit = {
    //通过fxml创建scene
    val scene = createSceneByFxml(controller,scenefxml)
    showScene(scene,title,resize,fullScreen,isSetOffX)

  }

  def showScene(scene:Scene,title: String = "geek",resize: Boolean = false, fullScreen: Boolean = false, isSetOffX: Boolean = false) = {
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

  def createSceneByFxml(controller: Any = null,scenefxml:String)={
    val fxmlLoader = new FXMLLoader(this.getClass.getClassLoader.getResource(scenefxml))
    if (controller!=null){
      fxmlLoader.setController(controller)
    }
    val mainViewRoot: Parent = fxmlLoader.load()
    new Scene(mainViewRoot)
  }


}

