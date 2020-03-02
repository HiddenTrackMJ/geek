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
    var isHost:Option[Boolean] = Some(false), //是否是房主，组员和房主的权限不同
    var isMic:Option[Boolean] = Some(true), //当前用户是否开了声音
    var isVideo:Option[Boolean] = Some(true), //当前用户是否开了摄像头
    var isAllow:Option[Boolean] = Some(false), //当前用户是否是发言人（发言模式下）
    var position:Int = 0, //0表示凸显的位置，1，2，3，4分别对应右侧的4个位置
  )

  case class CommentInfo(
    userId:Long,
    userName:String,
    headImgUrl:String,
    content:String,
    time:Long
  )
  case class UserInfoDetail(
                       userId: Long,
                       userName: String,
                       avatar:Option[String],
                       gender:Option[Int],
                       age:Option[Int],
                       address:Option[String]
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

  case class addInviteeReq(
                                   inviterId: Long,
                                   roomId:Long,
                                   inviteeName: String,
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

  case class InviterAndInviteeDetail(
                      roomId:String,
                      fileList:List[String],
                    )
  case class InviterAndInviteeDetailRsp(
                                         list: List[InviterAndInviteeDetail],
                                         errCode: Int = 0,
                                         msg: String = "Ok"
                                 ) extends Response


  object ModeStatus {
    val FREE = 0 //自由发言
    val ASK = 1 //申请发言
  }

  case class RoomInfo(
    roomId: Long,
    var roomName: String,
    roomDes: String,
    userId: Long,  //房主id
    userName:String,
    headImgUrl:String = "",
    coverImgUrl:String = "",
    var observerNum:Int,
    var rtmp: Option[String] = None,
    var userList:List[UserInfo] = Nil, //房间里面的所有人信息
    var modeStatus:Int = ModeStatus.FREE, //当前发言状态
  )

  case class MeetingInfo(
    name:String,
    id:String, //会议号
    time:Long
  )

  /*同一个房间的组员信息，不要使用这个*/
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


  /**
    * 获取，改变用户详细信息（不包含头像）接口
    **/
  case class GetUserReq(
                        userId: Long
                      ) extends Request

  case class GetUserRsp(
                      userInfo: Option[UserInfoDetail]=None,
                      errCode: Int = 0,
                      msg: String = "Ok"
                      ) extends Request

  case class UpdateUserReq(
                            userId: Long,
                            userName: String,
                            //                            avatar:String,
                            gender:Int,
                            age:Int,
                            address:String
                          ) extends Request

  case class CheckInviteeReq(
                            inviteeId:Long,
                            fileName:String,
                          ) extends Request

  /**
    * 单独改变头像数据库接口
    **/

  case class UpdateAvatarReq(
                         userId: Long,
                         Avatar: String
                       ) extends Request

  /**
    * 用户评论接口
    **/
  case class GetCommentReq(
                          roomId:Long,
                         filename: String,
                       ) extends Request

  case class Comment(
                    commentId:Long,
                    userId:Long,
                    invitation:Long,
                    invitationAvatar:Option[String],
                    invitationName:String,
                    commentContent:String,
                    )
  case class GetCommentRsp(
                            roomId:Option[List[Comment]],
                            errCode: Int = 0,
                            msg: String = "Ok"
                          ) extends Request

  case class addCommentReq(
                            fileName:String,
                            userId:Long,
                            commentContent:String,
                          ) extends Request
  case class delCommentReq(
                            roomId:Long,
                          ) extends Request
}
