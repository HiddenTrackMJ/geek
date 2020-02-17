package org.seekloud.geek.client.component

import com.jfoenix.controls.JFXRippler
import org.kordamp.ikonli.javafx.FontIcon

/**
  * User: hewro
  * Date: 2020/2/17
  * Time: 22:00
  * Description: 点击有波纹效果的fontIcon
  */
case class RippleIcon(IconLiteral:String){
  def apply(): JFXRippler = {
    new JFXRippler(new FontIcon(IconLiteral))
  }
}
