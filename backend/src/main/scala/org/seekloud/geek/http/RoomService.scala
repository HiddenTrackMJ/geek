package org.seekloud.geek.http


import akka.http.scaladsl.server.Directives._
import org.seekloud.geek.shared.ptcl.CommonProtocol.GetRoomInfoReq
import akka.http.scaladsl.marshalling.{ToResponseMarshallable, ToResponseMarshaller}

import scala.language.postfixOps
import org.seekloud.geek.Boot.executor
import akka.actor.typed.scaladsl.AskPattern._
import io.circe.Error
import akka.http.scaladsl.server.Route
import org.seekloud.geek.shared.ptcl.{ComRsp, CommonRsp, SuccessRsp}
import org.slf4j.LoggerFactory
import io.circe._
import io.circe.generic.auto._
import akka.actor.Scheduler
import akka.util.Timeout
import org.seekloud.geek.Boot
import org.seekloud.geek.core.RoomManager.{CreateRoom, StartLive, StartLive4Client, StopLive}
import org.seekloud.geek.shared.ptcl.CommonErrorCode.jsonFormatError
import org.seekloud.geek.shared.ptcl.RoomProtocol.{CreateRoomReq, CreateRoomRsp, StartLive4ClientReq, StartLive4ClientRsp, StartLiveReq, StartLiveRsp, StopLiveReq}

import scala.concurrent.Future


trait RoomService extends BaseService with ServiceUtils with UserService {
  implicit val timeout: Timeout

  implicit val scheduler: Scheduler
  private val log = LoggerFactory.getLogger(this.getClass)

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





  val roomRoutes: Route = pathPrefix("room") {
     getRoomInfo ~ createRoom ~ startLive ~ startLive4Client ~ stopLive
  }

}
