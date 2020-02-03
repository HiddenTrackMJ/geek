package org.seekloud.geek.shared.rtp

/**
  * Created by haoshuhan on 2019/7/23.
  */
object Protocol {
  trait Command
  case class AuthRsp(liveId: String, ifSuccess: Boolean) extends Command
  case class PushStreamError(liveId: String, errCode: Int, msg: String) extends Command
  case class PullStreamData(liveId: String, data: Array[Byte]) extends Command
  case class TestPullStreamData(liveId: String, header: Header) extends Command
  case class PullStreamReqSuccess(liveIds: List[String]) extends Command
  case class NoStream(liveIds: List[String]) extends Command
  case object PullStreamPacketLoss extends Command
  case object PullStreamReady extends Command
  case class StreamStop(liveId: String) extends Command
  case class Header(payloadType: Int, m: Int, seq: Int, ssrc: Int, timestamp: Long)
  case class Data(header: Header, body: Array[Byte])

}
