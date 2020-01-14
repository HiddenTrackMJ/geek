package org.seekloud.geek.http

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.model.headers.CacheDirectives.{`max-age`, public}
import akka.http.scaladsl.model.headers.`Cache-Control`
import akka.http.scaladsl.server.Directives.{pathPrefix, _}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.ContentTypeResolver.Default
import akka.stream.Materializer
import org.seekloud.geek.common.AppSettings

import scala.concurrent.ExecutionContextExecutor

/**
  * User: Taoz
  * Date: 11/16/2016
  * Time: 10:37 PM
  *
  * 12/09/2016:   add response compress. by zhangtao
  * 12/09/2016:   add cache support self. by zhangtao
  *
  */
trait ResourceService {

  implicit val system: ActorSystem

  implicit val executor: ExecutionContextExecutor

  implicit val materializer: Materializer

  val log: LoggingAdapter


  private val resources = {
    pathPrefix("css") {
      extractUnmatchedPath { path =>
        getFromResourceDirectory("css")
      }
    } ~
    pathPrefix("mp3") {
      extractUnmatchedPath { path =>
        getFromResourceDirectory("mp3")
      }
    } ~
    pathPrefix("video") {
      extractUnmatchedPath { path =>
        getFromResourceDirectory("video")
      }
    } ~
    pathPrefix("webfonts") {
      extractUnmatchedPath { path =>
        getFromResourceDirectory("webfonts")
      }
    } ~
    pathPrefix("fonts") {
      extractUnmatchedPath { path =>
        getFromResourceDirectory("fonts")
      }
    } ~
    pathPrefix("js") {
      extractUnmatchedPath { path =>
        getFromResourceDirectory("js")
      }
    } ~
    pathPrefix("sjsout") {
      extractUnmatchedPath { path =>
        getFromResourceDirectory("sjsout")
      }
    } ~
    pathPrefix(s"sjsout-${AppSettings.version}") {
      extractUnmatchedPath { path =>
        getFromResourceDirectory("sjsout")
      }
    } ~
    pathPrefix("img") {
      getFromResourceDirectory("img")
    }
  }

  //cache code copied from zhaorui.
  private val cacheSeconds = 24 * 60 * 60

  def resourceRoutes: Route = (pathPrefix("static") & get) {
    extractUnmatchedPath { name =>
      mapResponseHeaders { headers => `Cache-Control`(`public`, `max-age`(cacheSeconds)) +: headers } {
        encodeResponse(resources)
      }
    }

  }


}
