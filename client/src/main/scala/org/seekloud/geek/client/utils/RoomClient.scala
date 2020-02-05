package org.seekloud.geek.client.utils

import java.io.File

import org.seekloud.geek.client.Boot.executor
import org.seekloud.geek.client.common.Routes
import org.seekloud.geek.shared.ptcl.CommonProtocol._
import org.seekloud.geek.shared.ptcl.RoomProtocol.{CreateRoomReq, CreateRoomRsp, RoomUserInfo, StartLive4ClientReq, StartLive4ClientRsp, StartLiveReq, StartLiveRsp, StopLiveReq}
import org.seekloud.geek.shared.ptcl.SuccessRsp
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

  def startLive(roomId: Long): Future[Either[Throwable, StartLiveRsp]] = {

    val methodName = "startLive"
    val url = Routes.startLive

    val data = StartLiveReq(roomId).asJson.noSpaces
    postJsonRequestSend(methodName, url, Nil, data, needLogRsp = false).map {
      case Right(jsonStr) =>
        decode[StartLiveRsp](jsonStr)
      case Left(error) =>
        log.error(s"room-$roomId startLive error: $error")
        Left(error)
    }
  }

  def startLive4Client(userId: Long, roomId: Long): Future[Either[Throwable, StartLive4ClientRsp]] = {

    val methodName = "startLive4Client"
    val url = Routes.startLive4Client

    val data = StartLive4ClientReq(roomId, userId).asJson.noSpaces
    postJsonRequestSend(methodName, url, Nil, data, needLogRsp = false).map {
      case Right(jsonStr) =>
        decode[StartLive4ClientRsp](jsonStr)
      case Left(error) =>
        log.error(s"user-$userId createRoom error: $error")
        Left(error)
    }
  }

  def stopLive(roomId: Long): Future[Either[Throwable, SuccessRsp]] = {

    val methodName = "stopLive"
    val url = Routes.stopLive

    val data = StopLiveReq(roomId).asJson.noSpaces
    postJsonRequestSend(methodName, url, Nil, data, needLogRsp = false).map {
      case Right(jsonStr) =>
        decode[SuccessRsp](jsonStr)
      case Left(error) =>
        log.error(s"room-$roomId startLive error: $error")
        Left(error)
    }
  }

}
