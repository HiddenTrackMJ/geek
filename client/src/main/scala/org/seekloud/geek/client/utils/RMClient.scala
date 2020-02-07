package org.seekloud.geek.client.utils

import org.seekloud.geek.client.common.{AppSettings, Routes}
import org.seekloud.geek.shared.ptcl.CommonProtocol.{SignIn, SignInRsp, SignUp, SignUpRsp}
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import org.seekloud.geek.client.Boot.executor

/**
 * User: hewro
 * Date: 2020/2/1
 * Time: 16:01
 * Description: 客户端向后台发出的请求
 */
object RMClient extends HttpUtil{

  import io.circe.generic.auto._
  import io.circe.parser.decode
  import io.circe.syntax._

  private val log = LoggerFactory.getLogger(this.getClass)

  def getPushStream(liveCode:String) = {
    AppSettings.rtpServerDst+"/live/"+liveCode
  }

  //用户名登录
  def signIn(userName: String, pwd: String): Future[Either[Throwable, SignInRsp]] = {

    val methodName = "signIn"
    val url = Routes.signIn

    val data = SignIn(userName, pwd).asJson.noSpaces
    log.debug(s"signIn by userName post data:$data")
    postJsonRequestSend(methodName, url, Nil, data, needLogRsp = false).map {
      case Right(jsonStr) =>
        decode[SignInRsp](jsonStr)
      case Left(error) =>
        log.debug(s"signIn by userName error: $error")
        Left(error)
    }

  }

  def signUp(userName:String,pwd:String):Future[Either[Throwable, SignUpRsp]] = {

    val methodName = "signUp"
    val url = Routes.signUp

    val data = SignUp(userName, pwd, "").asJson.noSpaces

    postJsonRequestSend(methodName, url, Nil, data, timeOut = 60 * 1000, needLogRsp = false).map {
      case Right(jsonStr) =>
        decode[SignUpRsp](jsonStr)
      case Left(error) =>
        log.debug(s"sign up error: $error")
        Left(error)
    }
  }

}
