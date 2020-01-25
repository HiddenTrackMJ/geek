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
    liveUrl: String,
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

  case class StopLiveReq(
    roomId: Long
  )

  case class GetRoomListReq() extends Request

  case class GetRoomListRsp(
    roomList: List[RoomData],
    errCode: Int = 0,
    msg: String = "ok"
  ) extends Response

  /*common*/
  case class RoomUserInfo(
    roomName: String,
    des: String
  )

  case class ModifyRoomInfo(
    roomName: Option[String] = None,
    des: Option[String] = None,
    peopleNum: Option[Int] = None
  )

  case class RoomData(
    url: String,
    roomId: Long,
    roomInfo: RoomUserInfo
  )


  case class RtmpInfo(
    serverUrl: String,
    liveCode: List[String]
  )







}
