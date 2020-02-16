package org.seekloud.geek.client.utils

/**
 * User: hewro
 * Date: 2020/2/14
 * Time: 23:05
 * Description: 
 */
import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.{Parent, Scene}
import javafx.stage.Stage

class FxApp extends Application {
  override def start(primaryStage: Stage): Unit = {
    primaryStage.setTitle("Fx App")

//    import com.jfoenix.svg.SVGGlyphLoader
//    SVGGlyphLoader.loadGlyphsFont(this.getClass.getResourceAsStream("/fonts/icomoon.svg"), "icomoon.svg")

    val fxmlLoader = new FXMLLoader(this.getClass.getClassLoader.getResource("scene/HomeScene2.fxml"))
    val mainViewRoot: Parent = fxmlLoader.load()

    val scene = new Scene(mainViewRoot)
    primaryStage.setScene(scene)
    primaryStage.show()
  }
}

//object FxApp {
//  def main (args: Array[String]): Unit = {
//    Application.launch(classOf[FxApp], args:_*)
//  }
//}