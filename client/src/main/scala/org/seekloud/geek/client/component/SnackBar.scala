package org.seekloud.geek.client.component

import com.jfoenix.controls.JFXSnackbar.SnackbarEvent
import com.jfoenix.controls.{JFXSnackbar, JFXSnackbarLayout}
import javafx.scene.layout.Pane
import javafx.util.Duration

/**
  * User: hewro
  * Date: 2020/2/16
  * Time: 15:32
  * Description: 底部消息条
  */
object SnackBar{

  def show(root:Pane,message:String): Unit ={
    val snackbar = new JFXSnackbar(root)
    snackbar.setPrefWidth(300)
    snackbar.fireEvent(new JFXSnackbar.SnackbarEvent(new JFXSnackbarLayout(message),Duration.millis(3000),null))
  }
}
