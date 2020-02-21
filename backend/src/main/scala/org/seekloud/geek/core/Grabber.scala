package org.seekloud.geek.core

import java.io.InputStream

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import org.bytedeco.javacv.{FFmpegFrameGrabber, FFmpegFrameGrabber1}
import org.seekloud.geek.common.AppSettings
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

/**
 * Author: Jason
 * Date: 2020/1/28
 * Time: 12:20
 */
object Grabber {

  private  val log = LoggerFactory.getLogger(this.getClass)

  sealed trait Command

  private final case class SwitchBehavior(
    name: String,
    behavior: Behavior[Command],
    durationOpt: Option[FiniteDuration] = None,
    timeOut: TimeOut = TimeOut("busy time error")
  ) extends Command

  private case class TimeOut(msg: String) extends Command

  private final case object BehaviorChangeKey

  case class StartGrabber(roomId: Long) extends Command

  final case class StopGrabber(reason: String) extends Command

  case object CloseGrabber extends Command

  case object GrabFrameFirst extends Command

  case object GrabFrame extends Command

  case class GetRecorder(rec: ActorRef[Recorder.Command]) extends Command

  case object GrabLost extends Command

  case class State(image: Boolean, audio: Boolean)

  final case class Shield(image: Boolean, audio: Boolean) extends Command

  case object TimerKey4Close

  private[this] def switchBehavior(ctx: ActorContext[Command],
    behaviorName: String, behavior: Behavior[Command], durationOpt: Option[FiniteDuration] = None, timeOut: TimeOut = TimeOut("busy time error"))
    (implicit stashBuffer: StashBuffer[Command],
      timer: TimerScheduler[Command]) = {
    log.info(s"grabber switches to $behaviorName")
    timer.cancel(BehaviorChangeKey)
    durationOpt.foreach(timer.startSingleTimer(BehaviorChangeKey, timeOut, _))
    stashBuffer.unstashAll(ctx, behavior)
  }


  def create(roomId: Long, liveId: String, recorderRef: ActorRef[Recorder.Command]): Behavior[Command]= {
    Behaviors.setup[Command]{ ctx =>
      implicit val stashBuffer: StashBuffer[Command] = StashBuffer[Command](Int.MaxValue)
      Behaviors.withTimers[Command] {
        implicit timer =>
          log.info(s"grabberActor-$liveId start----")
          recorderRef ! Recorder.GetGrabber(liveId, ctx.self)
          init(roomId, liveId, recorderRef)
//          switchBehavior(ctx, "init", init(roomId, liveId, recorderRef))
      }
    }
  }

  def init(
    roomId: Long,
    liveId: String,
    recorderRef: ActorRef[Recorder.Command]
  )(implicit timer: TimerScheduler[Command],
    stashBuffer: StashBuffer[Command]):Behavior[Command] = {
    log.info(s"$liveId grabber turn to init")
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case t: GetRecorder =>
          val srcPath =
          //"rtmp://58.200.131.2:1935/livetv/hunantv"
//          "rtmp://127.0.0.1:1935/live/1000_2"
//          if (AppSettings.rtmpIsTest) "C:\\Users\\19783\\Videos\\Captures\\" + liveId.split("_").head
//          else
            AppSettings.rtmpServer + (if (liveId == "1000_1_2" || liveId == "1000_1_3" || liveId == "1000_1_4" || liveId == "1000_1_5") "1000_1" else liveId)
          println(s"srcPath: $srcPath")
          log.info(s"${ctx.self} receive a msg $t")
          val grabber = new FFmpegFrameGrabber(srcPath)
          Try {
            grabber.startUnsafe()
          } match {
            case Success(value) =>
              log.info(s"$liveId grabber start successfully")
              ctx.self ! GrabFrameFirst
            case Failure(e) =>
              log.info(s"exception occurs when creating grabber, error: ${e.getMessage}")
          }

          work(roomId, liveId, State(image = true, audio = true), grabber, t.rec)

        case StopGrabber(reason) =>
          log.info(s"grabber $liveId stopped when init, reason:$reason")
          Behaviors.stopped

        case x=>
          log.info(s"${ctx.self} got an unknown msg:$x")
          Behaviors.same
      }
    }
  }

  def work( roomId: Long,
    liveId: String,
    state: State,
    grabber: FFmpegFrameGrabber,
    recorder: ActorRef[Recorder.Command]
  )(implicit stashBuffer: StashBuffer[Command],
    timer: TimerScheduler[Command]): Behavior[Command] = {
    Behaviors.receive[Command] {(ctx, msg) =>
      msg match {
        case GrabLost =>
          val frame = grabber.grab()
          if(frame != null){
            if(frame.image != null){
              recorder ! Recorder.NewFrame(liveId, frame.clone())
              ctx.self ! GrabFrame
            }else{
              ctx.self ! GrabLost
            }
          }
          Behaviors.same

        case t:GetRecorder =>
          Behaviors.same

        case Shield(image, audio) =>
          work(roomId, liveId, State(image, audio), grabber, recorder)

        case GrabFrameFirst =>
          log.info(s"${ctx.self} receive a msg:${msg}")
          val frame = grabber.grab()
          val channel = grabber.getAudioChannels
          val sampleRate = grabber.getSampleRate
          val height = grabber.getImageHeight
          val width = grabber.getImageWidth
          recorder ! Recorder.UpdateRecorder(channel, sampleRate, grabber.getFrameRate, width, height, liveId)

          if(frame != null){
            if(frame.image != null && state.image){
              recorder ! Recorder.NewFrame(liveId, frame.clone())
              ctx.self ! GrabFrame
            }else{
              ctx.self ! GrabLost
            }
          } else {
            log.info(s"$liveId --- frame is null")
            ctx.self ! StopGrabber(s"$liveId --- frame is null")
          }
          Behaviors.same

        case GrabFrame =>
          val frame = grabber.grab()
          if (frame != null) {
            if(frame.image != null && state.image) {
              recorder ! Recorder.NewFrame(liveId, frame.clone())
              ctx.self ! GrabFrame
            }
            else if(frame.samples != null && state.audio) {
              recorder ! Recorder.NewFrame(liveId, frame.clone())
              ctx.self ! GrabFrame
            }
          }
          else{
            log.info(s"$liveId --- frame is null")
            ctx.self ! StopGrabber(s"$liveId --- frame is null")
          }
          Behaviors.same

        case StopGrabber(msg) =>
          timer.startSingleTimer(TimerKey4Close, CloseGrabber, 400.milli)
          Behaviors.same


        case CloseGrabber =>
          try {
            log.info(s"${ctx.self} stop ----")
            grabber.release()
            grabber.close()
          }catch {
            case e:Exception =>
              log.error(s"${ctx.self} close error:$e")
          }
          Behaviors.stopped

        case x@_ =>
          log.info(s"${ctx.self} rev an unknown msg: $x")
          Behaviors.same

      }
    }
  }


}
