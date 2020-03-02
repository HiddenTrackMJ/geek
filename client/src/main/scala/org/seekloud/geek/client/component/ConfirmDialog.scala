package org.seekloud.geek.client.component

import com.jfoenix.animation.alert.JFXAlertAnimation
import com.jfoenix.controls.{JFXAlert, JFXButton, JFXDialogLayout}
import javafx.scene.control.Label
import javafx.stage.{Modality, Stage}
import org.seekloud.geek.client.Boot
import org.slf4j.LoggerFactory

/**
  * User: hewro
  * Date: 2020/2/28
  * Time: 14:11
  * Description: 确认对话框
  */
case class ConfirmDialog(
  stage: Stage,
  title: String,
  content: String,
  yesText: String = "",
  noText: String = "",
  confirmAction: Option[()=>Unit] = None,
  refuseAction: Option[()=>Unit] = None,
  isCanClose:Boolean = false
){
  private val log = LoggerFactory.getLogger(this.getClass)

  def show() = {
    log.info("ConfirmDialog")

    try {

      Boot.addToPlatform{

        val layout = new JFXDialogLayout()

        layout.setHeading(new Label(title))
        layout.setBody(new Label(content))

        val alert = new JFXAlert[Void](stage)

        alert.setOverlayClose(isCanClose)

        alert.setAnimation(JFXAlertAnimation.CENTER_ANIMATION)

        alert.setContent(layout)
        alert.initModality(Modality.NONE)

        var buttonList :List[JFXButton] =  List.empty

        if (confirmAction nonEmpty){
          val confirmButton = new JFXButton(yesText)
          confirmButton.getStyleClass.add("dialog-accept")
          confirmButton.setOnAction(_ => {
            confirmAction.foreach(func => func())
            alert.hideWithAnimation()})

          buttonList = buttonList :+ confirmButton
        }

        if (refuseAction nonEmpty){
          val refuseButton = new JFXButton(noText)
          refuseButton.getStyleClass.add("dialog-accept")
          refuseButton.setOnAction(_ => {
            refuseAction.foreach(func => func())
            alert.hideWithAnimation()
          })
          buttonList = buttonList :+ refuseButton
        }

        layout.setActions(buttonList:_*)

        alert.setContent(layout)

        alert.showAndWait()

      }

    }catch {
      case e: Throwable =>
        log.info(s"dialog error: $e")
    }
  }

}
