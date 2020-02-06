package org.seekloud.geek.client.core.rtp

import java.nio.ByteBuffer

/**
  * User: TangYaruo
  * Date: 2019/7/21
  * Time: 20:58
  */
object TsPacket {

  val maxTsNum = 7 //受MTU限制
  val tsPacketSize = 188
  val tsHeadFirst: Byte = 0x47.toByte

  val tsBuf: List[ByteBuffer] = (0 until maxTsNum).map { i =>
    ByteBuffer.allocate(188 * 1)
  }.toList

}
