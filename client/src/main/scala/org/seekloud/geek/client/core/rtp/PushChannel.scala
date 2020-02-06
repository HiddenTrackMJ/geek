package org.seekloud.geek.client.core.rtp

import java.net.InetSocketAddress
import org.seekloud.geek.client.utils.RtpUtil._
import org.slf4j.LoggerFactory

/**
  * User: TangYaruo
  * Date: 2019/8/20
  * Time: 21:55
  */
class PushChannel {

  private val log = LoggerFactory.getLogger(this.getClass)

  /*PUSH*/
  val serverPushAddr = new InetSocketAddress(rtpServerHost, rtpServerPushPort)

}
