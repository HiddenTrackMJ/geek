package org.seekloud.geek

import akka.actor.{ActorSystem, Scheduler}
import akka.actor.typed.ActorRef
import akka.dispatch.MessageDispatcher
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.util.Timeout

import scala.language.postfixOps
import scala.util.{Failure, Success}
import akka.actor.typed.scaladsl.adapter._
import org.seekloud.geek.core.{GrabberManager, RoomManager, UserManager}
import org.seekloud.geek.http.HttpService

/**
  * User: Taoz
  * Date: 11/16/2016
  * Time: 1:00 AM
  */

object Boot extends HttpService {

  import org.seekloud.geek.common.AppSettings._
  import concurrent.duration._

  implicit val system: ActorSystem = ActorSystem("geek", config)
  //  implicit val executor = system.dispatchers.lookup("akka.actor.my-blocking-dispatcher")

  override implicit val executor: MessageDispatcher =
    system.dispatchers.lookup("akka.actor.my-blocking-dispatcher")

  override implicit val materializer: ActorMaterializer = ActorMaterializer()

  override implicit val scheduler: Scheduler = system.scheduler

  override implicit val timeout: Timeout = Timeout(10 seconds) // for actor asks

  val log: LoggingAdapter = Logging(system, getClass)

  val roomManager:ActorRef[RoomManager.Command] = system.spawn(RoomManager.init(),"roomManager")

  val grabManager: ActorRef[GrabberManager.Command] = system.spawn(GrabberManager.init(), "GrabManager")
  val userManager: ActorRef[UserManager.Command] = system.spawn(UserManager.create(), "userManager")



  def main(args: Array[String]): Unit = {
    log.info("Starting.")
    val binding = Http().bindAndHandle(routes, httpInterface, httpPort)
    binding.onComplete {
      case Success(b) ⇒
        val localAddress = b.localAddress
        println(s"Server is listening on ${localAddress.getHostName}:${localAddress.getPort}")
      case Failure(e) ⇒
        println(s"Binding failed with ${e.getMessage}")
        system.terminate()
        System.exit(-1)
    }
  }
}