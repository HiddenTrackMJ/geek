package org.seekloud.geek.client.utils

import java.io.File

import org.seekloud.geek.client.Boot.executor
import org.seekloud.geek.client.common.Routes
import org.seekloud.geek.shared.ptcl.CommonProtocol._
import org.seekloud.geek.shared.ptcl.RoomProtocol.{CreateRoomReq, CreateRoomRsp, RoomUserInfo}
import org.slf4j.LoggerFactory

import scala.concurrent.Future

/**
  * User: xgy
  * Date: 2020/1/31
  * Time: 21:24
  **/

object RoomClient extends HttpUtil {
  import io.circe.generic.auto._
  import io.circe.parser.decode
  import io.circe.syntax._

  private val log = LoggerFactory.getLogger(this.getClass)

  def getRoomInfo(userId: Long, token: String): Future[Either[Throwable, RoomInfoRsp]] = {

    val methodName = "getRoomInfo"
    val url = Routes.getRoomInfo

    val data = GetRoomInfoReq(userId, token).asJson.noSpaces
    postJsonRequestSend(methodName, url, Nil, data, needLogRsp = false).map {
      case Right(jsonStr) =>
        decode[RoomInfoRsp](jsonStr)
      case Left(error) =>
        log.error(s"user-$userId getRoomInfo error: $error")
        Left(error)
    }
  }

  def createRoom(userId: Long, info: RoomUserInfo): Future[Either[Throwable, CreateRoomRsp]] = {

    val methodName = "createRoom"
    val url = Routes.createRoom

    val data = CreateRoomReq(userId, info).asJson.noSpaces
    postJsonRequestSend(methodName, url, Nil, data, needLogRsp = false).map {
      case Right(jsonStr) =>
        decode[CreateRoomRsp](jsonStr)
      case Left(error) =>
        log.error(s"user-$userId createRoom error: $error")
        Left(error)
    }
  }


}
