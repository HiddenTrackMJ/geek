package org.seekloud.geek.common

/**
  * User: Jason
  * Date: 2019/5/24
  * Time: 15:34
  */

object Route {

  val baseUrl = "/geek"

  def imgPath(fileName: String): String = baseUrl + "/static/img/" + fileName

  object Admin {
    val base: String = baseUrl + "/admin"
  }

  object Room {
    val base: String = baseUrl + "/room"
    val createRoom: String = base + "/createRoom"
    val startLive: String = base + "/startLive"
    val stopLive: String = base + "/stopLive"
    val getRoomList: String = base + "/getRoomList"
    val getUserInfo: String = base + "/getUserInfo"
    val updateRoomInfo: String = base + "/updateRoomInfo"
  }

  object User {
    val base: String = baseUrl + "/user"
    val signUp: String = base + "/signUp"
    val signIn: String = base + "/signIn"
  }

  object Invitation {
    val base: String = baseUrl + "/invitation"
    val getInviterList: String = base + "/getInviterList"
    val getInviteeList: String = base + "/getInviteeList"
    val delInvitee: String = base + "/delInvitee"
    val signIn: String = base + "/signIn"
  }



}
