package org.seekloud.geek.client.component

import java.util.Optional

import javafx.scene.control.Alert.AlertType
import javafx.scene.control.{Alert, ButtonType}

/**
  * User: TangYaruo
  * Date: 2019/3/8
  * Time: 10:55
  */
object WarningDialog {

  def initWarningDialog(context: String): Optional[ButtonType] = {
    val alert = new Alert(AlertType.WARNING)
    alert.setTitle("警告")
    alert.setHeaderText("")
    alert.setContentText(context)
    alert.showAndWait()
  }

}
