package org.seekloud.geek.client.component

import javafx.geometry.HPos
import javafx.scene.control.ContentDisplay
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
    if (sType == CommentType.USER){
      (0 to 1).foreach{
        _ =>
          val column = new ColumnConstraints(width/2)
          gridPane.getColumnConstraints.add(column)
      }

      if (comment.userId == RmManager.userInfo.get.userId){//自己的消息在右侧
        gridPane.add(createBubble(comment), 1, 0)
      }else{//别人消息在左侧
        gridPane.add(createBubble(comment), 0, 0)
      }
    }else{//系统消息居中，占满一行
      gridPane.add(createBubble(comment),0,0)
    }

    gridPane.setPrefWidth(width)
    gridPane
  }

  def createBubble(t:CommentInfo) = {
    val bl6 = new BubbledLabel
    if (sType == CommentType.USER){
      if (t.userId == RmManager.userInfo.get.userId){
        bl6.setText(t.content +": " + t.userName)
        bl6.setBackground(new Background(new BackgroundFill(Color.GREEN, null, null)))//自己消息是绿色的
        bl6.setBubbleSpec(BubbleSpec.FACE_RIGHT_CENTER);//自己消息在右侧
        GridPane.setHalignment(bl6,HPos.RIGHT)
      }else{
        bl6.setText(t.userName + " :" + t.content)
        bl6.setBackground(new Background(new BackgroundFill(Color.WHITE, null, null)))
        bl6.setBubbleSpec(BubbleSpec.FACE_LEFT_CENTER);//别人的消息在左侧
        //      bl6.setPrefWidth(commentPane.getPrefWidth * 0.8)
        bl6.setContentDisplay(ContentDisplay.RIGHT)
      }
      bl6.getStyleClass.add("commentBubble")

    }else{
      bl6.setText(t.content)
      bl6.setBackground(new Background(new BackgroundFill(Color.WHITE, null, null)))
      bl6.setBubbleSpec(BubbleSpec.FACE_BOTTOM)
      bl6.getStyleClass.add("serverCommentBubble")
    }

    bl6
  }
}
