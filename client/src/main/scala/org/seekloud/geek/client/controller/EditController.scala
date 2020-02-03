package org.seekloud.geek.client.controller

import java.io.{File, FileInputStream}

import akka.actor.typed.ActorRef
import javafx.geometry.{Insets, Pos}
import javafx.scene.Group
import javafx.scene.control.ButtonBar.ButtonData
import javafx.scene.control._
import javafx.scene.effect.DropShadow
import javafx.scene.image.{Image, ImageView}
import javafx.scene.input.MouseEvent
import javafx.scene.layout.{HBox, VBox}
import javafx.scene.paint.Color
import javafx.stage.{FileChooser, Stage}
import org.seekloud.geek.client.common.{Constants, Pictures, StageContext}
import org.seekloud.geek.client.core.RmManager
import org.slf4j.LoggerFactory


/**
  * Author: 10632
  * Date: 2019/8/20
  * Time: 16:04
  */
class EditController(
  context: StageContext,
  rmManager: ActorRef[RmManager.RmCommand],
  stage: Stage
) {

  private[this] val log = LoggerFactory.getLogger(this.getClass)

  //修改信息弹窗
  def editDialog(): Option[(File, File, String)] = {
    val dialog = new Dialog[(File, File, String)]()
    dialog.setTitle("修改信息")

    var headerFile: File = null
    var coverFile: File = null
    val userNameField = new TextField(s"${RmManager.userInfo.get.userName}")

    def createLeftBox: VBox = {
      val headerLabel = new Label("我的头像：")
      headerLabel.setStyle("-fx-font: 20 KaiTi;-fx-fill: #333f50")

      val headerImg = Pictures.getPic(RmManager.userInfo.get.headImgUrl)
      headerImg.setFitHeight(220)
      headerImg.setFitWidth(220)
      val headerImgBox = new HBox()
      headerImgBox.getChildren.add(headerImg)
      headerImgBox.setAlignment(Pos.CENTER)
      headerImgBox.setPrefSize(220, 220)

      val chooseHeaderBtn = new Button("选择头像图片")
      addButtonEffect(chooseHeaderBtn)
      chooseHeaderBtn.setStyle("-fx-background-color: #dee7f4;" +
                               "-fx-border-color: #333f50;" +
                               "-fx-background-radius: 6;" +
                               "-fx-border-radius: 6;" +
                               "-fx-cursor: hand;")
      val headerFileChooser = new FileChooser

      chooseHeaderBtn.setOnAction(
        _ => {
          configureFileChooser(headerFileChooser)
          headerFile = headerFileChooser.showOpenDialog(stage)
          if (headerFile != null) {
            val image = new Image(new FileInputStream(headerFile))
            val newHeader = new ImageView(image)
            newHeader.setFitHeight(headerImg.getFitHeight)
            newHeader.setFitWidth(headerImg.getFitWidth)
            headerImgBox.getChildren.clear()
            headerImgBox.getChildren.add(newHeader)
          }
        }
      )

      val userNameLabel = new Label("我的昵称：")
      userNameLabel.setStyle("-fx-font: 20 KaiTi;-fx-fill: #333f50")
      userNameLabel.setPadding(new Insets(40, 0, 0, 0))

      val headerBox = new VBox()
      headerBox.getChildren.addAll(headerLabel, headerImgBox, chooseHeaderBtn, userNameLabel, userNameField)
      headerBox.setAlignment(Pos.CENTER)
      headerBox.setSpacing(20)
      headerBox.setPadding(new Insets(20, 45, 20, 45))
      headerBox.setStyle("-fx-background-color:#d4dbe3;-fx-background-radius: 10")

      headerBox
    }

    def createRightBox: VBox = {
      val coverLabel = new Label("直播间封面：")
      coverLabel.setStyle("-fx-font: 20 KaiTi;-fx-fill: #333f50")

      val coverImg = Pictures.getPic(RmManager.roomInfo.get.coverImgUrl, isHeader = false)
      coverImg.setFitHeight(Constants.DefaultPlayer.height / 3)
      coverImg.setFitWidth(Constants.DefaultPlayer.width / 3)
      val coverImgBox = new HBox()
      coverImgBox.getChildren.add(coverImg)
      coverImgBox.setAlignment(Pos.CENTER)
      coverImgBox.setPrefSize(Constants.DefaultPlayer.width / 3, Constants.DefaultPlayer.height / 3)

      val chooseCoverBtn = new Button("选择封面图片")
      addButtonEffect(chooseCoverBtn)
      chooseCoverBtn.setStyle("-fx-background-color: #dee7f4;" +
                              "-fx-border-color: #333f50;" +
                              "-fx-border-radius: 6;" +
                              "-fx-cursor: hand;")
      val coverFileChooser = new FileChooser

      chooseCoverBtn.setOnAction(
        _ => {
          configureFileChooser(coverFileChooser)
          coverFile = coverFileChooser.showOpenDialog(stage)
          if (coverFile != null) {
            val image = new Image(new FileInputStream(coverFile))
            val newCover = new ImageView(image)
            newCover.setFitHeight(coverImg.getFitHeight)
            newCover.setFitWidth(coverImg.getFitWidth)
            coverImgBox.getChildren.clear()
            coverImgBox.getChildren.add(newCover)
          }
        }
      )

      val rightBox = new VBox()
      rightBox.getChildren.addAll(coverLabel, coverImgBox, chooseCoverBtn)
      rightBox.setAlignment(Pos.CENTER)
      rightBox.setSpacing(20)
      rightBox.setPadding(new Insets(20, 45, 20, 45))
      rightBox.setStyle("-fx-background-color:#d4dbe3;-fx-background-radius: 10")

      rightBox
    }

    val wholeBox = new HBox()
    wholeBox.getChildren.addAll(createLeftBox, createRightBox)
    wholeBox.setAlignment(Pos.CENTER)
    wholeBox.setSpacing(30)
    wholeBox.setPadding(new Insets(50, 50, 50, 50))
    wholeBox.setStyle("-fx-background-color:#f2f5fb")

    val confirmButton = new ButtonType("确定", ButtonData.OK_DONE)

    val group = new Group()
    group.getChildren.addAll(wholeBox)
    dialog.getDialogPane.getButtonTypes.add(confirmButton)
    dialog.getDialogPane.setContent(group)
    dialog.setResultConverter(dialogButton =>
      if (dialogButton == confirmButton)
        (headerFile, coverFile, userNameField.getText())
      else
        null
    )
    var editInfo: Option[(File, File, String)] = None
    val rst = dialog.showAndWait()
    rst.ifPresent { a =>
      if (a != null)
        editInfo = Some(a)
      else
        None
    }
    editInfo
  }


  private def configureFileChooser(fileChooser: FileChooser): Unit = {
    fileChooser.setTitle("View Pictures")
    fileChooser.getExtensionFilters.addAll(
      //      new FileChooser.ExtensionFilter("All Images", "*.*"),
      new FileChooser.ExtensionFilter("JPG", "*.jpg"),
      new FileChooser.ExtensionFilter("PNG", "*.png")
    )
  }

  def addButtonEffect(button: Button): Unit = {
    val shadow = new DropShadow(10, Color.GRAY)
    button.addEventHandler(MouseEvent.MOUSE_ENTERED, (_: MouseEvent) => {
      button.setEffect(shadow)
    })

    button.addEventHandler(MouseEvent.MOUSE_EXITED, (_: MouseEvent) => {
      button.setEffect(null)
    })
  }


}
