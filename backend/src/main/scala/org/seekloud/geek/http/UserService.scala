package org.seekloud.geek.http

import akka.actor.typed.scaladsl.AskPattern._
import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.server.Directives.{pathPrefix, _}
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.Flow
import org.seekloud.geek.Boot.userManager
import org.seekloud.geek.core.UserManager.{MSignIn, MSignIn4Client, MSignUp, SetupWs}
import org.seekloud.geek.models.dao.UserDao
import org.seekloud.geek.shared.ptcl.CommonErrorCode._
import org.seekloud.geek.shared.ptcl.CommonProtocol._
import org.seekloud.geek.shared.ptcl.{ErrorRsp, SuccessRsp}
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.language.postfixOps
/**
 * User: hewro
 * Date: 2020/2/2
 * Time: 15:38
 * Description: 用户相关：登录注册,用户详情
 */
trait UserService extends BaseService {

  import io.circe.Error
  import io.circe.generic.auto._


  private val log = LoggerFactory.getLogger(this.getClass)

  private def signIn = (path("signIn") & post){
    entity(as[Either[Error, SignIn]]) {
      case Right(user) =>
        dealFutureResult{
          println("登录2")
          val rst:Future[SignInRsp] = userManager ? (MSignIn(user,_))
          rst.map{
            rsp=>
              complete(rsp)
          }
        }

      case Left(e) =>
        log.debug(s"signIn parse json failed,error:${e.getMessage}")
        complete(jsonFormatError)
    }
  }

  //登录注册于一体
  private def signIn4Client = (path("signIn4Client") & post){
    entity(as[Either[Error, SignIn]]) {
      case Right(user) =>
        dealFutureResult{
          println("signIn4Client")
          val rst:Future[SignInRsp] = userManager ? (MSignIn4Client(user,_))
          rst.map{
            rsp=>
              complete(rsp)
          }
        }

      case Left(e) =>
        log.debug(s"signIn parse json failed,error:${e.getMessage}")
        complete(jsonFormatError)
    }
  }


  private def signUp = (path("signUp") & post){
    entity(as[Either[Error, SignUp]]) {
      case Right(user) =>
        dealFutureResult{
          val rst:Future[SignUpRsp] = userManager ? (MSignUp(user,_))
          rst.map{
            rsp=>
              complete(rsp)
          }
        }

      case Left(e) =>
        log.debug(s"signUp parse json failed,error:${e.getMessage}")
        complete(jsonFormatError)
    }
  }

  private val setupWebSocket = (path("setupWebSocket") & get) {
    parameter(
      'userId.as[Long],
      'roomId.as[Long]
    ) { (uid,  roomId) =>
      val setWsFutureRsp: Future[Option[Flow[Message, Message, Any]]] = userManager ? (SetupWs(uid,  roomId, _))
      dealFutureResult(
        setWsFutureRsp.map {
          case Some(rsp) => handleWebSocketMessages(rsp)
          case None =>
            log.debug(s"建立websocket失败，userId=$uid,roomId=$roomId")
            complete("setup error")
        }
      )

    }
  }

  private def getUserDetail = (path("getUserDetail") & post){
    entity(as[Either[Error, GetUserReq]]) {
      case Right(req) =>
        dealFutureResult {
          UserDao.getUserDetail(req.userId).map { list =>
            val data = list.map(r => UserInfoDetail(r.id, r.name, r.avatar, r.gender, r.age, r.address)).head
            complete(GetUserRsp(Some(data)))
          }
        }
      case Left(error) =>
        log.warn(s"error in getUserDetail: $error")
        complete(jsonFormatError)
    }
  }

//  private def getUserDetail = (path("getUserDetail") & post){
//    entity(as[Either[Error, GetUserReq]]) {
//      case Right(req) =>
//        complete(GetUserRsp(Some(UserInfoDetail(1,"",Some(""),Some(2),Some(3),Some("北京市")))))
//      case Left(error) =>
//        log.warn(s"error in getUserDetail: $error")
//        complete(jsonFormatError)
//    }
//  }

  private def updateUserDetail = (path("updateUserDetail") & post){
    entity(as[Either[Error, UpdateUserReq]]) {
      case Right(req) =>
        dealFutureResult(
          UserDao.updateUserDetail(req.userId,req.userName,req.gender,req.age,req.address,{if(req.userName==req.userName2 ) true else false}).map{rsp=>
            if(rsp == -1) complete(ErrorRsp(rsp,"修改信息失败，用户名已存在"))
            else   complete(SuccessRsp())
          }

        )
      case Left(error) =>
        log.warn(s"error in updateUserDetail: $error")
        complete(jsonFormatError)
    }
  }

  private def updateAvatar = (path("updateAvatar") & post){
    entity(as[Either[Error, UpdateAvatarReq]]) {
      case Right(req) =>
        dealFutureResult(
          UserDao.updateAvatar(req.userId,req.Avatar).map { rsp =>
            if(rsp == -1) complete(ErrorRsp(rsp,"error in updateAvatar"))
            else   complete(SuccessRsp())
          }
        )
      case Left(error) =>
        log.warn(s"error in updateAvatar: $error")
        complete(jsonFormatError)
    }
  }


  val userRoutes: Route = pathPrefix("user") {
    signIn ~ signUp ~ setupWebSocket ~ getUserDetail ~ updateUserDetail ~ updateAvatar ~ signIn4Client
  }


}
