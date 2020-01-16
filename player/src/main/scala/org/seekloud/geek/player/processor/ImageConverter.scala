package org.seekloud.geek.player.processor

import java.lang

import org.bytedeco.javacv.JavaFXFrameConverter2

/**
  * User: TangYaruo
  * Date: 2019/9/17
  * Time: 17:58
  */
class ImageConverter extends JavaFXFrameConverter2 {
  private var timeGetter: () => Long = _

  def setTimeGetter(func: () => Long): Unit = timeGetter = func

  override def getTime: lang.Long = if (timeGetter != null) timeGetter() else System.currentTimeMillis()


}
