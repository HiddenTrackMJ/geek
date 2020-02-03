package org.seekloud.geek.client.utils

import org.seekloud.geek.player.util.CirceSupport
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.util.{Failure, Success}
import org.seekloud.geek.client.Boot.executor

/**
  * User: TangYaruo
  * Date: 2019/9/11
  * Time: 12:25
  * Description: 获取Hestiat图片
  *
  */
object HestiaClient extends HttpUtil with CirceSupport {

  private val log = LoggerFactory.getLogger(this.getClass)


  def getHestiaImage(url: String): Future[Either[Throwable, Array[Byte]]] = {
    val methodName = "getHestiaImage"
    getImageContent(methodName, url, Nil, needLogRsp = false)
  }


  def main1(args: Array[String]): Unit = {

    val url = "http://pic.neoap.com/hestia/files/image/roomManager/b8116fee146544809733802a2c3a1319.jpg"
    getHestiaImage(url).onComplete {
      case Success(value) =>
        log.info(s"get image success: $value")
      case Failure(exception) =>
        log.error(s"get image error: $exception")
    }
  }


}
