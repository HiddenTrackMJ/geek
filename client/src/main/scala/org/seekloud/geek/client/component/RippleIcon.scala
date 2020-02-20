package org.seekloud.geek.client.component

import com.jfoenix.controls.JFXRippler
import com.jfoenix.controls.JFXRippler.RipplerMask
import javafx.scene.layout.StackPane
import org.kordamp.ikonli.javafx.FontIcon

/**
  * User: hewro
  * Date: 2020/2/17
  * Time: 22:00
  * Description: 点击有波纹效果的fontIcon
  */
case class RippleIcon(IconLiterals:List[String]){
  def apply() = {

    val s = new StackPane()
    val f= IconLiterals.map{
      IconLiteral=>
//        println("图标名称:"+ IconLiteral)
        new FontIcon(IconLiteral)
    }
    s.getChildren.addAll(f:_*)

    val j = new JFXRippler(s)
    j.setMaskType(RipplerMask.CIRCLE)
    (j,s,f)
  }
}
