package org.seekloud.geek.player.util

import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image

/**
  * User: hewro
  * Date: 2020/2/27
  * Time: 15:01
  * Description: 画图的工具类
  */
object GCUtil {

  object Direction{
    val LEFT = 0
    val RIGHT = 1
  }


  def drawLeft (
    gc: GraphicsContext,
    canvas_all_width:Double,
    canvas_all_height:Double,
    image_w:Double,
    image_h:Double,
    image:Image
  ) ={
    val canvas_distribute_w = canvas_all_width /2
    val canvas_distribute_h = canvas_all_height //画在左侧占用整个左侧的画布

    if (canvas_distribute_w < canvas_distribute_h){//以宽度为基准




//      gc.drawImage(image, x, y, canvas_last_w, canvas_last_h)
    }else{//以高为基准
      val canvas_last_w = canvas_distribute_h * image_w / image_h
      val canvas_last_h = canvas_distribute_h
      val x = (canvas_distribute_w  - canvas_last_w) / 2
      val y = 0


      gc.drawImage(image, x, y,canvas_last_w, canvas_last_h)
    }

  }

  def drawRight (
    gc: GraphicsContext,
    canvas_all_width:Double,
    canvas_all_height:Double,
    image_w:Double,
    image_h:Double,
    position: Int  //右边有4个空位，可选值 1，2，3，4，
  ) = {

  }


  def calculate(
    offset_x:Double,
    offset_y:Double,
    distribute_w:Double,
    distribute_h:Double
  ) = {

    if (distribute_w < distribute_h){//以宽度为基准

    }


  }




}
