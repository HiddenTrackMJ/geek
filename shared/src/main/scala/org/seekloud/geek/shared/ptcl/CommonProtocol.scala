package org.seekloud.geek.shared.ptcl

/**
 * User: hewro
 * Date: 2020/1/31
 * Time: 18:52
 * Description: 一些共用的协议
 */
object CommonProtocol {

  trait Request

  trait Response {
    val errCode: Int
    val msg: String
  }



  case class UserInfo(
    userId: Long,
    userName: String,
    headImgUrl:String
  )

  case class SignIn(
    userName: String,
    password: String
  ) extends Request


  case class SignUp(
    userName: String,
    password: String,
    url: String //重定向url
  ) extends Request



  case class SignUpRsp(
    //    code:String,
    errCode: Int = 0,
    msg: String = "ok"
  ) extends Response

  case class SignInRsp(
    userInfo: Option[UserInfo] = None,
    roomInfo: Option[RoomInfo] = None,
    errCode: Int = 0,
    msg: String = "ok"
  ) extends Response //fixme url，userName

  case class RoomInfo(
    roomId: Long,
    roomName: String,
    roomDes: String,
    userId: Long,  //房主id
    userName:String,
    headImgUrl:String,
    coverImgUrl:String,
    var observerNum:Int,
    var like:Int,
    var mpd: Option[String] = None,
    var rtmp: Option[String] = None
  )

  /*同一个房间的组员信息*/
  case class MemberInfo(
    userId: Long,
    userName: String,
    liveId: String
  )


}
