package org.seekloud.geek.http

import akka.http.scaladsl.server.Directives.pathPrefix
import akka.http.scaladsl.server.Route

import scala.language.postfixOps
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import org.seekloud.geek.Boot.{executor, invitation, roomManager, scheduler, userManager}
import org.seekloud.geek.common.AppSettings.authCheck
import akka.actor.typed.scaladsl.AskPattern._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.Message
import org.seekloud.geek.models.dao.{UserDao, VideoDao}
import org.seekloud.geek.shared.ptcl.CommonProtocol.{CheckInviteeReq, GetRoomInfoReq, InvitationReq, InvitationRsp, Inviter, InviterAndInviteeAndRoomReq, InviterAndInviteeDetail, InviterAndInviteeDetailRsp, InviterAndInviteeReq, SignIn, SignInRsp, SignUp, SignUpRsp, addInviteeReq}
import org.seekloud.geek.utils.SecureUtil
import org.seekloud.geek.shared.ptcl.CommonErrorCode._
import org.slf4j.LoggerFactory
import akka.actor.Scheduler
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import akka.actor.typed.scaladsl.AskPattern._
import ch.megard.akka.http.cors.scaladsl.model.HttpOriginMatcher
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings
import org.seekloud.geek.core.Invitation.{DelInvitee, GetInviteeList, GetInviterList}
import org.seekloud.geek.core.UserManager.{MSignIn, MSignUp}
import org.seekloud.geek.shared.ptcl.{ErrorRsp, SuccessRsp}
import org.slf4j.LoggerFactory

import scala.concurrent.Future
/**
  * User: xgy
  * Date: 2020/2/6
  * Time: 19:23
  * Description: 邀请相关：添加邀请，查看邀请
  */
trait InvitationService extends BaseService{
  import io.circe.generic.auto._
  import io.circe.Error

  implicit val timeout: Timeout

  implicit val scheduler: Scheduler
  private val log = LoggerFactory.getLogger(this.getClass)

  private val settings = CorsSettings.defaultSettings.withAllowedOrigins(
    HttpOriginMatcher.*
  )

//  private val getRoomInfo = (path("getRoomInfo") & post){
//    entity(as[Either[Error, GetRoomInfoReq]]) {
//      case Right(req) =>
//        //        complete("ok")
//        println("ok")
//        complete(req)
//      case Left(error) =>
//        complete(jsonFormatError)
//    }
//  }

  private def getInviterList = (path("getInviterList") & post){
    entity(as[Either[Error, InvitationReq]]) {
      case Right(user) =>
        dealFutureResult{
          println("getInviterList")
          val rst:Future[InvitationRsp] = invitation ? (GetInviterList(user,_))
          rst.map{
            rsp=>
              complete(rsp)
          }
        }

      case Left(e) =>
        log.debug(s"getInviterList parse json failed,error:${e.getMessage}")
        complete(jsonFormatError)
    }
  }


  private def getInviteeList = (path("getInviteeList") & post){
    entity(as[Either[Error, InvitationReq]]) {
      case Right(user) =>
        dealFutureResult{
          println("getInviteeList")
          val rst:Future[InvitationRsp] = invitation ? (GetInviteeList(user,_))
          rst.map{
            rsp=>
              complete(rsp)
          }
        }

      case Left(e) =>
        log.debug(s"getInviteeList parse json failed,error:${e.getMessage}")
        complete(jsonFormatError)
    }
  }

//  private def getInviteeList = (path("getInviteeList") & post){
//    entity(as[Either[Error, InvitationReq]]) {
//      case Right(user) =>
//
//          complete(InvitationRsp(Option(List(Inviter("sdfa",11),Inviter("sdfdsfa",11)))))
//
//
//      case Left(e) =>
//        log.debug(s"getInviteeList parse json failed,error:${e.getMessage}")
//        complete(jsonFormatError)
//    }
//  }

  private def delInvitee = (path("delInvitee") & post){
    entity(as[Either[Error, InviterAndInviteeAndRoomReq]]) {
      case Right(user) =>
        dealFutureResult{
          println("delInvitee")
          val rst:Future[SuccessRsp] = invitation ? (DelInvitee(user,_))
          rst.map{
            rsp=>
              complete(rsp)
          }
        }
      case Left(e) =>
        log.debug(s"delInvitee parse json failed,error:${e.getMessage}")
        complete(jsonFormatError)
    }
  }

  private def checkInvitee =(path("checkInvitee") & post){
    entity(as[Either[Error, CheckInviteeReq]]) {
      case Right(req) =>
        if(authCheck){
          dealFutureResult(
            VideoDao.checkInvitee(req.inviteeId,req.fileName).map { rsp =>
              if (rsp != Vector())
                complete(SuccessRsp())
              else
                complete(ErrorRsp(1000045, "没有被邀请"))
            }

          )
        }else complete(SuccessRsp(-1,"超级权限已开启，无需邀请即可查看所有录像"))

      case Left(error) =>
        log.warn(s"error in checkInvitee: $error")
        complete(ErrorRsp(msg = "json parse error.1", errCode = 1000005))
    }
  }


  private def getInviteDetail = (path("getInviteDetail") & post){
    entity(as[Either[Error, InviterAndInviteeReq]]) {
      case Right(req) =>
        dealFutureResult{
          VideoDao.getInviteDetail(req.inviterId,req.inviteeId).map { rsp =>
            if (rsp != Vector()){
              val data = rsp.map(r=>InviterAndInviteeDetail(r.roomid)).toSet.toList
              complete(InviterAndInviteeDetailRsp(data))
            }
            else
              complete(ErrorRsp(1000045, "没有被邀请"))
          }

        }
      case Left(e) =>
        log.debug(s"delInvitee parse json failed,error:${e.getMessage}")
        complete(jsonFormatError)
    }
  }

  private def addInvitee = (path("addInvitee") & post){
    //搜索被邀请人是否存在，不存在返回空表；其次搜索邀请人在目标房间是否邀请被邀请人，不存在则存入，存在则返回错误
    entity(as[Either[Error, addInviteeReq]]) {
      case Right(req) =>
        dealFutureResult(
          VideoDao.searchInvitee_new(req.inviteeName,req.roomId).map { rsp =>
            if (rsp._1.nonEmpty){
              if(rsp._2.isEmpty){
                println("可存入")
                VideoDao.addInvitee(req.inviterId,req.roomId,rsp._1.head.id)
                complete(SuccessRsp)
              }else
                {
                  println(rsp._2)
                  complete(ErrorRsp(msg = "用户不能重复邀请", errCode = 1000005))
                }
            }else
              complete(ErrorRsp(msg = "该用户不存在", errCode = 1000005))

          }
        )
      case Left(error) =>
        log.warn(s"error in updateAvatar: $error")
        complete(ErrorRsp(msg = "json parse error.1", errCode = 1000005))
    }
  }


//  private def addInvitee = (path("addInvitee") & post){
//    //搜索被邀请人是否存在，不存在返回空表；其次搜索邀请人在目标房间是否邀请被邀请人，不存在则存入，存在则返回错误
//    entity(as[Either[Error, addInviteeReq]]) {
//      case Right(req) =>
//        dealFutureResult(
//          VideoDao.searchInvitee(req.inviteeName).map { rsp1 =>
//
//            if(rsp1.nonEmpty) {
//              dealFutureResult(
//              VideoDao.searchInvitee2(rsp1.head.id,req.roomId).map{rsp2 =>
//                if(rsp2.isEmpty){println("可存入");println(req.inviterId+" "+req.roomId+" "+rsp1.head.id);VideoDao.addInvitee(req.inviterId,req.roomId,rsp1.head.id);complete(SuccessRsp)}
//                else {println(rsp2.head.id);complete(ErrorRsp(msg = "该用户已添加", errCode = 1000005))}
//              }
//              )
//            }
//            else {complete(ErrorRsp(msg = "该用户不存在", errCode = 1000005))}
//          }
//        )
//      case Left(error) =>
//        log.warn(s"error in updateAvatar: $error")
//        complete(ErrorRsp(msg = "json parse error.1", errCode = 1000005))
//    }
//  }

  val invitationRoutes: Route = pathPrefix("invitation") {
    getInviterList ~ getInviteeList ~ delInvitee  ~ addInvitee ~ checkInvitee ~ getInviteDetail
  }

}
