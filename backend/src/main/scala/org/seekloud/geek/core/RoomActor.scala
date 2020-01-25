package org.seekloud.geek.core

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import org.seekloud.geek.shared.ptcl.RoomProtocol.{RoomUserInfo, RtmpInfo}
import org.slf4j.LoggerFactory

import scala.concurrent.duration.FiniteDuration

/**
 * Author: Jason
 * Date: 2020/1/25
 * Time: 15:25
 */
object RoomActor {
  private val log = LoggerFactory.getLogger(this.getClass)

  sealed trait Command

  private final case class SwitchBehavior(
    name: String,
    behavior: Behavior[Command],
    durationOpt: Option[FiniteDuration] = None,
    timeOut: TimeOut = TimeOut("busy time error")
  ) extends Command

  private case class TimeOut(msg: String) extends Command

  private final case object BehaviorChangeKey

  private[this] def switchBehavior(ctx: ActorContext[Command],
    behaviorName: String, behavior: Behavior[Command], durationOpt: Option[FiniteDuration] = None, timeOut: TimeOut = TimeOut("busy time error"))
    (implicit stashBuffer: StashBuffer[Command],
      timer: TimerScheduler[Command]) = {
    timer.cancel(BehaviorChangeKey)
    durationOpt.foreach(timer.startSingleTimer(BehaviorChangeKey, timeOut, _))
    stashBuffer.unstashAll(ctx, behavior)
  }

  def create(roomId: Long, roomInfo: RoomUserInfo): Behavior[Command] =
    Behaviors.setup[Command] { ctx =>
      log.info(s"RoomActor-$roomId is starting...")
      implicit val stashBuffer: StashBuffer[Command] = StashBuffer[Command](Int.MaxValue)
      Behaviors.withTimers[Command] { implicit timer =>
        switchBehavior(ctx, "idle", idle(roomId, roomInfo))
      }
    }

  private def idle(
    roomId: Long,
    roomInfo: RoomUserInfo,
    rtmpInfo: Option[RtmpInfo] = None
  )(
    implicit stashBuffer: StashBuffer[Command],
    timer: TimerScheduler[Command]
  ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {

        case x =>
          log.warn(s"unknown msg: $x")
          Behaviors.unhandled
      }
    }
}
