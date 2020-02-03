package org.seekloud.geek.http


import akka.http.scaladsl.server.Directives._
import org.seekloud.geek.shared.ptcl.CommonProtocol.GetRoomInfoReq
import akka.http.scaladsl.marshalling.{ToResponseMarshallable, ToResponseMarshaller}

import scala.language.postfixOps
import org.seekloud.geek.Boot.executor
import io.circe.Error
import akka.http.scaladsl.server.Route
import org.seekloud.geek.shared.ptcl.{ComRsp,SuccessRsp, CommonRsp}
import org.slf4j.LoggerFactory
import io.circe._
import io.circe.generic.auto._
import akka.actor.Scheduler
import akka.util.Timeout

import scala.concurrent.Future


trait RoomService extends BaseService with ServiceUtils {
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
        complete(ToResponseMarshallable(error))
    }
  }





  val userRoutes: Route = pathPrefix("user") {
     getRoomInfo ~ getRoomInfo
  }

}
