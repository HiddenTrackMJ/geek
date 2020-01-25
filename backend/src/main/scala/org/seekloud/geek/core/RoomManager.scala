package org.seekloud.geek.core

import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicLong

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import org.slf4j.LoggerFactory
import org.seekloud.geek.Boot.executor
import org.seekloud.geek.shared.ptcl.RoomProtocol.RoomUserInfo

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

  private[this] def switchBehavior(ctx: ActorContext[Command],
    behaviorName: String, behavior: Behavior[Command], durationOpt: Option[FiniteDuration] = None, timeOut: TimeOut = TimeOut("busy time error"))
    (implicit stashBuffer: StashBuffer[Command],
      timer: TimerScheduler[Command]) = {
    timer.cancel(BehaviorChangeKey)
    durationOpt.foreach(timer.startSingleTimer(BehaviorChangeKey, timeOut, _))
    stashBuffer.unstashAll(ctx, behavior)
  }

  private def init(): Behavior[Command] =
    Behaviors.setup[Command] { ctx =>
      log.info(s"RoomManager is starting...")
      implicit val stashBuffer: StashBuffer[Command] = StashBuffer[Command](Int.MaxValue)
      Behaviors.withTimers[Command] { implicit timer =>
        busy()
      }
    }

  private def getRoomActor(
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
