package org.seekloud.geek.common

import java.util
import java.util.concurrent.TimeUnit

import org.seekloud.geek.utils.SessionSupport.SessionConfig
import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.{Logger, LoggerFactory}



/**
  * User: Taoz
  * Date: 9/4/2015
  * Time: 4:29 PM
  */
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


  val log: Logger = LoggerFactory.getLogger(this.getClass)
  val config: Config = ConfigFactory.parseResources("product.conf").withFallback(ConfigFactory.load())

  val appConfig: Config = config.getConfig("app")
  val version: String = appConfig.getString("version")

  /*server*/
  val serverProtocol: String = appConfig.getString("server.protocol")
  val serverDomain: String = appConfig.getString("server.domain")
  val serverHostName: String = appConfig.getString("server.hostName")
  val serverPort: String = appConfig.getString("server.port")
  val serverUrl: String = appConfig.getString("server.url")
  val host = s"$serverHostName:$serverPort"
  //  val host = serverDomain
  val baseUrl: String = serverProtocol + "://" + host + "/" + serverUrl
  val videoPath: String = appConfig.getString("server.videoPath")

  val httpInterface: String = appConfig.getString("http.interface")
  val httpPort: Int = appConfig.getInt("http.port")

  val authCheck: Boolean = appConfig.getBoolean("authCheck")


  val appSecureMap: Map[String, String] = {
    import collection.JavaConverters._
    val appIds = appConfig.getStringList("client.appIds").asScala
    val secureKeys = appConfig.getStringList("client.secureKeys").asScala
    require(appIds.length == secureKeys.length, "appIdList.length and secureKeys.length not equal.")
    appIds.zip(secureKeys).toMap
  }


  /*rtmp settings*/
  val rtmpConfig: Config = appConfig.getConfig("rtmp")
  val rtmpProtocol: String = rtmpConfig.getString("protocol")
  val rtmpIp: String = rtmpConfig.getString("ip")
  val rtmpPort: Int = rtmpConfig.getInt("port")
  val rtmpScope: String = rtmpConfig.getString("scope")
  val rtmpServer = s"$rtmpProtocol://$rtmpIp:$rtmpPort/$rtmpScope/"

  val serverConfig: Config = appConfig.getConfig("server")
  //  val serverHost: String = serverConfig.getString("host")
  //  val server = s"$serverProtocol://$serverHost/$rtmpScope/"


  val slickConfig: Config = config.getConfig("slick.db")
  val slickUrl: String = slickConfig.getString("url")
  val slickUser: String = slickConfig.getString("user")
  val slickPassword: String = slickConfig.getString("password")
  val slickMaximumPoolSize: Int = slickConfig.getInt("maximumPoolSize")
  val slickConnectTimeout: Int = slickConfig.getInt("connectTimeout")
  val slickIdleTimeout: Int = slickConfig.getInt("idleTimeout")
  val slickMaxLifetime: Int = slickConfig.getInt("maxLifetime")

  val essfMapKeyName = "essfMap"

  val sessionConfig: SessionConfig = {
    val sConf = config.getConfig("session")
    SessionConfig(
      cookieName = sConf.getString("cookie.name"),
      serverSecret = sConf.getString("serverSecret"),
      domain = sConf.getOptionalString("cookie.domain"),
      path = sConf.getOptionalString("cookie.path"),
      secure = sConf.getBoolean("cookie.secure"),
      httpOnly = sConf.getBoolean("cookie.httpOnly"),
      maxAge = sConf.getOptionalDurationSeconds("cookie.maxAge"),
      sessionEncryptData = sConf.getBoolean("encryptData")
    )
  }

  val basicFilePath = appConfig.getString("fileSetting.basicFilePath")
  val pdfFilePath = appConfig.getString("fileSetting.pdfFilePath")
  val convertPdfFilePath = pdfFilePath + "/convert"
  val imgFilePath = appConfig.getString("fileSetting.imgFilePath")
  val tourFilePath = appConfig.getString("fileSetting.tourFilePath")
  val videoFilePath = appConfig.getString("fileSetting.videoFilePath")
  val excelFilePath = appConfig.getString("fileSetting.excelFilePath")
  val statisticPath = appConfig.getString("fileSetting.statisticPath")

  val hestiaConfig = config.getConfig("dependence.hestia")
  val hestiaProtocol = hestiaConfig.getString("protocol")
  val hestiaHost = hestiaConfig.getString("host")
  val hestiaPort = hestiaConfig.getString("port")
  val hestiaDomain = hestiaConfig.getString("domain")
  val hestiaAppId = hestiaConfig.getString("appId")
  val hestiaSecureKey = hestiaConfig.getString("secureKey")

}

