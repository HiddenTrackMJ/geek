package org.seekloud.geek.client.utils

import java.nio.ByteBuffer

import org.seekloud.geek.client.common.AppSettings

import scala.collection.mutable

/**
  * User: TangYaruo
  * Date: 2019/7/21
  * Time: 14:42
  * Description: RTP 拉流和推流过程中一些参数和工具方法
  */
object RtpUtil {

  var clientHostQueue: mutable.Queue[String] = mutable.Queue.empty

  val clientHost: String = if (NetUtil.getLocalIpv4Address.nonEmpty) {NetUtil.getLocalIpv4Address.head} else AppSettings.host//AppSettings.host
  val clientPushPort: Int = AppSettings.pushPort
  val clientPullPort: Int = AppSettings.pullPort

  val rtpServerHost: String = AppSettings.rsHostName
  val rtpServerPushPort: Int = AppSettings.rsPushPort
  val rtpServerPullPort: Int = AppSettings.rsPullPort

  def initIpPool(): Unit = {
    clientHostQueue.enqueue(AppSettings.host)
    if (NetUtil.getLocalIpv4Address.nonEmpty) NetUtil.getLocalIpv4Address.foreach(clientHostQueue.enqueue(_))
    clientHostQueue = clientHostQueue.distinct
//    println(clientHostQueue)
  }

  initIpPool()

  object PAYLOAD_TYPE {
    val STREAM: Byte = AppSettings.rsStreamPT.toByte
    val PUSH_REQ: Byte = AppSettings.rsPushReqPT.toByte
    val PUSH_RSP: Byte = AppSettings.rsPushRspPT.toByte
    val PUSH_REFUSE: Byte = AppSettings.rsPushRefuse.toByte
    val PULL_REQ: Byte = AppSettings.rsPullReqPT.toByte
    val PULL_RSP: Byte = AppSettings.rsPullRspPT.toByte
  }

  case class RtpHeader(
    firstByte: Byte = 0x80.toByte, // V | P | X | CC
    ptByte: Byte, // m | payload type
    sn: Short, // sequence number(16 bit)
    timestamp: Int, // 32bit
    ssrc: Int, //32bit
  ) {

    def toByteArray: Array[Byte] = {
      val targetArray = new Array[Byte](12)
      val header = ByteBuffer.allocate(12)
      header.put(firstByte)
      header.put(ptByte)
      header.putShort(sn)
      header.putInt(timestamp)
      header.putInt(ssrc)
      header.flip()
      header.get(targetArray)
      targetArray
    }
  }

  def headerDecode(array: Array[Byte]): RtpHeader = {
    println(s"array: ${array.toList.map(_.toHexString)}")
    val firstByte = array(0)
    val ptByte = array(1)
    val sn = ((array(2) & 0xff) << 8 | (array(3) & 0xff)).toShort
    val timestamp = (array(4) & 0xff) << 24 | (array(5) & 0xff) << 16 | (array(6) & 0xff) << 8 | (array(7) & 0xff)
    val ssrc = (array(8) & 0xff) << 24 | (array(9) & 0xff) << 16 | (array(10) & 0xff) << 8 | (array(11) & 0xff)
    RtpHeader(firstByte, ptByte, sn, timestamp, ssrc)
  }


  case class UdpHeader(
    payloadType: Byte,
    sn: Int,
    ssrc: Int
  ) {

    def toByteArray: Array[Byte] = {
      val targetArray = new Array[Byte](9)
      val header = ByteBuffer.allocate(9)
      header.put(payloadType)
      header.putInt(sn)
      header.putInt(ssrc)
      header.flip()
      header.get(targetArray)
      targetArray
    }

  }

  def udpHeaderDecode(array: Array[Byte]): UdpHeader = {
    val headerBuffer = ByteBuffer.wrap(array)
    val payloadType = headerBuffer.get()
    val sn = headerBuffer.getInt()
    val ssrc = headerBuffer.getInt()
    UdpHeader(payloadType, sn, ssrc)
  }

  def main1(args: Array[String]): Unit = {
    println(clientHostQueue)
  }


}
