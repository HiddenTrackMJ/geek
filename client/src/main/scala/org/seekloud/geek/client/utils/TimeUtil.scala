package org.seekloud.geek.client.utils

import java.text.SimpleDateFormat

/**
  * User: Jason
  * Date: 2019/9/18
  * Time: 16:05
  */
object TimeUtil {
  def timeStamp2DetailDate(timestamp: Long): String = {
    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(timestamp)
  }
}
