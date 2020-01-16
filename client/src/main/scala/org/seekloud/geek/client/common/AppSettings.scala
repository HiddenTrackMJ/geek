package org.seekloud.geek.client.common

import java.util.concurrent.TimeUnit

import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.LoggerFactory

object AppSettings {

  private implicit class RichConfig(config: Config) {
    val noneValue = "none"

    def getOptionalString(path: String): Option[String] =
      if (config.getAnyRef(path) == noneValue) None
      else Some(config.getString(path))

    def getOptionalLong(path: String): Option[Long] =
      if (config.getAnyRef(path) == noneValue) None
      else Some(config.getLong(path))

    def getOptionalDurationSeconds(path: String): Option[Long] =
      if (config.getAnyRef(path) == noneValue) None
      else Some(config.getDuration(path, TimeUnit.SECONDS))
  }


//  val log = LoggerFactory.getLogger(this.getClass)
  val config = ConfigFactory.parseResources("product.conf").withFallback(ConfigFactory.load())

  val appConfig = config.getConfig("app")


  val host = appConfig.getString("host")
  val pushPort = appConfig.getInt("pushPort")
  val pullPort = appConfig.getInt("pullPort")
  val captureDebug = appConfig.getBoolean("captureDebug")
  val playerDebug = appConfig.getBoolean("playerDebug")
  val needTimestamp = appConfig.getBoolean("needTimestamp")

  val dependenceConfig = config.getConfig("dependence")

  /*roomManager*/
  private val rmConfig = dependenceConfig.getConfig("roomManager.config")

  val rmProtocol = rmConfig.getString("protocol")
  val rmWsProtocol = rmConfig.getString("wsProtocol")
  val rmDomain = rmConfig.getString("domain")
  val rmHostName = rmConfig.getString("hostName")
  val rmPort = rmConfig.getInt("port")
  val rmUrl = rmConfig.getString("url")
//  val d_w = rmConfig.getInt("width")
//  val d_h = rmConfig.getInt("height")

  /*rtpServer*/
  private val rtpConfig = dependenceConfig.getConfig("rtpServer.config")

  val rsHostName = rtpConfig.getString("hostName")
  val rtpServerDst = rtpConfig.getString("rtpServerDst")
  val rsStreamPT = rtpConfig.getInt("streamPT")

  val rsPushPort = rtpConfig.getInt("push.pushPort")
  val rsPushReqPT = rtpConfig.getInt("push.pushReqPT")
  val rsPushRspPT = rtpConfig.getInt("push.pushRspPT")
  val rsPushRefuse = rtpConfig.getInt("push.pushRefuse")

  val rsPullPort = rtpConfig.getInt("pull.pullPort")
  val rsPullReqPT = rtpConfig.getInt("pull.pullReqPT")
  val rsPullRspPT = rtpConfig.getInt("pull.pullRspPT")



}
