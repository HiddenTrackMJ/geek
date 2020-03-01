package org.seekloud.geek.player.util

import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import org.slf4j.LoggerFactory

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

  private val log = LoggerFactory.getLogger(this.getClass)


  def draw (
    gc: GraphicsContext,
    image:Image,
    position: Int  //0表示左侧，右边有4个空位，可选值 1，2，3，4，
  ): Unit ={
    if (position == -1){
      gc.drawImage(image, 0, 0,gc.getCanvas.getWidth, gc.getCanvas.getHeight)
    }else if(position == 0){
      drawLeft(gc,image)
    }else{
      drawRight(gc,image,position)
    }

  }

  def drawLeft (
    gc: GraphicsContext,
    image:Image
  ) ={
    val canvas_all_width = gc.getCanvas.getWidth
    val canvas_all_height = gc.getCanvas.getHeight

    val image_w = image.getWidth //图像的宽度
    val image_h = image.getHeight //图像的高度

    val canvas_distribute_w = canvas_all_width /2
    val canvas_distribute_h = canvas_all_height //画在左侧占用整个左侧的画布


    val (x,y,canvas_last_w,canvas_last_h) = calculate(0,0,canvas_distribute_w,canvas_distribute_h,image_w,image_h)
    gc.drawImage(image, x, y,canvas_last_w, canvas_last_h)

  }


  def drawRight (
    gc: GraphicsContext,
    image:Image,
    position: Int,  //右边有4个空位，可选值 1，2，3，4，
    center:Boolean = false
  ) = {

    val canvas_all_width = gc.getCanvas.getWidth
    val canvas_all_height = gc.getCanvas.getHeight

    val image_w = image.getWidth //图像的宽度
    val image_h = image.getHeight //图像的高度


    val canvas_distribute_w = canvas_all_width /4 //左侧一半，右侧是4格，每行两格，一共4格两行
    val canvas_distribute_h = if(canvas_all_width /2 < canvas_all_height){
      canvas_all_width /2 * image_h / image_w /2
    }else{
      canvas_all_height / 2
    } //画布左侧的实际绘画高度

    val fill_h = canvas_all_height / 2 - canvas_distribute_h  //补足的高度

    //position 为2，4需要多一个1/4的宽度偏移（左侧有一列被占据了），1，2，3，4都需要多一个1/2的偏移（左侧部分被占据了）
    val offset_x = ((position -1 ) % 2) * canvas_distribute_w + canvas_all_width/2
    //position 为3，4时候需要多一个1/2的高度偏移 （上面有一排占据了）
    val offset_y = if(position >2){canvas_distribute_h + fill_h}else fill_h

    val (x,y,canvas_last_w,canvas_last_h) = calculate(offset_x,offset_y,canvas_distribute_w,canvas_distribute_h,image_w,image_h)
//    log.info(s"position:$position,x:$x,y$y,offset_x:$offset_x,offset_y:$offset_y,w:$canvas_last_w,h:$canvas_last_h")

    if (center){
      val w = canvas_last_w * 0.5
      val h = canvas_last_h * 0.5
      val n_x = x + (canvas_distribute_w -w) /2
      val n_y = y + (canvas_distribute_h -h) /2
      gc.drawImage(image, n_x, n_y,w, h)
    }else{
      gc.drawImage(image, x, y,canvas_last_w, canvas_last_h)
    }

  }


  def calculate(
    offset_x:Double,
    offset_y:Double,
    distribute_w:Double,
    distribute_h:Double,
    image_w:Double,
    image_h:Double
  ) = {
    if (distribute_w < distribute_h){//以宽度为基准
      val canvas_last_w = distribute_w
      val canvas_last_h = distribute_w * image_h / image_w
      val x = offset_x + 0
      val y = offset_y + (distribute_h - canvas_last_h) / 2
      (x,y,canvas_last_w,canvas_last_h)
    }else{
      val canvas_last_w = distribute_h * image_w / image_h
      val canvas_last_h = distribute_h
      val x = offset_x + (distribute_w  - canvas_last_w) / 2
      val y = offset_y + 0
      (x,y,canvas_last_w,canvas_last_h)
    }
  }




}
