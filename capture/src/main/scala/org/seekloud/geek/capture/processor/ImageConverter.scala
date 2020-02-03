package org.seekloud.geek.capture.processor

import java.lang

import org.bytedeco.javacv.JavaFXFrameConverter1

/**
  * User: TangYaruo
  * Date: 2019/9/16
  * Time: 22:28
  */
class ImageConverter extends JavaFXFrameConverter1 {

  private var timeGetter: () => Long = _

  def setTimeGetter(func: () => Long): Unit = timeGetter = func

  override def getTime(): lang.Long = if (timeGetter != null) timeGetter() else System.currentTimeMillis()

}
