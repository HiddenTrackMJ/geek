package org.seekloud.geek.capture.core

import java.util.concurrent.LinkedBlockingDeque

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import org.bytedeco.javacv.{FFmpegFrameGrabber, OpenCVFrameGrabber}
import org.seekloud.geek.capture.core.MontageActor.DesktopImage
import org.seekloud.geek.capture.protocol.Messages.LatestFrame
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.util.{Failure, Success, Try}
/**
  * Author: wqf
  * Date: 2019/10/14
  * Time: 14:52
  */
object DesktopCapture {
  private val log = LoggerFactory.getLogger(this.getClass)
  var debug: Boolean = true

  def debug(msg: String): Unit = {
    if (debug) log.debug(msg)
  }

  sealed trait Command

  final case object StartGrab extends Command

  final case object GrabFrame extends Command

  final case object SuspendGrab extends Command

  final case object StopGrab extends Command

  def create(grabber: FFmpegFrameGrabber,  frameRate: Int, isDebug: Boolean, montageActor: ActorRef[MontageActor.Command]): Behavior[Command] =
    Behaviors.setup[Command] { ctx =>
      log.info(s"ImageCapture is staring...")
      debug = isDebug
      ctx.self ! StartGrab
      working(grabber, montageActor, frameRate)
    }


  private def working(
    grabber: FFmpegFrameGrabber,
    montageActor: ActorRef[MontageActor.Command],
    frameRate: Int,
    state: Boolean = false
  ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case StartGrab =>
          log.info(s"Media desktop started.")
          ctx.self ! GrabFrame
          working(grabber, montageActor, frameRate, true)

        case SuspendGrab =>
          log.info(s"Media desktop suspend.")
          working(grabber, montageActor, frameRate, false)

        case GrabFrame =>
          if(state) {
            Try(grabber.grab()) match {
              case Success(frame) =>
                if (frame != null) {
                  if (frame.image != null) {
                    ctx.self ! GrabFrame
                    montageActor ! DesktopImage(frame.clone())
                  }
                }

              case Failure(ex) =>
                log.error(s"grab error: $ex")
            }
          }
          Behaviors.same

        case StopGrab =>
          log.info(s"Media camera stopped.")
          try {
            grabber.release()
          } catch {
            case ex: Exception =>
              log.warn(s"release camera resources failed: $ex")
          }
          Behaviors.stopped

        case x =>
          log.warn(s"unknown msg in working: $x")
          Behaviors.unhandled
      }
    }
}
