package org.seekloud.geek.http

import akka.http.scaladsl.server.Directives.pathPrefix
import akka.http.scaladsl.server.Route

import scala.language.postfixOps
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import org.seekloud.geek.Boot.{executor, roomManager, scheduler, userManager}
import akka.actor.typed.scaladsl.AskPattern._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.Message
import org.seekloud.geek.models.dao.UserDao
import org.seekloud.geek.shared.ptcl.CommonProtocol.{SignIn, SignInRsp, SignUp, SignUpRsp}
import org.seekloud.geek.utils.SecureUtil
import org.seekloud.geek.shared.ptcl.CommonErrorCode._
import org.slf4j.LoggerFactory
import akka.actor.Scheduler
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import akka.actor.typed.scaladsl.AskPattern._
import org.seekloud.geek.core.UserManager.{MSignIn, MSignUp}
import org.slf4j.LoggerFactory

import scala.concurrent.Future
/**
 * User: hewro
 * Date: 2020/2/2
 * Time: 15:38
 * Description: 用户相关：登录注册
 */
trait UserService extends BaseService {

  import io.circe.generic.auto._
  import io.circe.Error


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


  val userRoutes: Route = pathPrefix("user") {
    signIn ~ signUp
  }


}
