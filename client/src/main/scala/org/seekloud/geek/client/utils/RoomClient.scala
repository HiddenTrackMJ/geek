package org.seekloud.geek.client.utils

import java.io.File

import org.seekloud.geek.client.Boot.executor
import org.seekloud.geek.client.common.Routes
import org.seekloud.geek.shared.ptcl.CommonProtocol._
import org.seekloud.geek.shared.ptcl.RoomProtocol.{CreateRoomReq, CreateRoomRsp, GetRoomListReq, GetRoomListRsp, JoinRoomReq, JoinRoomRsp, RoomUserInfo, StartLive4ClientReq, StartLive4ClientRsp, StartLiveReq, StartLiveRsp, StopLive4ClientReq, StopLiveReq}
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


  def createRoom(userId: Long, info: RoomUserInfo): Future[Either[Throwable, CreateRoomRsp]] = {

    val methodName = "createRoom"
    val url = Routes.createRoom

    val data = CreateRoomReq(userId, info).asJson.noSpaces
    postJsonRequestSend(methodName, url, Nil, data, needLogRsp = false).map {
      case Right(jsonStr) =>
        log.info(s"createRomm 返回信息 $jsonStr")
        decode[CreateRoomRsp](jsonStr)
      case Left(error) =>
        log.error(s"user-$userId createRoom error: $error")
        Left(error)
    }
  }

  def joinRoom(roomId: Long, userId: Long): Future[Either[Throwable, JoinRoomRsp]] = {

    val methodName = "joinRoom"
    val url = Routes.joinRoom

    val data =  JoinRoomReq(roomId, userId).asJson.noSpaces
    postJsonRequestSend(methodName, url, Nil, data, needLogRsp = false).map {
      case Right(jsonStr) =>
        decode[JoinRoomRsp](jsonStr)
      case Left(error) =>
        log.error(s"room-$roomId user-$userId joinRoom error: $error")
        Left(error)
    }
  }


}
