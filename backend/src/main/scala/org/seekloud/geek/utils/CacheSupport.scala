package org.seekloud.geek.utils

import akka.http.scaladsl.model.headers.CacheDirectives.{`max-age`, `public`}
import akka.http.scaladsl.model.headers.`Cache-Control`
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._

/**
  * User: zhaorui
  * Date: 2016/10/24
  * Time: 13:29
  */
trait CacheSupport {
  private val cacheSeconds = 24 * 60 * 60

  val addCacheControlHeaders: Directive0 = {
    mapResponseHeaders { headers =>
      `Cache-Control`(`public`, `max-age`(cacheSeconds)) +: headers
    }
  }
}
