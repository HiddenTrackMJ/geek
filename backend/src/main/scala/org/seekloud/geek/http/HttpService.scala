package org.seekloud.geek.http

import akka.actor.{ActorSystem, Scheduler}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.model.headers.CacheDirectives.{`max-age`, `public`}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import akka.util.Timeout


import scala.concurrent.{ExecutionContextExecutor, Future}

/**
  * User: Taoz
  * Date: 8/26/2016
  * Time: 10:27 PM
  */
trait HttpService extends
       ServiceUtils
  with RoomService
  with UserService
  with ResourceService
  with InvitationService
  with FileService
  {

    implicit val system: ActorSystem

    implicit val executor: ExecutionContextExecutor

    implicit val materializer: Materializer

    implicit val timeout: Timeout

    implicit val scheduler: Scheduler


    private val home = get {
      getFromResource("html/index.html")
    }

    val routes: Route =
      ignoreTrailingSlash {
        pathPrefix("geek") {
          pathEndOrSingleSlash {
            getFromResource("html/home.html")
          } ~
          resourceRoutes ~ userRoutes ~ roomRoutes ~ invitationRoutes ~ fileRoute

        } ~ home
      }


}
