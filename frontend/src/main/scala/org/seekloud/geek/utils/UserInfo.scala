package org.seekloud.geek.utils

import org.scalajs.dom

/**
  * @author Jingyi
  * @version 创建时间：2018/10/31
  */
object UserInfo {

  def removeStorage() = {
    dom.window.localStorage.removeItem("headImg")
    dom.window.localStorage.removeItem("nickname")
    dom.window.localStorage.removeItem("userId")
    dom.window.localStorage.removeItem("token")
  }

}
