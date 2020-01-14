package org.seekloud.geek.http

import akka.actor.typed.ActorRef
import akka.actor.{ActorSystem, Scheduler}
import akka.http.scaladsl.model.headers.{CacheDirective, CacheDirectives, `Cache-Control`}
import akka.http.scaladsl.server.Directive0
import akka.http.scaladsl.server.Directives.mapResponseHeaders
import akka.stream.Materializer
import akka.util.Timeout
import org.seekloud.geek.utils.CirceSupport

import scala.concurrent.ExecutionContextExecutor

/**
  * Created by dry on 2018/4/26.
  **/
trait BaseService extends CirceSupport with ServiceUtils {

  def addCacheControlHeaders(first: CacheDirective, more: CacheDirective*): Directive0 = {
    mapResponseHeaders { headers =>
      `Cache-Control`(first, more: _*) +: headers
    }
  }

  //  只使用强制缓存,设置强制缓存时间,去除协商缓存的字段
  //  addCacheControlHeadersWithFilter(`public`,`max-age`(cacheSeconds))
  //  private val cacheSeconds = 24 * 60 * 60
  def addCacheControlHeadersWithFilter(first: CacheDirective, more: CacheDirective*): Directive0 = {
    mapResponseHeaders { headers =>
      `Cache-Control`(first, more: _*) +: headers.filterNot(h => h.name() == "Last-Modified" || h.name() == "ETag")
    }
  }

  //只使用强制缓存,设置不缓存，去除协商缓存字段
  def setNoCacheInHeaderWithFilter: Directive0 = {
    mapResponseHeaders { headers =>
      `Cache-Control`.apply(CacheDirectives.`no-cache`) +: headers.filterNot(h => h.name() == "Last-Modified" || h.name() == "ETag")
    }
  }

  implicit val system: ActorSystem

  implicit val executor: ExecutionContextExecutor

  implicit val materializer: Materializer

  implicit val timeout: Timeout

  implicit val scheduler: Scheduler



}
