package org.seekloud.geek.player.util

import org.seekloud.geek.player.Boot.executor
import org.slf4j.LoggerFactory

import scala.concurrent.Future

/**
  * User: TangYaruo
  * Date: 2019/9/23
  * Time: 14:23
  */
object RecordUtil extends HttpUtil with CirceSupport {

  private val log = LoggerFactory.getLogger(this.getClass)


  def getRecordContent(url: String): Future[Either[Throwable, Array[Byte]]] = {
    val methodName = "getRecordContent"
    getFileContent(methodName, url, Nil, needLogRsp = false)
  }


  def main1(args: Array[String]): Unit = {
    val url = "https://media.seekloud.org:50443/geek/distributor/getRecord/1000008/1569213160185/record.mp4"

    getRecordContent(url).map {
      case Right(rst) =>
        log.debug(s"rst: ${rst.length}")
      case Left(error) =>
        log.error(s"get record error: $error")

    }
  }



}
