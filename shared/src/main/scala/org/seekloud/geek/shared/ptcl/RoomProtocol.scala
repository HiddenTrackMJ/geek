package org.seekloud.geek.shared.ptcl

/**
  * User: TangYaruo
  * Date: 2019/5/23
  * Time: 17:37
  */
object RoomProtocol {

  trait Request

  trait Response {
    val errCode: Int
    val msg: String
  }


  /**
    * 创建直播间相关
    *
    * errCode: 2000001 ~
    *
    * */
  case class CreateRoomReq(
    userId: Long,
    info: RoomUserInfo
  ) extends Request

  case class CreateRoomRsp(
    roomId: Long,
    liveCode: String,
    errCode: Int = 0,
    msg: String = "ok"
  ) extends Response

  val CreateRoomFail = CreateRoomRsp(0L, "", errCode = 2000001, msg = "create room fail.")

  case class GetUserInfoReq(
    userId: Long
  ) extends Request

  case class GetUserInfoRsp(
    roomData: Option[RoomData],
    rtmpInfo: Option[RtmpInfo],
    errCode: Int = 0,
    msg: String = "ok"
  ) extends Response

  case class UpdateRoomInfoReq(
    roomId: Long,
    roomInfo: ModifyRoomInfo
  ) extends Request


  case class StartLiveReq(
    roomId: Long
  )

  case class StartLiveRsp(
    rtmp: RtmpInfo,
    errCode: Int = 0,
    msg: String = "ok"
  ) extends Response

  case class StartLive4ClientReq(
    roomId: Long,
    userId: Long
  )

  case class StartLive4ClientRsp(
    rtmp: Option[RtmpInfo] = None,
    errCode: Int = 0,
    msg: String = "ok"
  ) extends Response

  val StartLive4ClientFail = StartLive4ClientRsp(None, errCode = 2000001, msg = "StartLive4Client fail.")

  case class StopLiveReq(
    roomId: Long
  )

  case class StopLive4ClientReq(
    roomId: Long,
    userId: Long
  )

  case class JoinRoomReq(
    roomId: Long,
    userId: Long
  )

  case class JoinRoomRsp(
    rtmp: Option[UserPushInfo] = None,
    errCode: Int = 0,
    msg: String = "ok"
  ) extends Response

  case class InviteReq(
    roomId: Long,
    userId: Long
  )

  case class InviteRsp(
    rtmp: Option[UserPushInfo] = None,
    errCode: Int = 0,
    msg: String = "ok"
  ) extends Response

  case class KickOffReq(
    roomId: Long,
    userId: Long
  )

  case class GetRoomListReq() extends Request

  case class GetRoomSectionListReq(userId:Long) extends Request

  case class GetRoomListRsp(
    roomList: List[RoomData],
    errCode: Int = 0,
    msg: String = "ok"
  ) extends Response

  case class GetRoomSectionListRsp(
                             roomList: List[RoomId],
                             errCode: Int = 0,
                             msg: String = "ok"
                           ) extends Response

  case class RecordData(
    userId: Long,
    roomId: Long,
    timeStamp: Long,
    length: String
  )

  case class GetRecordListReq() extends Request

  case class GetRecordListRsp(
    roomList: List[RecordData],
    errCode: Int = 0,
    msg: String = "ok"
  ) extends Response

  /*common*/
  case class RoomUserInfo(
    userId: Long,
    roomName: String,
    des: String
  )

  case class ModifyRoomInfo(
    roomName: Option[String] = None,
    des: Option[String] = None,
    peopleNum: Option[Int] = None
  )

  case class RoomData(
    userLiveCodeMap: Map[String, Long],
    roomId: Long,
    roomInfo: RoomUserInfo,
    liveOrNot: Boolean
  )

  case class RoomId(roomId:Long)


  case class RtmpInfo(
    serverUrl: String,
    stream: String,
    liveCode: List[String]
  )

  case class UserPushInfo(
    roomUserInfo: RoomUserInfo,
    pushStream: String,
    pullStream: String
  )





}
