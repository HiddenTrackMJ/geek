package org.seekloud.geek.http

import akka.http.scaladsl.server.Directives.pathPrefix
import akka.http.scaladsl.server.Route

import scala.language.postfixOps
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import org.seekloud.geek.Boot.{executor, invitation, roomManager, scheduler, userManager}
import akka.actor.typed.scaladsl.AskPattern._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.Message
import org.seekloud.geek.models.dao.UserDao
import org.seekloud.geek.shared.ptcl.CommonProtocol.{GetRoomInfoReq, InvitationReq, InvitationRsp, Inviter, InviterAndInviteeReq, SignIn, SignInRsp, SignUp, SignUpRsp}
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
import org.seekloud.geek.shared.ptcl.SuccessRsp
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
    entity(as[Either[Error, InviterAndInviteeReq]]) {
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

  val invitationRoutes: Route = pathPrefix("invitation") {
    getInviterList ~ getInviteeList ~ delInvitee
  }

}
