package org.seekloud.geek.client.component

import java.util.Optional

import javafx.scene.control.Alert.AlertType
import javafx.scene.control.{Alert, ButtonType}


object WarningDialog {

  def initWarningDialog(context: String): Optional[ButtonType] = {
    val alert = new Alert(AlertType.WARNING)
    alert.setTitle("警告")
    alert.setHeaderText("")
    alert.setContentText(context)
    alert.showAndWait()
  }

}
