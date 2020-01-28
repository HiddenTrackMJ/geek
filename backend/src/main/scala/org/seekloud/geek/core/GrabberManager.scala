package org.seekloud.geek.core

import java.io.{InputStream, OutputStream}

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import org.seekloud.geek.shared.ptcl.Protocol.OutTarget
import org.seekloud.geek.shared.ptcl.RoomProtocol.RtmpInfo
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration

/**
 * Author: Jason
 * Date: 2020/1/28
 * Time: 14:33
 */
object GrabberManager {

  private val log = LoggerFactory.getLogger(this.getClass)

  trait Command

  private case class ChildDead[U](roomId: Long, name: String, childRef: ActorRef[U]) extends Command

  private final case class SwitchBehavior(
    name: String,
    behavior: Behavior[Command],
    durationOpt: Option[FiniteDuration] = None,
    timeOut: TimeOut = TimeOut("busy time error")
  ) extends Command

  private case class TimeOut(msg: String) extends Command

  private final case object BehaviorChangeKey

  final case class StartLive(roomId: Long, rtmpInfo: RtmpInfo, roomActor: ActorRef[RoomActor.Command]) extends Command

  final case class StopLive(roomId: Long, rtmpInfo: RtmpInfo, roomActor: ActorRef[RoomActor.Command]) extends Command

  final case class StartTrans(src: List[String], out: OutTarget, roomActor: ActorRef[RoomActor.Command]) extends Command

  private[this] def switchBehavior(ctx: ActorContext[Command],
    behaviorName: String, behavior: Behavior[Command], durationOpt: Option[FiniteDuration] = None, timeOut: TimeOut = TimeOut("busy time error"))
    (implicit stashBuffer: StashBuffer[Command],
      timer: TimerScheduler[Command]) = {
    timer.cancel(BehaviorChangeKey)
    durationOpt.foreach(timer.startSingleTimer(BehaviorChangeKey, timeOut, _))
    stashBuffer.unstashAll(ctx, behavior)
  }

  def init(): Behavior[Command] =
    Behaviors.setup[Command] { ctx =>
      log.info(s"GrabberManager is starting...")
      implicit val stashBuffer: StashBuffer[Command] = StashBuffer[Command](Int.MaxValue)
      Behaviors.withTimers[Command] { implicit timer =>
        val activeRooms = mutable.HashMap[Long, (RtmpInfo, ActorRef[RoomActor.Command])]()
        val roomWorkers = mutable.HashMap[Long, (ActorRef[Recorder.Command], List[ActorRef[Grabber.Command]])]()
        switchBehavior(ctx, "idle", idle(activeRooms, roomWorkers))
      }
    }

  private def idle(
    activeRooms: mutable.HashMap[Long, (RtmpInfo, ActorRef[RoomActor.Command])],
    roomWorkers: mutable.HashMap[Long, (ActorRef[Recorder.Command], List[ActorRef[Grabber.Command]])]
  )(
    implicit stashBuffer: StashBuffer[Command],
    timer: TimerScheduler[Command]
  ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case msg: StartLive =>
          val recordActor = getRecorder(ctx, msg.roomId, msg.roomActor, msg.rtmpInfo.liveCode, 1)//todo layout
          val grabbers = msg.rtmpInfo.liveCode.map {
            stream =>
              getGrabber(ctx, msg.roomId, stream, recordActor)
          }
          activeRooms.put(msg.roomId, (msg.rtmpInfo, msg.roomActor))
          roomWorkers.put(msg.roomId, (recordActor, grabbers))
          Behaviors.same

        case msg: StopLive =>
          log.info(s"stopping room ${msg.roomId}")
          val recordActor = getRecorder(ctx, msg.roomId, msg.roomActor, msg.rtmpInfo.liveCode, 1)//todo layout
          recordActor ! Recorder.StopRecorder("user stop live")
          msg.rtmpInfo.liveCode.foreach {
            stream =>
              getGrabber(ctx, msg.roomId, stream, recordActor) ! Grabber.StopGrabber("user stop live")
          }
          activeRooms.remove(msg.roomId)
          roomWorkers.remove(msg.roomId)
          Behaviors.same


        case msg: StartTrans =>
          log.info(s"start testing...")
          val recordActor = getRecorder(ctx, 0L, msg.roomActor, msg.src, 1, Some(msg.out))//todo layout
          msg.src.map {
            stream =>
              getGrabber(ctx, 0L, stream, recordActor)
          }
          Behaviors.same

        case ChildDead(roomId, child, childRef) =>
          log.debug(s"grabManager unWatch room-$roomId: child-$child")
          ctx.unwatch(childRef)
          Behaviors.same

        case x =>
          log.warn(s"unknown msg: $x")
          Behaviors.unhandled
      }
    }


  def getGrabber(
    ctx: ActorContext[Command],
    roomId: Long,
    liveId: String,
    recorderRef: ActorRef[Recorder.Command]
  ): ActorRef[Grabber.Command] = {
    val childName = s"grabber_-${liveId.split("/").last}"
    ctx.child(childName).getOrElse{
      val actor = ctx.spawn(Grabber.create(roomId, liveId, recorderRef), childName)
//      ctx.watchWith(actor,ChildDead4Grabber(roomId, childName, actor))
      ctx.watchWith(actor, ChildDead(roomId, childName, actor))
      actor
    }.unsafeUpcast[Grabber.Command]
  }

  def getRecorder(
    ctx: ActorContext[Command],
    roomId: Long,
    roomActor: ActorRef[RoomActor.Command],
    pullLiveId: List[String],
    layout: Int,
    outTarget: Option[OutTarget] = None): ActorRef[Recorder.Command] = {
    val childName = s"recorder_$roomId"
    ctx.child(childName).getOrElse{
      val actor = ctx.spawn(Recorder.create(roomId, pullLiveId, layout, outTarget), childName)
//      ctx.watchWith(actor,ChildDead4Recorder(roomId, childName, actor))
      ctx.watchWith(actor, ChildDead(roomId, childName, actor))
      actor
    }.unsafeUpcast[Recorder.Command]
  }
}
