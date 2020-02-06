package org.seekloud.geek.http


import java.io.File

import akka.http.scaladsl.server.Directives._
import org.seekloud.geek.shared.ptcl.CommonProtocol.GetRoomInfoReq
import akka.http.scaladsl.marshalling.{ToResponseMarshallable, ToResponseMarshaller}

import scala.language.postfixOps
import org.seekloud.geek.Boot.executor
import akka.actor.typed.scaladsl.AskPattern._
import io.circe.Error
import akka.http.scaladsl.server.Route
import org.seekloud.geek.shared.ptcl.{ComRsp, CommonErrorCode, CommonRsp, SuccessRsp}
import org.slf4j.LoggerFactory
import io.circe._
import io.circe.generic.auto._
import akka.actor.Scheduler
import akka.http.scaladsl.model.ContentTypes
import akka.util.Timeout
import ch.megard.akka.http.cors.scaladsl.CorsDirectives.cors
import ch.megard.akka.http.cors.scaladsl.model.HttpOriginMatcher
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings
import org.seekloud.geek.Boot
import org.seekloud.geek.common.AppSettings
import org.seekloud.geek.core.RoomManager
import org.seekloud.geek.core.RoomManager.{CreateRoom, JoinRoom, KickOff, StartLive, StartLive4Client, StopLive, StopLive4Client}
import org.seekloud.geek.models.dao.VideoDao
import org.seekloud.geek.shared.ptcl.CommonErrorCode.jsonFormatError
import org.seekloud.geek.shared.ptcl.RoomProtocol.{CreateRoomReq, CreateRoomRsp, GetRecordListReq, GetRecordListRsp, GetRoomListReq, GetRoomListRsp, GetUserInfoReq, GetUserInfoRsp, JoinRoomReq, JoinRoomRsp, KickOffReq, RecordData, StartLive4ClientReq, StartLive4ClientRsp, StartLiveReq, StartLiveRsp, StopLive4ClientReq, StopLiveReq}

import scala.concurrent.Future


trait RoomService extends BaseService with ServiceUtils {
  implicit val timeout: Timeout

  implicit val scheduler: Scheduler
  private val log = LoggerFactory.getLogger(this.getClass)

  private val settings = CorsSettings.defaultSettings.withAllowedOrigins(
    HttpOriginMatcher.*
  )

//  private val getRoomInfo = (path("getRoomInfo") & post) {
//    entity(as[Either[Error, GetRoomInfoReq]]) {
//      case Right(req) =>
//        complete(ComRsp(100046, s"userId和token验证失败"))
//        complete()
////        dealFutureResult {
////          for {
////            verify <- UserInfoDao.verifyUserWithToken(req.userId, req.token)
////          } yield {
////            if (verify) {
////              dealFutureResult {
////                UserInfoDao.searchById(req.userId).map { r =>
////                  val rsp = r.get
////                  complete(RoomInfoRsp(Some(RoomInfo(rsp.roomid, s"room:${rsp.roomid}", "", rsp.uid, rsp.userName, if (rsp.headImg == "") Common.DefaultImg.headImg else rsp.headImg, if (rsp.coverImg == "") Common.DefaultImg.coverImg else rsp.coverImg, 0, 0))))
////                }
////              }
////            } else {
////              complete(ComRsp(100046, s"userId和token验证失败"))
////            }
////          }
////        }
//
//      case Left(error) =>
//        log.debug(s"获取房间信息失败，解码失败，error:$error")
//        complete(ComRsp(100045, s"decode error:$error"))
//    }


  private val getRoomInfo = (path("getRoomInfo") & post){
    entity(as[Either[Error, GetRoomInfoReq]]) {
      case Right(req) =>
//        complete("ok")
        println("ok")
        complete(req)
      case Left(error) =>
        complete(jsonFormatError)
    }
  }

  private val createRoom = (path("createRoom") & post){
    entity(as[Either[Error, CreateRoomReq]]) {
      case Right(req) =>
        dealFutureResult{
          val rst: Future[CreateRoomRsp] = Boot.roomManager ? (CreateRoom(req, _))
          rst.map{
            rsp=>
              complete(rsp)
          }
        }

      case Left(error) =>
        complete(jsonFormatError)
    }
  }

  private val startLive = (path("startLive") & post){
    entity(as[Either[Error, StartLiveReq]]) {
      case Right(req) =>
        dealFutureResult{
          val rst: Future[StartLiveRsp] = Boot.roomManager ? (StartLive(req, _))
          rst.map{
            rsp=>
              complete(rsp)
          }
        }

      case Left(error) =>
        complete(jsonFormatError)
    }
  }

  private val startLive4Client = (path("startLive4Client") & post){
    entity(as[Either[Error, StartLive4ClientReq]]) {
      case Right(req) =>
        dealFutureResult{
          val rst: Future[StartLive4ClientRsp] = Boot.roomManager ? (StartLive4Client(req, _))
          rst.map{
            rsp=>
              complete(rsp)
          }
        }

      case Left(error) =>
        complete(jsonFormatError)
    }
  }

  private val stopLive4Client = (path("stopLive4Client") & post){
    entity(as[Either[Error, StopLive4ClientReq]]) {
      case Right(req) =>
        dealFutureResult{
          val rst: Future[SuccessRsp] = Boot.roomManager ? (StopLive4Client(req, _))
          rst.map{
            rsp=>
              complete(rsp)
          }
        }

      case Left(error) =>
        complete(jsonFormatError)
    }
  }


  private val stopLive = (path("stopLive") & post){
    entity(as[Either[Error, StopLiveReq]]) {
      case Right(req) =>
        dealFutureResult{
          val rst: Future[SuccessRsp] = Boot.roomManager ? (StopLive(req, _))
          rst.map{
            rsp=>
              complete(rsp)
          }
        }

      case Left(error) =>
        complete(jsonFormatError)
    }
  }

  private val kickOff = (path("kickOff") & post){
    entity(as[Either[Error, KickOffReq]]) {
      case Right(req) =>
        dealFutureResult{
          val rst: Future[SuccessRsp] = Boot.roomManager ? (KickOff(req, _))
          rst.map{
            rsp=>
              complete(rsp)
          }
        }

      case Left(error) =>
        complete(jsonFormatError)
    }
  }

  private val getRoomList = (path("getRoomList") & post) {
    entity(as[Either[Error, GetRoomListReq]]) {
      case Right(_) =>
        dealFutureResult {
          val getRoomListRsp: Future[GetRoomListRsp] = Boot.roomManager ? RoomManager.GetRoomList
          getRoomListRsp.map {
            rsp =>
              complete(rsp)
          }
        }
      case Left(error) =>
        log.error(s"getRoomList json parse error: $error")
        complete(jsonFormatError)
    }
  }

  private val getUserInfo = (path("getUserInfo") & post) {
    entity(as[Either[Error, GetUserInfoReq]]) {
      case Right(req) =>
        dealFutureResult {
          val getRoomListRsp: Future[GetUserInfoRsp] = Boot.roomManager ? (RoomManager.GetUserInfo(req, _))
          getRoomListRsp.map {
            rsp =>
              complete(rsp)
          }
        }
      case Left(error) =>
        log.error(s"getUserInfo json parse error: $error")
        complete(jsonFormatError)
    }
  }

  val getRecord: Route = (path("getRecord" / Segments(3)) & get & pathEndOrSingleSlash & cors(settings)){
    case roomId :: startTime :: userId :: Nil =>
      println(s"getRecord req for ${roomId}_$startTime.flv")
//      dealFutureResult {
//        val getRoomListRsp: Future[GetUserInfoRsp] = Boot.roomManager ? (RoomManager.GetUserInfo(req, _))
//        getRoomListRsp.map {
//          rsp =>
//            complete(rsp)
//        }
//      }
      val f = new File(s"${AppSettings.videoPath}${roomId}_$startTime.flv").getAbsoluteFile
      getFromFile(f,ContentTypes.`application/octet-stream`)

    case x =>
      log.error(s"errs in getRecord: $x")
      complete(CommonErrorCode.fileNotExistError)
  }

  private val getRecordList = (path("getRecordList") & post){
    entity(as[Either[Error, GetRecordListReq]]) {
      case Right(req) =>
        dealFutureResult{
          VideoDao.getAllVideo.map{ v =>
            val videos = v.toList.map(i => RecordData(i.userid, i.roomid, i.timestamp, i.length))
            complete(GetRecordListRsp(videos))
          }
        }

      case Left(error) =>
        complete(jsonFormatError)
    }
  }

  private val joinRoom = (path("joinRoom") & post){
    entity(as[Either[Error, JoinRoomReq]]) {
      case Right(req) =>
        dealFutureResult{
          val rst: Future[JoinRoomRsp] = Boot.roomManager ? (JoinRoom(req, _))
          rst.map{
            rsp=>
              complete(rsp)
          }
        }

      case Left(error) =>
        complete(jsonFormatError)
    }
  }



  val roomRoutes: Route = pathPrefix("room") {
    getRoomInfo ~ createRoom ~ startLive ~ startLive4Client ~ stopLive ~ getRecordList ~ joinRoom ~
    stopLive4Client ~ getRecord ~ getRoomList ~ getUserInfo ~ kickOff
  }

}
