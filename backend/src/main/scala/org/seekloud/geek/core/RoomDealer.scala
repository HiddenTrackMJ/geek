package org.seekloud.geek.core

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import org.seekloud.byteobject.MiddleBufferInJvm
import org.seekloud.geek.shared.ptcl.CommonProtocol.RoomInfo
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.concurrent.duration.{FiniteDuration, _}
import scala.util.{Failure, Success}

/**
 * Author: Jason
 * Date: 2020/2/10
 * Time: 13:46
 */
object RoomDealer {
  import org.seekloud.byteobject.ByteObject._

  import scala.language.implicitConversions

  private val log = LoggerFactory.getLogger(this.getClass)

  trait Command

  final case class ChildDead[U](name: String, childRef: ActorRef[U]) extends Command with RoomManager.Command
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

  final case class TestRoom(roomInfo: RoomInfo) extends Command

  final case class GetRoomInfo(replyTo: ActorRef[RoomInfo]) extends Command //考虑后续房间的建立不依赖ws
  final case class UpdateRTMP(rtmp: String) extends Command

  private final val InitTime = Some(5.minutes)

  def create(roomId: Long): Behavior[Command] = {
    Behaviors.setup[Command] { ctx =>
      implicit val stashBuffer = StashBuffer[Command](Int.MaxValue)
      log.debug(s"${ctx.self.path} setup")
      Behaviors.withTimers[Command] { implicit timer =>
        implicit val sendBuffer: MiddleBufferInJvm = new MiddleBufferInJvm(1024)  //8192
        val subscribers = mutable.HashMap.empty[(Long, Boolean), ActorRef[UserActor.Command]]
        init(roomId, subscribers)
      }
    }
  }

  private def init(
    roomId: Long,
    subscribers: mutable.HashMap[(Long, Boolean), ActorRef[UserActor.Command]],
    roomInfoOpt: Option[RoomInfo] = None
  )
    (
      implicit stashBuffer: StashBuffer[Command],
      timer: TimerScheduler[Command],
      sendBuffer: MiddleBufferInJvm
    ): Behavior[Command] = {
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
//        case ActorProtocol.StartRoom4Anchor(userId, `roomId`, actor) =>
//          log.debug(s"${ctx.self.path} 用户id=$userId 开启了的新的直播间id=$roomId")
//          subscribers.put((userId, false), actor)
//          for {
//            data <- RtpClient.getLiveInfoFunc()
//            userTableOpt <- UserInfoDao.searchById(userId)
//          } yield {
//            data match {
//              case Right(rsp) =>
//                if (userTableOpt.nonEmpty) {
//
//                  val roomInfo = RoomInfo(roomId, s"${userTableOpt.get.userName}的直播间", "", userTableOpt.get.uid, userTableOpt.get.userName,
//                    UserInfoDao.getHeadImg(userTableOpt.get.headImg),
//                    UserInfoDao.getHeadImg(userTableOpt.get.coverImg), 0, 0, None,
//                    Some(rsp.liveInfo.liveId)
//                  )
//
//                  DistributorClient.startPull(roomId, rsp.liveInfo.liveId).map {
//                    case Right(r) =>
//                      log.info(s"distributor startPull succeed, get live address: ${r.liveAdd}")
//                      dispatchTo(subscribers)(List((userId, false)), StartLiveRsp(Some(rsp.liveInfo)))
//                      roomInfo.mpd = Some(r.liveAdd)
//                      val startTime = r.startTime
//                      ctx.self ! SwitchBehavior("idle", idle(WholeRoomInfo(roomInfo), mutable.HashMap(Role.host -> mutable.HashMap(userId -> rsp.liveInfo)), subscribers, mutable.Set[Long](), startTime, 0))
//
//                    case Left(e) =>
//                      log.error(s"distributor startPull error: $e")
//                      dispatchTo(subscribers)(List((userId, false)), StartLiveRefused4LiveInfoError)
//                      ctx.self ! SwitchBehavior("init", init(roomId, subscribers))
//                  }
//
//                } else {
//                  log.debug(s"${ctx.self.path} 开始直播被拒绝，数据库中没有该用户的数据，userId=$userId")
//                  dispatchTo(subscribers)(List((userId, false)), StartLiveRefused)
//                  ctx.self ! SwitchBehavior("init", init(roomId, subscribers))
//                }
//              case Left(error) =>
//                log.debug(s"${ctx.self.path} 开始直播被拒绝，请求rtp server解析失败，error:$error")
//                dispatchTo(subscribers)(List((userId, false)), StartLiveRefused)
//                ctx.self ! SwitchBehavior("init", init(roomId, subscribers))
//            }
//          }
//          switchBehavior(ctx, "busy", busy(), InitTime, TimeOut("busy"))
//
//        case GetRoomInfo(replyTo) =>
//          if (roomInfoOpt.nonEmpty) {
//            replyTo ! roomInfoOpt.get
//          }else {
//            log.debug("房间信息未更新")
//            replyTo ! RoomInfo(-1, "", "", -1l, "", "", "", -1, -1)
//          }
//          Behaviors.same
//
//        case TestRoom(roomInfo) =>
//          //仅用户测试使用空房间
//          idle(WholeRoomInfo(roomInfo), mutable.HashMap[Int, mutable.HashMap[Long, LiveInfo]](), subscribers, mutable.Set[Long](), System.currentTimeMillis(), 0)
//
//        case ActorProtocol.AddUserActor4Test(userId, roomId, userActor) =>
//          subscribers.put((userId, false), userActor)
//          Behaviors.same

        case x =>
          log.debug(s"${ctx.self.path} recv an unknown msg:$x in init state...")
          Behaviors.same
      }
    }
  }

}
