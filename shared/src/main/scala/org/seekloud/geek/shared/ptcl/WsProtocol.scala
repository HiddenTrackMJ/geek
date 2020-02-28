package org.seekloud.geek.shared.ptcl

import org.seekloud.geek.shared.ptcl.CommonInfo.{AudienceInfo, LiveInfo, RoomInfo, UserDes}
import org.seekloud.geek.shared.ptcl.RoomProtocol.{ModifyRoomInfo, RoomData, RoomUserInfo, RtmpInfo, UserPushInfo}

/**
 * Author: Jason
 * Date: 2020/2/8
 * Time: 16:26
 */
object WsProtocol  {

  sealed trait WsMsgFront //前端发的消息

  sealed trait WsMsgManager //后端发的消息

  sealed trait WsMsgClient extends WsMsgFront//client 发的消息

  sealed trait WsMsgRm extends WsMsgManager// backend 发的消息

  case object CompleteMsgClient extends WsMsgFront

  case class FailMsgClient(ex: Exception) extends WsMsgFront

  case object CompleteMsgRm extends WsMsgManager

  case class FailMsgRm(ex: Exception) extends WsMsgManager

  case class Wrap(ws: Array[Byte]) extends WsMsgRm

  case class TextMsg(msg: String) extends WsMsgRm

  case object DecodeError extends WsMsgRm

  case class Test(msg: String) extends WsMsgClient

  /**
   *  通用
   */
  case class GetUserInfoReq(
    userId: Long
  ) extends WsMsgClient

  case class GetUserInfoRsp(
    roomData: Option[RoomData],
    rtmpInfo: Option[RtmpInfo],
    errCode: Int = 0,
    msg: String = "ok"
  ) extends WsMsgRm

  case class GetRoomListReq() extends WsMsgClient

  case class GetRoomListRsp(
    roomList: List[RoomData],
    errCode: Int = 0,
    msg: String = "ok"
  ) extends WsMsgRm

  case class CreateRoomReq(
    userId: Long,
    info: RoomUserInfo
  ) extends WsMsgClient

  case class CreateRoomRsp(
    roomId: Long,
    liveCode: String,
    errCode: Int = 0,
    msg: String = "ok"
  ) extends WsMsgRm


  /**
   *
   * 主播端
   *
   **/

  /*client发送*/
  sealed trait WsMsgHost extends WsMsgClient


  /*roomManager发送*/
  sealed trait WsMsgRm2Host extends WsMsgRm

  /*心跳包*/

  case object PingPackage extends WsMsgClient with WsMsgRm

  case class HeatBeat(ts: Long) extends WsMsgRm

  case object AccountSealed extends WsMsgRm// 被封号

  case object NoUser extends WsMsgRm

  case object NoAuthor extends WsMsgRm

  //fixme url
  case class UpdateAudienceInfo(AudienceList: List[UserDes]) extends WsMsgRm   //当前房间内所有观众的id和昵称,新加入--join--true

  case class ReFleshRoomInfo(roomInfo: RoomInfo) extends WsMsgRm
  /*申请直播*/


//  case class StartLiveReq(
//    userId: Long,
//    token: String,
//    clientType: Int
//  ) extends WsMsgHost
//
//  case class StartLiveRsp(
//    liveInfo: Option[LiveInfo] = None,
//    errCode: Int = 0,
//    msg: String = "ok"
//  ) extends WsMsgRm2Host

  case class UpdateRoomInfoReq(
    roomId: Long,
    roomInfo: ModifyRoomInfo
  ) extends WsMsgHost

  case class ChangePossessionReq(
    roomId: Long,
    userId: Long
  ) extends WsMsgHost

  case class ChangePossessionRsp(
    userId: Long,
    userName: String
  ) extends WsMsgRm

  case class ChangeErrorRsp(msg: String) extends WsMsgRm2Host


  case class StartLiveReq(
    roomId: Long
  ) extends WsMsgHost

  case class StartLiveRsp(
    rtmp: RtmpInfo,
    userLiveCodeMap: Map[String, Long],
    selfCode: String,
    errCode: Int = 0,
    msg: String = "ok"
  ) extends WsMsgRm2Host

  case class StopLiveReq(
    roomId: Long
  ) extends WsMsgHost

  case class StopLiveRsp(
    roomId: Long,
    errCode: Int = 0,
    msg: String = "ok"
  ) extends WsMsgRm

  case class InviteReq(
    roomId: Long,
    userId: Long
  ) extends WsMsgHost

  case class InviteRsp(
    rtmp: Option[UserPushInfo] = None,
    errCode: Int = 0,
    msg: String = "ok"
  ) extends WsMsgRm

  case class KickOffReq(
    roomId: Long,
    userId: Long
  ) extends WsMsgHost

  case class KickOffRsp(
    errCode: Int = 0,
    msg: String = "ok"
  ) extends WsMsgRm

  //当前用户请求发言或者关闭发言，如果status是true，后端收到后转发给主持人，是false后端直接处理后发AppointRsp消息
  case class Appoint4ClientReq(
    roomId: Long,
    userId: Long,
    userName: String, //携带名字方便显示
    status: Boolean //true 为请求发言，false为请求关闭发言
  )extends WsMsgHost with WsMsgRm


  //这个消息是给主持人使用的，主持人直接指定让谁成为发言人,取消谁的发言人身份
  case class AppointReq(
    roomId: Long,
    userId: Long,
    status: Boolean = false
  ) extends WsMsgHost

  //主持人对请求发言消息的回复，用户请求关闭发言消息，后端直接处理就可以了
  case class Appoint4HostReplyReq(
    status: Boolean, //同意为true，拒绝为false
    roomId: Long,
    userId: Long
  )extends WsMsgHost

  //后端给所有用户发送该用户的发言状态
  case class AppointRsp(
    userId: Long,
    userName: String,
    status: Boolean = false, //true为请求发言成功，false为被拒绝或者请求关闭发言成功
    msg: String = "ok",
    errCode: Int = 0,
  ) extends WsMsgRm

  case class AppointReq(
    roomId: Long,
    userId: Long
  ) extends WsMsgHost

  case class AppointRsp(
    userId: Long,
    userName: String,
    errCode: Int = 0,
    msg: String = "ok"
  ) extends WsMsgRm

  case class ShieldReq(
    isForced: Boolean, //为true是被主持人屏蔽的，为false是主动屏蔽的
    roomId: Long,
    userId: Long,
    isImage: Boolean,
    isAudio: Boolean
  ) extends WsMsgClient

  case class ShieldRsp(
    isForced: Boolean,
    userId: Long,
    userName: String,
    isImage: Boolean,
    isAudio: Boolean,
    errCode: Int = 0,
    msg: String = "ok"
  ) extends WsMsgRm



//  val StartLiveRefused: StartLiveRsp = StartLiveRsp(errCode = 200001, msg = "start live refused.")
//  val StartLiveRefused4Seal: StartLiveRsp = StartLiveRsp(errCode = 200001, msg = "start live refused.account has been sealed")
//  val StartLiveRefused4LiveInfoError: StartLiveRsp = StartLiveRsp(errCode = 200001, msg = "start live refused because of getting live info from distributor error.")


  /*修改房间信息*/

  case class ModifyRoomInfo(
    roomName: Option[String] = None,
    roomDes: Option[String] = None
  ) extends WsMsgHost

  case class ModifyRoomRsp(errCode: Int = 0, msg: String = "ok") extends WsMsgRm2Host

  val ModifyRoomError: ModifyRoomRsp = ModifyRoomRsp(errCode = 200010, msg = "modify room error.")


  /*设置直播内容*/

  case class ChangeLiveMode(
    isJoinOpen: Option[Boolean] = None, //是否开启连线
    aiMode: Option[Int] = None, //是否开启人脸识别
    screenLayout: Option[Int] = None //调整画面布局（对等窗口/大小窗口）
  ) extends WsMsgHost

  case class ChangeModeRsp(errCode: Int = 0, msg: String = "ok") extends WsMsgRm2Host

  val ChangeModeError: ChangeModeRsp = ChangeModeRsp(errCode = 200020, msg = "change live mode error.")


  /*连线控制*/

  case class AudienceJoin(userId: Long, userName: String, clientType: Int) extends WsMsgRm2Host //申请连线者信息

  case class JoinAccept(roomId: Long, userId: Long, clientType: Int, accept: Boolean) extends WsMsgHost //审批某个用户连线请求

  case class AudienceJoinRsp(
    joinInfo: Option[AudienceInfo] = None, //连线者信息
    errCode: Int = 0,
    msg: String = "ok"
  ) extends WsMsgRm2Host //拒绝成功不发joinInfo，仅发送默认状态信息

  val AudienceJoinError: AudienceJoinRsp = AudienceJoinRsp(errCode = 400020, msg = "audience join error")

  val NoHostLiveInfoError: AudienceJoinRsp = AudienceJoinRsp(errCode = 400030, msg = "no liveInfo")

  case class HostShutJoin(roomId: Long) extends WsMsgHost //断开与观众连线请求//fixme 多人连线需要修改消息添加userId

  case class AudienceDisconnect(hostLiveId: String) extends WsMsgRm2Host //观众断开连线通知（同时rm断开与观众ws）

  case class HostStopPushStream(roomId: Long) extends WsMsgHost //房主停止推流


  /**
   *
   * 观众端
   *
   **/


  /*client发送*/
  sealed trait WsMsgAudience extends WsMsgClient

  /*room manager发送*/
  sealed trait WsMsgRm2Audience extends WsMsgRm


  /*申请连线*/
  case class JoinReq(userId: Long, roomId: Long, clientType: Int) extends WsMsgAudience

  case class JoinRsp(
    hostLiveId: Option[String] = None, //房主liveId
    joinInfo: Option[LiveInfo] = None, //连线者live信息
    errCode: Int = 0,
    msg: String = "ok"
  ) extends WsMsgRm2Audience

  case class Join4AllRsp(
    mixLiveId: Option[String] = None,
    errCode: Int = 0,
    msg: String = "ok",
  ) extends WsMsgRm2Audience

  case class StartLive4ClientReq(
    roomId: Long,
    userId: Long
  ) extends WsMsgAudience

  case class StartLive4ClientRsp(
    rtmp: Option[RtmpInfo] = None,
    userLiveCodeMap: Map[String, Long],
    selfCode: String,
    errCode: Int = 0,
    msg: String = "ok"
  ) extends WsMsgRm2Audience

  case class StopLive4ClientReq(
    roomId: Long,
    userId: Long
  )extends WsMsgAudience

  case class StopLive4ClientRsp(
    roomId: Long,
    userId: Long,
    errCode: Int = 0,
    msg: String = "ok"
  ) extends WsMsgRm

  case class JoinRoomReq(
    roomId: Long,
    userId: Long
  )extends WsMsgAudience

  case class JoinRoomRsp(
    rtmp: Option[UserPushInfo] = None,
    errCode: Int = 0,
    msg: String = "ok"
  ) extends WsMsgRm2Audience


  val StartLive4ClientFail = StartLive4ClientRsp(None, Map(),"", errCode = 2000001, msg = "StartLive4Client fail.")
  /*
  点赞
   */
//  case class LikeRoom(userId: Long, roomId: Long, upDown:Int) extends WsMsgClient
//
//  case class LikeRoomRsp(
//    errCode: Int = 0,
//    msg: String = "ok"
//  ) extends WsMsgRm2Audience

  case class JudgeLike(userId: Long, roomId:Long) extends WsMsgClient


  case class JudgeLikeRsp(
    like:Boolean,                 //true->已点过赞  false->未点过赞
    errCode: Int = 0,
    msg: String = "ok"
  ) extends WsMsgRm2Audience

  val JoinInvalid: JoinRsp = JoinRsp(errCode = 300001, msg = "join not open.") //房主未开通连线功能

  val JoinInternalError: JoinRsp = JoinRsp(errCode = 300001, msg = "internal error") //房主未开通连线功能
  val JoinAccountError: JoinRsp = JoinRsp(errCode = 300001, msg = "userId error") //房主未开通连线功能

  val JoinRefused: JoinRsp = JoinRsp(errCode = 300002, msg = "host refuse your request.") //房主拒绝连线申请

  case class AudienceShutJoin(roomId: Long) extends WsMsgAudience //断开与房主的连线请求

  //fixme 切断与某个用户的连线，增加userId，拓展多个用户连线的情况
  case class AudienceShutJoinPlus(userId:Long) extends WsMsgAudience //断开与房主的连线请求

  case class HostDisconnect(hostLiveId: String) extends WsMsgRm2Audience //房主断开连线通知 (之后rm断开ws连接)

  case object HostCloseRoom extends WsMsgRm2Audience //房主关闭房间通知房间所有用户
  case class HostCloseRoom() extends WsMsgRm2Audience //房主关闭房间通知房间所有用户，class方便后台一些代码的处理


  case object BanOnAnchor extends WsMsgRm2Host//禁播消息

  /**
   * 所有用户
   * 留言
   **/

  case class Comment(
    userId: Long,
    roomId: Long,
    comment: String,
    color:String = "#FFFFFF",
    extension: Option[String] = None
  ) extends WsMsgClient

  case class RcvComment(
    userId: Long,
    userName: String,
    comment: String,
    color:String = "#FFFFFF",
    extension: Option[String] = None
  ) extends WsMsgRm

  case class CommentError(
    msg: String
  ) extends WsMsgRm

  case class UpdateRoomInfo2Client(
    roomName: String,
    roomDec: String
  ) extends WsMsgRm2Audience

  case object HostStopPushStream2Client extends WsMsgRm2Audience

  case class GetRoomInfoReq(
    roomId: Long
  ) extends WsMsgClient

  case class GetRoomInfoRsp(
    info: CommonProtocol.RoomInfo
  ) extends WsMsgRm


}
