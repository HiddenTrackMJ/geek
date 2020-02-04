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
    var rtmp: Option[String] = None
  )

  /**
    * 根据userId,token查询roomInfo接口
    **/
  final case class GetRoomInfoReq(
                                   userId: Long,
                                   token: String
                                 )

  final case class RoomInfoRsp(
                                roomInfoOpt: Option[RoomInfo],
                                errCode: Int = 0,
                                msg: String = "ok"
                              )

}
