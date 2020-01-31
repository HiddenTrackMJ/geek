package org.seekloud.geek.shared.ptcl


import org.seekloud.geek.shared.ptcl.{Request,_}
import CommonInfo._
/**
  * User: Arrow
  * Date: 2019/7/15
  * Time: 18:00
  */
object CommonProtocol {

  //TODO 具体错误情况和错误码由room manager细化

  /**
    * 注册 & 登录 & 查询房间
    *
    * POST
    *
    **/
  case class SignUp(
    email: String,
    userName: String,
    password: String,
    url: String //重定向url
  ) extends Request

  case class SignIn(
    userName: String,
    password: String
  ) extends Request

  case class SignInByMail(
    email: String,
    password: String
  ) extends Request

  case class SearchRoom(roomId: Long) extends Request

  case class SignUpRsp(
    //    code:String,
    errCode: Int = 0,
    msg: String = "ok"
  ) extends CommonRsp

  case class SignInRsp(
    userInfo: Option[UserInfo] = None,
    roomInfo: Option[RoomInfo] = None,
    errCode: Int = 0,
    msg: String = "ok"
  ) extends CommonRsp //fixme url，userName

  case class RegisterSuccessRsp(
    url: String, //重定向URL
    errCode: Int = 0,
    msg: String = "ok"
  ) extends CommonRsp

  case class RegisterInfo(id: Long, email: String, name: String, password: String)

  case class UserInfoRsp(info: RegisterInfo,
    errCode: Int = 0,
    msg: String = "ok"
  ) extends CommonRsp

  case class SearchRoomReq(
    userId: Option[Long],
    roomId: Long
  ) extends Request

  case class SearchRoomRsp(
    roomInfo: Option[RoomInfo],
    errCode: Int = 0,
    msg: String = "ok"
  ) extends CommonRsp


  /**
    * 录像
    */

  case class GetRecordListRsp(
    recordNum: Int,
    recordInfo: List[RecordInfo],
    errCode: Int = 0,
    msg: String = "ok"
  ) extends CommonRsp

  case class SearchRecord(
    roomId: Long,
    startTime: Long,
    inTime:Long,//用户开始观看视频的时间
    userIdOpt:Option[Long] = None
  ) extends Request

  case class SearchRecordRsp(
    url: String = "",
    recordInfo: RecordInfo,
    errCode: Int = 0,
    msg: String = "ok"
  ) extends CommonRsp

  /*case class GetAuthorRecordListReq(
    roomId: Long,
    sortBy: String,
    pageNum: Int,
    pageSize: Int
  ) extends Request*/

  case class GetAuthorRecordListRsp(
    recordNum: Int,
    recordInfo: List[RecordInfo],
    errCode: Int = 0,
    msg: String = "ok"
  ) extends CommonRsp

  case class AuthorDeleteRecordReq(
    recordId: Long
  ) extends Request

  case class AddRecordAddrReq(
    recordId: Long,
    recordAddr: String
  ) extends Request

  /**
    *
    * 头像 昵称
    */

  case class ImgChangeRsp(
    url: String,
    errCode: Int = 0,
    msg: String = "ok"
  ) extends CommonRsp

  val ImgChangeRspDecodeError = ImgChangeRsp("", 100101, "error:decode error")
  val ImgChangeRspInternalError = ImgChangeRsp("", 1001012, "error:internal error")

  val SignInternalError = SignInRsp(errCode = 100001, msg = "error : internal error.")
  val NoUserError = SignInRsp(errCode = 100002, msg = "error : no user.")
  val WrongPwError = SignInRsp(errCode = 100003, msg = "error : wrong password.")
  val TokenError = SignInRsp(errCode = 100007, msg = "error : token error.")

  val SignUpInternalError = SignUpRsp(errCode = 100001, msg = "error : internal error.")
  val UserExistError = SignUpRsp(100004, msg = "error: user already exist")
  def SearchRoomError(errCode: Int = 100005, msg: String = "error: search room error，processor left") = SearchRoomRsp(None, errCode, msg)
  val SearchRoomError4RoomId = SearchRoomRsp(None, 100008, msg = "error: roomId error")
  val SearchRoomError4ProcessorDead = SearchRoomRsp(None, 100006, msg = "error: processor failed")

  /**
    *
    * 建立websocket
    *
    * post
    */
  case class SetupWsReq(
    userId: Long,
    token: String
  ) extends Request

  case class SetupWsRsp(
    errCode: Int = 0,
    msg: String = "ok"
  ) extends CommonRsp

  val SetupWsError = SetupWsRsp(100007, msg = "error: setupWs error")

  /**
    *
    * 获取房间列表
    *
    * GET
    *
    **/

  case class RoomListRsp(
    roomList: Option[List[RoomInfo]] = None,
    errCode: Int = 0,
    msg: String = "ok"
  ) extends CommonRsp //fixme 添加userName,url,观众数量

  val GetRoomListError = RoomListRsp(errCode = 100011, msg = "get room list error.")

  /** 临时用户申请userId和token接口 */
  final case class GetTemporaryUserRsp(
    userInfoOpt: Option[UserInfo],
    errCode: Int = 0,
    msg: String = "ok"
  ) extends CommonRsp

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
    * 获取liveinfo
    **/
  case class GetLiveInfoRsp(
    liveInfo: LiveInfo,
    errCode: Int = 0,
    msg: String = "ok"
  ) extends CommonRsp

  case class GetLiveInfoRsp4RM(
    liveInfo: Option[LiveInfo],
    errCode: Int = 0,
    msg: String = "ok"
  ) extends CommonRsp

  case class ClientInfo(
                  win:List[String],
                  mac:List[String],
                  )

  case class ListClientFiles(
                            data:Option[ClientInfo],
                            errCode:Int = 0,
                            msg:String = "ok"
                            )

}
