package org.seekloud.geek.capture.common

import java.util.concurrent.TimeUnit

import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.{Logger, LoggerFactory}

/**
  * User: TangYaruo
  * Date: 2019/8/27
  * Time: 11:36
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



  val dependenceConfig: Config = config.getConfig("dependence")

  /* video */
  private val videoConfig = dependenceConfig.getConfig("video.config")
  val bit: Int = videoConfig.getInt("bit")
  val imgResolution: String = videoConfig.getString("imgResolution")
  val frameRate: Int = videoConfig.getInt("frameRate")

}
