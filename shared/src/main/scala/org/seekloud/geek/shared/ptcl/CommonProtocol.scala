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
    headImgUrl:String,
    var pushStream:Option[String] = None,//推流的地址
    var pullStream:Option[String] = None,//拉流的地址
    var isHost:Option[Boolean] = Some(false) //是否是房主，组员和房主的权限不同
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

  case class InvitationReq(
                     inviterId: Long,
                   ) extends Request

  case class InviterAndInviteeReq(
                            inviterId: Long,
                            inviteeId: Long,
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
  ) extends Response

  case class Inviter(
                      inviterName:String,
                      inviterId:Long
                    )

  case class InvitationRsp(
                         list: Option[List[Inviter]],
                         errCode: Int = 0,
                         msg: String = "Ok"
                       ) extends Response


  case class RoomInfo(
    roomId: Long,
    roomName: String,
    roomDes: String,
    userId: Long,  //房主id
    userName:String,
    headImgUrl:String = "",
    coverImgUrl:String = "",
    var observerNum:Int,
    var rtmp: Option[String] = None
  )

  case class MeetingInfo(
    name:String,
    id:String, //会议号
    time:Long
  )

  /*同一个房间的组员信息*/
  case class MemberInfo(
    users:List[UserInfo] = Nil
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
