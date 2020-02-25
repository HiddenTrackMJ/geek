package org.seekloud.geek.http


import java.io.File

import akka.http.scaladsl.server.Directives._
import org.seekloud.geek.shared.ptcl.CommonProtocol.{Comment, GetCommentReq, GetCommentRsp, GetRoomInfoReq, addCommentReq, delCommentReq}
import akka.http.scaladsl.marshalling.{ToResponseMarshallable, ToResponseMarshaller}

import scala.language.postfixOps
import org.seekloud.geek.Boot.executor
import akka.actor.typed.scaladsl.AskPattern._
import io.circe.Error
import akka.http.scaladsl.server.Route
import org.seekloud.geek.shared.ptcl.{ComRsp, CommonErrorCode, CommonRsp, ErrorRsp, SuccessRsp}
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
import org.seekloud.geek.core.{Invitation, RoomManager}
import org.seekloud.geek.core.RoomManager.{CreateRoom, JoinRoom, KickOff, StartLive, StartLive4Client, StopLive, StopLive4Client}
import org.seekloud.geek.models.dao.VideoDao
import org.seekloud.geek.shared.ptcl.CommonErrorCode.jsonFormatError
import org.seekloud.geek.shared.ptcl.RoomProtocol.{CreateRoomReq, CreateRoomRsp, GetRecordListReq, GetRecordListRsp, GetRoomIdListRsp, GetRoomListReq, GetRoomListRsp, GetRoomSectionListReq, GetRoomSectionListRsp, GetUserInfoReq, GetUserInfoRsp, JoinRoomReq, JoinRoomRsp, KickOffReq, RecordData, StartLive4ClientReq, StartLive4ClientRsp, StartLiveReq, StartLiveRsp, StopLive4ClientReq, StopLiveReq}

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

  private val getRoomSectionList = (path("getRoomSectionList") & post) {
    entity(as[Either[Error, GetRoomSectionListReq]]) {
      case Right(req) =>
        dealFutureResult {
          val getRoomListRsp: Future[GetRoomSectionListRsp] = Boot.invitation ? (Invitation.GetRoomSectionList(req, _))
          getRoomListRsp.map {
            rsp =>
              complete(rsp)
          }
        }
      case Left(error) =>
        log.error(s"getRoomSecList json parse error: $error")
        complete(jsonFormatError)
    }
  }
  private val getRoomIdList = (path("getRoomIdList") & post) {
    entity(as[Either[Error, GetRoomSectionListReq]]) {
      case Right(req) =>
        dealFutureResult {
          val getRoomListRsp: Future[GetRoomIdListRsp] = Boot.invitation ? (Invitation.GetRoomIdList(req, _))
          getRoomListRsp.map {
            rsp =>
              complete(rsp)
          }
        }
      case Left(error) =>
        log.error(s"getRoomIdList json parse error: $error")
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

  val getRecord: Route = (path("getRecord" / Segments(2)) & get & pathEndOrSingleSlash & cors(settings)){
    case userId :: file  :: Nil =>
      println(s"user id: $userId getRecord req for $file")
      dealFutureResult {
        VideoDao.getInviteeVideo(userId.toLong,file).map { list =>
          if (list.toList.nonEmpty) {
            val f = new File(s"${AppSettings.videoPath}${list.toList.head.filename}").getAbsoluteFile
//                              val f = new File(s"J:\\暂存\\videos\\录制_2020_02_15_18_54_27_35.mp4").getAbsoluteFile
            getFromFile(f,ContentTypes.`application/octet-stream`)
          }
          else {
            complete(ErrorRsp(10001, "没有该录像"))
          }
        }
      }


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

  private val getRoomCommentList = (path("getRoomCommentList") & post){
    entity(as[Either[Error, GetCommentReq]]) {
      case Right(req) =>
        dealFutureResult{
          VideoDao.getComment(req.roomId,req.filename).map{ v =>
            val rsp = v.toList.map(i => Comment(i._2.id, i._2.userid, i._2.invitation,i._1.name, i._2.comment))
            complete(GetCommentRsp(Some(rsp)))
          }
        }

      case Left(error) =>
        complete(jsonFormatError)
    }
  }

  private val addRoomComment = (path("addRoomComment") & post){
    entity(as[Either[Error, addCommentReq]]) {
      case Right(req) =>
          VideoDao.addComment(req.fileName,req.userId,req.commentContent)
          complete(SuccessRsp())

      case Left(error) =>
        complete(jsonFormatError)
    }
  }
  private val delComment = (path("delComment") & post){
    entity(as[Either[Error, delCommentReq]]) {
      case Right(req) =>
        VideoDao.deleteComment(req.roomId)
        complete(SuccessRsp())
      case Left(error) =>
        complete(jsonFormatError)
    }
  }



  val roomRoutes: Route = pathPrefix("room") {
    getRoomInfo ~ getRoomCommentList ~ createRoom ~ startLive ~ startLive4Client ~ stopLive ~ getRecordList ~ joinRoom ~
    stopLive4Client ~ getRecord ~ getRoomList ~ getUserInfo ~ kickOff ~getRoomSectionList ~ getRoomIdList ~
       addRoomComment ~ delComment

  }

}
