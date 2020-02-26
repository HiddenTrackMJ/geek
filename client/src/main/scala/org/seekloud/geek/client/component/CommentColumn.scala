package org.seekloud.geek.client.component

import javafx.geometry.HPos
import javafx.scene.control.{ContentDisplay, Label}
import javafx.scene.layout.{Background, BackgroundFill, ColumnConstraints, GridPane}
import javafx.scene.paint.Color
import org.seekloud.geek.client.common.Constants.CommentType
import org.seekloud.geek.client.component.bubble.{BubbleSpec, BubbledLabel}
import org.seekloud.geek.client.core.RmManager
import org.seekloud.geek.shared.ptcl.CommonProtocol.CommentInfo

/**
  * User: hewro
  * Date: 2020/2/19
  * Time: 14:28
  * Description: 评论
  */
case class CommentColumn(
  width: Double,
  comment:CommentInfo,
  sType: Int = CommentType.USER
){
  def apply(): GridPane = {
    val gridPane = new GridPane()
    gridPane.setPrefWidth(width)
    val column = new ColumnConstraints(width)
    gridPane.getColumnConstraints.add(column)
    gridPane.add(createBubble(comment),0,0)
    gridPane
  }

  def createBubble(t:CommentInfo) = {
    val bl6 = new BubbledLabel
    if (sType == CommentType.USER){
      if (t.userId == RmManager.userInfo.get.userId){
        bl6.setText(t.content +": " + t.userName)
        bl6.setBackground(new Background(new BackgroundFill(Color.GREEN, null, null)))//自己消息是绿色的
        bl6.setBubbleSpec(BubbleSpec.FACE_RIGHT_CENTER);//自己消息在右侧
        bl6.setContentDisplay(ContentDisplay.RIGHT)
        GridPane.setHalignment(bl6,HPos.RIGHT)
      }else{
        bl6.setText(t.userName + " :" + t.content)
        bl6.setBackground(new Background(new BackgroundFill(Color.WHITE, null, null)))
        bl6.setBubbleSpec(BubbleSpec.FACE_LEFT_CENTER);//别人的消息在左侧
        bl6.setContentDisplay(ContentDisplay.LEFT)
        GridPane.setHalignment(bl6,HPos.LEFT)
      }
      bl6.getStyleClass.add("commentBubble")
      bl6
    }else{

      val label = new Label("系统消息:" + t.content)
      GridPane.setHalignment(label,HPos.CENTER)
      label.getStyleClass.add("serverCommentBubble")
      label
    }

  }
}
