package org.seekloud.geek.client.component

import akka.actor.typed.ActorRef
import com.jfoenix.controls.JFXToolbar
import javafx.geometry.Insets
import javafx.scene.control.Label
import javafx.scene.input.MouseEvent
import javafx.scene.layout.{Background, BackgroundFill, ColumnConstraints, GridPane}
import javafx.scene.paint.Color
import org.seekloud.geek.client.common.StageContext
import org.seekloud.geek.client.core.RmManager.BackToHome
import org.seekloud.geek.client.core.{RmManager, SceneManager}
import org.slf4j.LoggerFactory

/**
  * User: hewro
  * Date: 2020/2/18
  * Time: 23:38
  * Description: 自定义标题栏统一，使用mac的样式，只有关闭和最小化两个按钮，禁止调整窗口尺寸
  */
case class TopBar(
  title:String = "", //标题
  color:Color = Color.TRANSPARENT, //标题栏背景颜色
  width:Double, //标题栏的长度
  height:Double, //标题栏的高度
  name: String, //根据不同name关闭窗口执行的操作不同
  context: StageContext,
  rmManager:ActorRef[RmManager.RmCommand]
){

  //todo 需要给整个窗体一个边框阴影

  private val log = LoggerFactory.getLogger(this.getClass)

  def apply(): JFXToolbar = {
    val primaryStage = context.getStage
    val topBar = new JFXToolbar()
    topBar.setPrefWidth(width)
    topBar.setPrefHeight(height)
    topBar.setBackground(new Background(new BackgroundFill(color, null, null)))

    //左侧边栏的按钮栏
    val pane = new GridPane()
    pane.setPadding(new Insets(10,0,7,10))
    val bWidth = 50
    pane.setPrefWidth(bWidth)
    (0 to 1).foreach{
      _=>
        val column = new ColumnConstraints(bWidth/2)
        pane.getColumnConstraints.add(column)
    }


    val close = RippleIcon(List("fas-circle:15:#7a0d07"))()._1
    close.setOnMouseClicked{
      _=>
        //关闭当前窗口
        name match {
          case "host" =>
            //跳转到user窗口
            rmManager ! BackToHome
//            SceneManager.showUserScene(context,rmManager)

          case "user" =>
            //跳转到login窗口
            SceneManager.showLoginScene(context,rmManager)
          case "login" =>
            // 关闭窗口
            primaryStage.close()
            System.exit(0)
        }
    }

    pane.add(close,0,0)

    val min = RippleIcon(List("fas-minus-circle:15:#ffbe2d", "fas-circle:15:#985c00"))()._1
    min.setOnMouseClicked{
      _=>
        //最小化当前窗口
        primaryStage.setIconified(true)

    }

    close.setOnMouseEntered{
      _=>
        mouseEnter
    }
    min.setOnMouseEntered{
      _=>
        mouseEnter
    }

    close.setOnMouseExited{
      _=>
        mouseExit
    }

    min.setOnMouseExited{
      _=>
        mouseExit
    }




    def mouseEnter ={
      log.info("mouseEnter")
      close.getChildren.removeAll()
      val m = RippleIcon(List("fas-circle:15:#7a0d07","fas-times-circle:15:#ff6258"))()._2
      close.getChildren.add(m)

      min.getChildren.removeAll()
      val n = RippleIcon(List("fas-circle:15:#985c00","fas-minus-circle:15:#ffbe2d"))()._2
      min.getChildren.add(n)

    }

    def mouseExit = {
      log.info("setOnMouseExited")
      close.getChildren.removeAll()
      val m = RippleIcon(List("fas-circle:15:#7a0d07"))()._2
      close.getChildren.add(m)

      min.getChildren.removeAll()
      val n = RippleIcon(List("fas-circle:15:#985c00"))()._2
      min.getChildren.add(n)
    }


    pane.add(min,1,0)

    topBar.setLeft(pane)

    if (title!=""){
      //todo 设置标题的颜色
      topBar.setCenter(new Label(title))
    }

    val isRight: Boolean = false // 是否处于右边界调整窗口状态

    val isBottomRight: Boolean = false // 是否处于右下角调整窗口状态

    val isBottom: Boolean = false // 是否处于下边界调整窗口状态

    var xOffset: Double = 0
    var yOffset: Double = 0 //自定义dialog移动横纵坐标


    //移动窗口
    topBar.setOnMouseDragged{
      event=>
        //根据鼠标的横纵坐标移动dialog位置
//        log.info("当前窗口移动了")
        event.consume()
        if (yOffset != 0) {
          primaryStage.setX(event.getScreenX - xOffset)
          if (event.getScreenY - yOffset < 0) primaryStage.setY(0)
          else primaryStage.setY(event.getScreenY - yOffset)
        }

//        val x = event.getSceneX
//        val y = event.getSceneY
        // 保存窗口改变后的x、y坐标和宽度、高度，用于预判是否会小于最小宽度、最小高度
        val nextX = primaryStage.getX
        val nextY = primaryStage.getY
        var nextWidth = primaryStage.getWidth
        var nextHeight = primaryStage.getHeight
//        if (isRight || isBottomRight) { // 所有右边调整窗口状态
//          nextWidth = x
//        }
//        if (isBottomRight || isBottom) { // 所有下边调整窗口状态
//          nextHeight = y
//        }
        // 最后统一改变窗口的x、y坐标和宽度、高度，可以防止刷新频繁出现的屏闪情况
        primaryStage.setX(nextX)
        primaryStage.setY(nextY)
        primaryStage.setWidth(nextWidth)
        primaryStage.setHeight(nextHeight)
    }
//
    //鼠标点击获取横纵坐标
    topBar.setOnMousePressed((event: MouseEvent) => {
      def foo(event: MouseEvent) = {
        event.consume()
        xOffset = event.getSceneX
        if (event.getSceneY > 46) yOffset = 0
        else yOffset = event.getSceneY
      }

      foo(event)
    })

    topBar
  }

}
