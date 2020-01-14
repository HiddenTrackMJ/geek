package org.seekloud.geek.http

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse}
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives.extractRequestContext
import akka.actor.typed.scaladsl.AskPattern._
import org.seekloud.geek.shared.ptcl.ErrorRsp
import org.seekloud.geek.shared.ptcl.CommonErrorCode._
import org.slf4j.LoggerFactory
import io.circe.parser.decode
import io.circe.Error
import io.circe.Decoder

import scala.concurrent.Future
import scala.util.{Failure, Success}


/**
  * User: Taoz
  * Date: 11/18/2016
  * Time: 7:57 PM
  */

object ServiceUtils{
  private val log = LoggerFactory.getLogger(this.getClass)

  case class CommonRsp(errCode: Int = 0, msg: String = "ok")
  final val JsonParseError = CommonRsp(10002, "Json parse error.")

}

trait ServiceUtils {

  import ServiceUtils._
  import io.circe.generic.auto._
  import org.seekloud.geek.utils.CirceSupport._


  def htmlResponse(html: String): HttpResponse = {
    HttpResponse(entity = HttpEntity(ContentTypes.`text/html(UTF-8)`, html))
  }

  def jsonResponse(json: String): HttpResponse = {
    HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, json))
  }

  def dealFutureResult(future: => Future[server.Route]): Route = {
    onComplete(future) {
      case Success(rst) => rst
      case Failure(e) =>
        e.printStackTrace()
        log.error("internal error: {}", e.getMessage)
        complete(ErrorRsp(1000, "internal error."))
    }
  }

}
