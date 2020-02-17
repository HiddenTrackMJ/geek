package org.seekloud.geek.client.component

import javafx.scene.image.{Image, ImageView}
import javafx.scene.shape.Circle
/**
  * User: hewro
  * Date: 2020/2/17
  * Time: 15:09
  * Description: 用户圆形头像
  * refer: 参考 https://www.jianshu.com/p/779090da020f
  *
  * @param size 图片大小
  * @param src 图片地址 本地路径/网络地址（暂不支持）
  * @return
  */
case class Avatar(size:Int,src:String){

  def apply(): ImageView = {
    val image = new Image(src)
    val imageView = new ImageView(image)
    imageView.setFitWidth(size)
    imageView.setFitHeight(size)
    val circle = new Circle(15,15,15)
    imageView.setClip(circle)
    imageView
  }
}
