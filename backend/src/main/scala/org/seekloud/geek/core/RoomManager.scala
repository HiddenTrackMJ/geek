package org.seekloud.geek.core

import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicLong

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import org.seekloud.geek.Boot
import org.slf4j.LoggerFactory
import org.seekloud.geek.Boot.executor
import org.seekloud.geek.shared.ptcl.RoomProtocol.{RoomUserInfo, RtmpInfo}

import scala.collection.mutable
import scala.util.{Failure, Success}
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._



/**
 * Author: Jason
 * Date: 2020/1/25
 * Time: 15:20
 */
object RoomManager {
  private val log = LoggerFactory.getLogger(this.getClass)

  trait Command

  private final case class SwitchBehavior(
    name: String,
    behavior: Behavior[Command],
    durationOpt: Option[FiniteDuration] = None,
    timeOut: TimeOut = TimeOut("busy time error")
  ) extends Command

  private case class TimeOut(msg: String) extends Command

  private final case object BehaviorChangeKey

  private case object InitTimeKey

  private val initTime = 5.minutes

  case object Test extends Command

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
      log.info(s"RoomManager is starting...")
      implicit val stashBuffer: StashBuffer[Command] = StashBuffer[Command](Int.MaxValue)
      Behaviors.withTimers[Command] { implicit timer =>
        ctx.self ! Test
        idle()
      }
    }

  private def idle(
  )(
    implicit stashBuffer: StashBuffer[Command],
    timer: TimerScheduler[Command]
  ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case Test =>
          val roomActor = getRoomActor(ctx, 1000, RoomUserInfo("a", "b"))
          Boot.grabManager ! GrabberManager.StartLive(1000, RtmpInfo("a",List("0")), roomActor)
          Behaviors.same

        case x@_ =>
          log.info(s"${ctx.self} got an unknown msg:$x")
          Behaviors.same
      }
    }

   def getRoomActor(
    ctx: ActorContext[Command],
    roomId: Long,
    roomInfo: RoomUserInfo) = {
    val childName = s"RoomActor-$roomId"

    ctx.child(childName).getOrElse {
      ctx.spawn(RoomActor.create(roomId, roomInfo), childName)
    }.unsafeUpcast[RoomActor.Command]

  }


  private def busy()
    (
      implicit stashBuffer: StashBuffer[Command],
      timer: TimerScheduler[Command]
    ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case SwitchBehavior(name, behavior, durationOpt, timeOut) =>
          log.info(s"change behavior to $name")
          switchBehavior(ctx, name, behavior, durationOpt, timeOut)

        case TimeOut(m) =>
          log.debug(s"${ctx.self.path} is time out when busy, msg=$m")
          switchBehavior(ctx, "init", init())

        case x =>
          stashBuffer.stash(x)
          Behavior.same

      }
    }


}
