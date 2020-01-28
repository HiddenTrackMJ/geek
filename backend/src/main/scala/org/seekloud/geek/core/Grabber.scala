package org.seekloud.geek.core

import java.io.InputStream

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{Behaviors, StashBuffer, TimerScheduler}
import org.bytedeco.javacv.FFmpegFrameGrabber1
import org.slf4j.LoggerFactory

/**
 * Author: Jason
 * Date: 2020/1/28
 * Time: 12:20
 */
object Grabber {

  private  val log = LoggerFactory.getLogger(this.getClass)

  sealed trait Command

  case class StartGrabber(roomId: Long) extends Command

  case object StopGrabber extends Command

  case object CloseGrabber extends Command

  case object GrabFrameFirst extends Command

  case object GrabFrame extends Command

  case class RecordActor(rec: ActorRef[Recorder.Command]) extends Command

  case object GrabLost extends Command

  case object TimerKey4Close

  def create(roomId: Long, liveId: String, buf: InputStream, recorderRef: ActorRef[Recorder.Command]): Behavior[Command]= {
    Behaviors.setup[Command]{ ctx =>
      implicit val stashBuffer: StashBuffer[Command] = StashBuffer[Command](Int.MaxValue)
      Behaviors.withTimers[Command] {
        implicit timer =>
          log.info(s"grabberActor start----")
          init(roomId, liveId, buf, recorderRef)
      }
    }
  }

  def init(roomId: Long, liveId: String, buf: InputStream,
    recorderRef:ActorRef[Recorder.Command]
  )(implicit timer: TimerScheduler[Command],
    stashBuffer: StashBuffer[Command]):Behavior[Command] = {
    log.info(s"$liveId grabber turn to init")
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case t: RecordActor =>
          log.info(s"${ctx.self} receive a msg $t")
          val grabber = new FFmpegFrameGrabber1(buf)
          try {
            grabber.start()
          } catch {
            case e: Exception =>
              log.info(s"exception occured in creant grabber")
          }
          log.info(s"$liveId grabber start successfully")
          ctx.self ! GrabFrameFirst
          work(roomId, liveId, grabber, t.rec, buf)

        case StopGrabber =>
          log.info(s"grabber $liveId stopped when init")
          Behaviors.stopped

        case x=>
          log.info(s"${ctx.self} got an unknown msg:$x")
          Behaviors.same
      }
    }
  }

  def work( roomId: Long,
    liveId: String,
    grabber: FFmpegFrameGrabber1,
    recorder: ActorRef[Recorder.Command],
    buf: InputStream
  )(implicit stashBuffer: StashBuffer[Command],
    timer: TimerScheduler[Command]): Behavior[Command] = {
    Behaviors.receive[Command] { (ctx, msg) =>
      work(roomId, liveId, grabber, recorder, buf)
    }
  }


}
