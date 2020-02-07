package org.seekloud.geek.shared.client2Manager.websocket


/**
  * User: Arrow
  * Date: 2019/7/16
  * Time: 17:03
  */
object AuthProtocol {

  sealed trait WsMsgFront

  sealed trait WsMsgManager

  sealed trait WsMsgClient extends WsMsgFront

  sealed trait WsMsgRm extends WsMsgManager

  case object CompleteMsgClient extends WsMsgFront

  case class FailMsgClient(ex: Exception) extends WsMsgFront

  case object CompleteMsgRm extends WsMsgManager

  case class FailMsgRm(ex: Exception) extends WsMsgManager

  case class Wrap(ws: Array[Byte]) extends WsMsgRm


  case class TextMsg(msg: String) extends WsMsgRm

  case object DecodeError extends WsMsgRm

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


}
