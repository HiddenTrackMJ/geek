package org.seekloud.geek.client.component

import javafx.scene.control.Button
import javafx.scene.effect.DropShadow
import javafx.scene.image.ImageView
import javafx.scene.input.MouseEvent
import javafx.scene.layout.{HBox, VBox}
import javafx.scene.paint.Color

/**
  * User: TangYaruo
  * Date: 2019/9/16
  * Time: 15:18
  */
object Common {

  def addButtonEffect(button: Button): Unit = {
    val shadow = new DropShadow(10, Color.GRAY)
    button.addEventHandler(MouseEvent.MOUSE_ENTERED, (_: MouseEvent) => {
      button.setEffect(shadow)
    })

    button.addEventHandler(MouseEvent.MOUSE_EXITED, (_: MouseEvent) => {
      button.setEffect(null)
    })
  }

  def addBoxEffect(box: Either[HBox, VBox]): Unit = {
    val shadow = new DropShadow(10, Color.GRAY)
    box match{
      case Left(box) =>
        box.addEventHandler(MouseEvent.MOUSE_ENTERED, (_: MouseEvent) => {
          box.setEffect(shadow)
        })
        box.addEventHandler(MouseEvent.MOUSE_EXITED, (_: MouseEvent) => {
          box.setEffect(null)
        })
      case Right(box) =>
        box.addEventHandler(MouseEvent.MOUSE_ENTERED, (_: MouseEvent) => {
          box.setEffect(shadow)
        })

        box.addEventHandler(MouseEvent.MOUSE_EXITED, (_: MouseEvent) => {
          box.setEffect(null)
        })
    }


  }

  def addPicEffect(image: ImageView): Unit = {
    val shadow = new DropShadow(10, Color.GRAY)
    //    val glow = new Glow()
    image.addEventHandler(MouseEvent.MOUSE_ENTERED, (_: MouseEvent) => {
      image.setEffect(shadow)
    })
    image.addEventHandler(MouseEvent.MOUSE_EXITED, (_: MouseEvent) => {
      image.setEffect(null)
    })
    image.setStyle("-fx-cursor: hand;")

  }

  def getImageView(path: String, width: Double, height: Double): ImageView = {
    val img = new ImageView(path)
    img.setFitHeight(height)
    img.setFitWidth(width)
    img

  }

}
