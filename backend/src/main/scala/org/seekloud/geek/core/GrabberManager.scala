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
//        switchBehavior(ctx, "idle", idle(activeRooms, roomWorkers))
        init()
      }
    }

//  private def getRecorder(
//    ctx: ActorContext[Command],
//    roomId: Long,
//    roomActor: ActorRef[RoomActor.Command],
//    peopleNum: Int,
//    outTarget: Option[Out] = None
//  ) = {
//    val childName = s"Recorder-$roomId"
//    ctx.child(childName).getOrElse {
//      val actor = ctx.spawn(Recorder.create(roomId, roomActor, peopleNum, outTarget), childName)
//      ctx.watchWith(actor, ChildDead(childName, actor))
//      actor
//    }.unsafeUpcast[Recorder.Command]
//  }
//
//  private def getGrabber(
//    ctx: ActorContext[Command],
//    id: String,
//    recorder: ActorRef[Recorder.Command]
//  ) = {
//    val childName = s"GrabActor-${id.split("/").last}"
//    ctx.child(childName).getOrElse {
//      val actor = ctx.spawn(Grabber.create(id, recorder), childName)
//      ctx.watchWith(actor, ChildDead(childName, actor))
//      actor
//    }.unsafeUpcast[Grabber.Command]
//  }

  def getGrabberActor(
    ctx: ActorContext[Command],
    roomId: Long,
    liveId: String,
    source: InputStream,
    recorderRef: ActorRef[Recorder.Command]
  ): ActorRef[Grabber.Command] = {
    val childName = s"grabberActor_$liveId"
    ctx.child(childName).getOrElse{
      val actor = ctx.spawn(Grabber.create(roomId, liveId, source, recorderRef), childName)
//      ctx.watchWith(actor,ChildDead4Grabber(roomId, childName, actor))
      ctx.watchWith(actor, ChildDead(roomId, childName, actor))
      actor
    }.unsafeUpcast[Grabber.Command]
  }

  def getRecorderActor(
    ctx: ActorContext[Command],
    roomId: Long,
    host: String,
    client:String,
    pushLiveId: String,
    pushLiveCode: String,
    layout: Int,
    out: OutputStream): ActorRef[Recorder.Command] = {
    val childName = s"recorderActor_$pushLiveId"
    ctx.child(childName).getOrElse{
      val actor = ctx.spawn(Recorder.create(roomId, host, client, layout, out), childName)
//      ctx.watchWith(actor,ChildDead4Recorder(roomId, childName, actor))
      ctx.watchWith(actor, ChildDead(roomId, childName, actor))
      actor
    }.unsafeUpcast[Recorder.Command]
  }
}
