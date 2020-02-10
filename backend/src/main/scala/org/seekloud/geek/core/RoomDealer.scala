package org.seekloud.geek.core

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import org.seekloud.byteobject.MiddleBufferInJvm
import org.seekloud.geek.models.dao.UserDao
import org.seekloud.geek.protocol.RoomProtocol
import org.seekloud.geek.shared.ptcl.CommonInfo.LiveInfo
import org.seekloud.geek.shared.ptcl.CommonProtocol.RoomInfo
import org.seekloud.geek.shared.ptcl.WsProtocol._
import org.seekloud.geek.Boot.{executor, roomManager, scheduler, timeout}
import org.seekloud.geek.common.Common
import org.seekloud.geek.common.Common.Role
import org.seekloud.geek.shared.ptcl.WsProtocol
import org.seekloud.geek.shared.ptcl.WsProtocol.{ChangeLiveMode, ChangeModeRsp, Comment, GetTokenRsp, HostShutJoin, JoinAccept, JudgeLike, JudgeLikeRsp, PingPackage, RcvComment, Wrap, WsMsgClient, WsMsgRm}
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
      implicit val stashBuffer: StashBuffer[Command] = StashBuffer[Command](Int.MaxValue)
      log.debug(s"${ctx.self.path} setup")
      Behaviors.withTimers[Command] { implicit timer =>
        implicit val sendBuffer: MiddleBufferInJvm = new MiddleBufferInJvm(1024)  //8192
        val subscribers = mutable.HashMap.empty[Long, ActorRef[UserActor.Command]]
        init(roomId, subscribers)
      }
    }
  }

  private def init(
    roomId: Long,
    subscribers: mutable.HashMap[Long, ActorRef[UserActor.Command]],
    roomInfoOpt: Option[RoomInfo] = None
  )
    (
      implicit stashBuffer: StashBuffer[Command],
      timer: TimerScheduler[Command],
      sendBuffer: MiddleBufferInJvm
    ): Behavior[Command] = {
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {

        case GetRoomInfo(replyTo) =>
          if (roomInfoOpt.nonEmpty) {
            replyTo ! roomInfoOpt.get
          }else {
            log.debug("房间信息未更新")
            replyTo ! RoomInfo(-1, "", "", -1L, "", "", "", 0)
          }
          Behaviors.same

        case TestRoom(roomInfo) =>
          //仅用户测试使用空房间
          idle(roomInfo, mutable.HashMap[Int, mutable.HashMap[Long, LiveInfo]](), subscribers, mutable.Set[Long](), System.currentTimeMillis(), 0)

        case RoomProtocol.AddUserActor4Test(userId, roomId, userActor) =>
          subscribers.put(userId, userActor)
          Behaviors.same

        case x =>
          log.debug(s"${ctx.self.path} recv an unknown msg:$x in init state...")
          Behaviors.same
      }
    }
  }

  private def idle(
    wholeRoomInfo: RoomInfo, //可以考虑是否将主路的liveinfo加在这里，单独存一份连线者的liveinfo列表
    liveInfoMap: mutable.HashMap[Int, mutable.HashMap[Long, LiveInfo]],
    subscribe: mutable.HashMap[Long, ActorRef[UserActor.Command]], //需要区分订阅的用户的身份，注册用户还是临时用户(uid,是否是临时用户true:是)
    liker: mutable.Set[Long],
    startTime: Long,
    totalView: Int,
    isJoinOpen: Boolean = false,
  )
    (implicit stashBuffer: StashBuffer[Command],
      timer: TimerScheduler[Command],
      sendBuffer: MiddleBufferInJvm
    ): Behavior[Command] = {
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case RoomProtocol.AddUserActor4Test(userId, roomId, userActor) =>
          subscribe.put(userId, userActor)
          Behaviors.same

        case GetRoomInfo(replyTo) =>
          replyTo ! wholeRoomInfo
          Behaviors.same

        case UpdateRTMP(rtmp) =>
          //timer.cancel(DelayUpdateRtmpKey + wholeRoomInfo.roomId.toString)
          val newRoomInfo = wholeRoomInfo.copy(rtmp = Some(rtmp))
          log.debug(s"${ctx.self.path} 更新liveId=$rtmp,更新后的liveId=${newRoomInfo.rtmp}")
          idle(newRoomInfo, liveInfoMap, subscribe, liker, startTime, totalView, isJoinOpen)

        case RoomProtocol.WebSocketMsgWithActor(userId, roomId, wsMsg) =>
          handleWebSocketMsg(wholeRoomInfo, subscribe, liveInfoMap, liker, startTime, totalView, isJoinOpen, dispatch(subscribe), dispatchTo(subscribe))(ctx, userId, roomId, wsMsg)


        case RoomProtocol.UpdateSubscriber(join, roomId, userId, userActorOpt) =>
          var viewNum = totalView
          //虽然房间存在，但其实主播已经关闭房间，这时的startTime=-1
          //向所有人发送主播已经关闭房间的消息
          log.info(s"-----roomActor get UpdateSubscriber id: $roomId")
          if (startTime == -1) {
            dispatchTo(subscribe)(List(userId), NoAuthor)
          }
          else {
            if (join == Common.Subscriber.join) {
              // todo observe event
              viewNum += 1
              log.debug(s"${ctx.self.path}新用户加入房间roomId=$roomId,userId=$userId")
              subscribe.put((userId), userActorOpt.get)
            } else if (join == Common.Subscriber.left) {
              // todo observe event
              log.debug(s"${ctx.self.path}用户离开房间roomId=$roomId,userId=$userId")
              subscribe.remove((userId))
              if(liveInfoMap.contains(Role.audience)){
                if(liveInfoMap(Role.audience).contains(userId)){
                  wholeRoomInfo.rtmp match {
                    case Some(v) =>
                      if(v != liveInfoMap(Role.host)(wholeRoomInfo.userId).liveId){
                        liveInfoMap.remove(Role.audience)
                        ctx.self ! UpdateRTMP(liveInfoMap(Role.host)(wholeRoomInfo.userId).liveId)
                        dispatch(subscribe)(WsProtocol.AudienceDisconnect(liveInfoMap(Role.host)(wholeRoomInfo.userId).liveId))
                        dispatch(subscribe)(RcvComment(-1l, "", s"the audience has shut the join in room $roomId"))
                      }
                    case None =>
                      log.debug("no host liveId when audience left room")
                  }
                }
              }
            }
          }
          //所有的注册用户
          val audienceList = subscribe.filterNot(_._1 == (wholeRoomInfo.userId, false)).keys.toList
          val temporaryList = subscribe.filterNot(_._1 == (wholeRoomInfo.userId, false)).keys.toList
//          UserDao.getUserDes(audienceList).onComplete {
//            case Success(rst) =>
//              val temporaryUserDesList = temporaryList.map(r => UserDes(r, s"guest_$r", Common.DefaultImg.headImg))
//              dispatch(subscribe)(UpdateAudienceInfo(rst ++ temporaryUserDesList))
//            case Failure(_) =>
//
//          }  //Todo
          wholeRoomInfo.observerNum = subscribe.size - 1
          idle(wholeRoomInfo, liveInfoMap, subscribe, liker, startTime, viewNum, isJoinOpen)

        case RoomProtocol.HostCloseRoom(roomId) =>
          log.debug(s"${ctx.self.path} host close the room")
          wholeRoomInfo.rtmp match {
            case Some(v) =>
              if(v != liveInfoMap(Role.host)(wholeRoomInfo.userId).liveId){
//                ProcessorClient.closeRoom(wholeRoomInfo.roomId) //Todo  start
              }
            case None =>
          }
          if (startTime != -1l) {
            log.debug(s"${ctx.self.path} 主播向distributor发送finishPull请求")
//            DistributorClient.finishPull(liveInfoMap(Role.host)(wholeRoomInfo.userId).liveId) //Todo  start stop
//            roomManager ! RoomManager.DelaySeekRecord(wholeRoomInfo, totalView, roomId, startTime, liveInfoMap(Role.host)(wholeRoomInfo.userId).liveId)
          }
          dispatchTo(subscribe)(subscribe.filter(r => r._1 != (wholeRoomInfo.userId, false)).keys.toList, HostCloseRoom())
          Behaviors.stopped

//        case RoomProtocol.StartLiveAgain(roomId) =>
//          log.debug(s"${ctx.self.path} the room actor has been exist,the host restart the room")
//          for {
//            data <- RtpClient.getLiveInfoFunc()
//          } yield {
//            data match {
//              case Right(rsp) =>
//                liveInfoMap.put(Role.host, mutable.HashMap(wholeRoomInfo.userId -> rsp.liveInfo))
//                val liveList = liveInfoMap.toList.sortBy(_._1).flatMap(r => r._2).map(_._2.liveId)
//                //timer.startSingleTimer(DelayUpdateRtmpKey + roomId.toString, UpdateRTMP(rsp.liveInfo.liveId), 4.seconds)
//                DistributorClient.startPull(roomId, rsp.liveInfo.liveId).map {
//                  case Right(r) =>
//                    log.info("distributor startPull succeed")
//                    val startTime = r.startTime
//                    val newWholeRoomInfo = wholeRoomInfo.copy(rtmp = Some(rsp.liveInfo.liveId))
//                    dispatchTo(subscribe)(List((wholeRoomInfo.userId, false)), StartLiveRsp(Some(rsp.liveInfo)))
//                    ctx.self ! SwitchBehavior("idle", idle(newWholeRoomInfo, liveInfoMap, subscribe, liker, startTime, 0, isJoinOpen))
//                  case Left(e) =>
//                    log.error(s"distributor startPull error: $e")
//                    val newWholeRoomInfo = wholeRoomInfo.copy()
//                    dispatchTo(subscribe)(List((wholeRoomInfo.userId, false)), StartLiveRsp(Some(rsp.liveInfo)))
//                    ctx.self ! SwitchBehavior("idle", idle(newWholeRoomInfo, liveInfoMap, subscribe, liker, startTime, 0, isJoinOpen))
//                }
//
//
//              case Left(str) =>
//                log.debug(s"${ctx.self.path} 重新开始直播失败=$str")
//                dispatchTo(subscribe)(List((wholeRoomInfo.userId, false)), StartLiveRefused4LiveInfoError)
//                ctx.self ! RoomProtocol.HostCloseRoom(wholeRoomInfo.roomId)
//                ctx.self ! SwitchBehavior("idle", idle(wholeRoomInfo, liveInfoMap, subscribe, liker, startTime, 0, isJoinOpen))
//            }
//          }
//          switchBehavior(ctx, "busy", busy(), InitTime, TimeOut("busy"))

        case RoomProtocol.BanOnAnchor(roomId) =>
          //ProcessorClient.closeRoom(wholeRoomInfo.roomId)
          dispatchTo(subscribe)(subscribe.filter(r => r._1 != wholeRoomInfo.userId).keys.toList, HostCloseRoom())
          dispatchTo(subscribe)(List(wholeRoomInfo.userId), WsProtocol.BanOnAnchor)
          Behaviors.stopped

        case x =>
          log.debug(s"${ctx.self.path} recv an unknown msg $x")
          Behaviors.same
      }
    }
  }

  private def busy()
    (
      implicit stashBuffer: StashBuffer[Command],
      timer: TimerScheduler[Command],
      sendBuffer: MiddleBufferInJvm
    ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case SwitchBehavior(name, b, durationOpt, timeOut) =>
          switchBehavior(ctx, name, b, durationOpt, timeOut)

        case TimeOut(m) =>
          log.debug(s"${ctx.self.path} is time out when busy, msg=$m")
          Behaviors.stopped

        case x =>
          stashBuffer.stash(x)
          Behavior.same

      }
    }

  //websocket处理消息的函数
  /**
   * userActor --> roomManager --> roomActor --> userActor
   * roomActor
   * subscribers:map(userId,userActor)
   *
   *
   *
   **/
  private def handleWebSocketMsg(
    wholeRoomInfo: RoomInfo,
    subscribers: mutable.HashMap[Long, ActorRef[UserActor.Command]], //包括主播在内的所有用户
    liveInfoMap: mutable.HashMap[Int, mutable.HashMap[Long, LiveInfo]], //"audience"/"anchor"->Map(userId->LiveInfo)
    liker: mutable.Set[Long],
    startTime: Long,
    totalView: Int,
    isJoinOpen: Boolean = false,
    dispatch: WsMsgRm => Unit,
    dispatchTo: (List[Long], WsMsgRm) => Unit
  )
    (ctx: ActorContext[Command], userId: Long, roomId: Long, msg: WsMsgClient)
    (
      implicit stashBuffer: StashBuffer[Command],
      timer: TimerScheduler[Command],
      sendBuffer: MiddleBufferInJvm
    ): Behavior[Command] = {
    msg match {
      case ChangeLiveMode(isConnectOpen, aiMode, screenLayout) =>
        val connect = isConnectOpen match {
          case Some(v) => v
          case None => isJoinOpen
        }
        val liveList = liveInfoMap.toList.sortBy(_._1).flatMap(r => r._2).map(_._2.liveId)
        dispatchTo(List(wholeRoomInfo.userId), ChangeModeRsp())
        idle(wholeRoomInfo, liveInfoMap, subscribers, liker, startTime, totalView, connect)

      case JoinAccept(`roomId`, userId4Audience, clientType, accept) =>
        log.debug(s"${ctx.self.path} 接受连线者请求，roomId=$roomId")
        if (accept) {
        } else {
          dispatchTo(List(wholeRoomInfo.userId), AudienceJoinRsp(None))
          dispatchTo(List(userId4Audience), JoinRefused)
        }

        Behaviors.same

      case HostShutJoin(`roomId`) =>
        log.debug(s"${ctx.self.path} the host has shut the join in room$roomId")
        liveInfoMap.remove(Role.audience)
        liveInfoMap.get(Role.host) match {
          case Some(value) =>
            val liveIdHost = value.get(wholeRoomInfo.userId)
            if (liveIdHost.nonEmpty) {
              ctx.self ! UpdateRTMP(liveIdHost.get.liveId)
            }
            else {
              log.debug(s"${ctx.self.path} 没有主播的liveId,无法撤回主播流,roomId=$roomId")
              dispatchTo(List(wholeRoomInfo.userId), AudienceJoinError)
            }
          case None =>
            log.debug(s"${ctx.self.path} 没有主播的liveInfo")
            dispatchTo(List(wholeRoomInfo.userId), NoHostLiveInfoError)
        }
        //        val liveList = liveInfoMap.toList.sortBy(_._1).flatMap(r => r._2).map(_._2.liveId)
        //        ProcessorClient.updateRoomInfo(wholeRoomInfo.roomId, liveList, wholeRoomInfo.layout, wholeRoomInfo.aiMode, 0l)
        dispatch(HostDisconnect(liveInfoMap(Role.host)(wholeRoomInfo.userId).liveId))
        dispatch(RcvComment(-1l, "", s"the host has shut the join in room $roomId"))
        Behaviors.same

      case ModifyRoomInfo(roomName, roomDes) =>
        val roomInfo = if (roomName.nonEmpty && roomDes.nonEmpty) {
          wholeRoomInfo.copy(roomName = roomName.get, roomDes = roomDes.get)
        } else if (roomName.nonEmpty) {
          wholeRoomInfo.copy(roomName = roomName.get)
          wholeRoomInfo.copy(roomName = roomName.get)
        } else if (roomDes.nonEmpty) {
          wholeRoomInfo.copy(roomDes = roomDes.get)
        } else {
          wholeRoomInfo
        }
        val info = roomInfo
        log.debug(s"${ctx.self.path} modify the room info$info")
        dispatch(UpdateRoomInfo2Client(roomInfo.roomName, roomInfo.roomDes))
        dispatchTo(List(wholeRoomInfo.userId), ModifyRoomRsp())
        idle(info, liveInfoMap, subscribers, liker, startTime, totalView, isJoinOpen)


      case HostStopPushStream(`roomId`) =>
        //val liveId = wholeRoomInfo.rtmp.get
        /* wholeRoomInfo.rtmp match {
           case Some(v) =>
           case None =>
         }*/

        log.debug(s"${ctx.self.path} host stop stream in room${wholeRoomInfo.roomId},name=${wholeRoomInfo.roomName}")
        //前端需要自行处理主播主动断流的情况，后台默认连线者也会断开
        dispatch(HostStopPushStream2Client)
//        wholeRoomInfo.rtmp match {
//          case Some(v) =>
//            if(v != liveInfoMap(Role.host)(wholeRoomInfo.userId).liveId)
//            log.debug(s"roomId:$roomId 主播停止推流，向distributor发送finishpull消息")
//            if (startTime != -1l) {
//              roomManager ! RoomManager.DelaySeekRecord(wholeRoomInfo, totalView, roomId, startTime, v)
//            }
//          case None =>
//        }
        //        if (wholeRoomInfo.rtmp.get != liveInfoMap(Role.host)(wholeRoomInfo.userId).liveId)
        //          ProcessorClient.closeRoom(roomId)
        liveInfoMap.clear()

        val newroomInfo = wholeRoomInfo.copy()
        log.debug(s"${ctx.self.path} 主播userId=${userId}已经停止推流，更新房间信息，liveId=${newroomInfo.rtmp}")
        subscribers.get((wholeRoomInfo.userId)) match {
          case Some(hostActor) =>
            idle(newroomInfo, liveInfoMap, mutable.HashMap(wholeRoomInfo.userId -> hostActor), mutable.Set[Long](), -1l, totalView, isJoinOpen)
          case None =>
            idle(newroomInfo, liveInfoMap, mutable.HashMap.empty[Long, ActorRef[UserActor.Command]], mutable.Set[Long](), -1l, totalView, isJoinOpen)
        }

      case JoinReq(userId4Audience, `roomId`, clientType) =>
        if (isJoinOpen) {
          UserDao.searchById(userId4Audience).map { r =>
            if (r.nonEmpty) {
              dispatchTo(List(wholeRoomInfo.userId), AudienceJoin(userId4Audience, r.get.name, clientType))
            } else {
              log.debug(s"${ctx.self.path} 连线请求失败，用户id错误id=$userId4Audience in roomId=$roomId")
              dispatchTo(List(userId4Audience), JoinAccountError)
            }
          }.recover {
            case e: Exception =>
              log.debug(s"${ctx.self.path} 连线请求失败，内部错误error=$e")
              dispatchTo(List(userId4Audience), JoinInternalError)
          }
        } else {
          dispatchTo(List(userId4Audience), JoinInvalid)
        }
        Behaviors.same

      case AudienceShutJoin(`roomId`) =>
        //切断所有的观众连线
        liveInfoMap.get(Role.audience) match {
          case Some(value) =>
            log.debug(s"${ctx.self.path} the audience connection has been shut")
            liveInfoMap.remove(Role.audience)
            liveInfoMap.get(Role.host) match {
              case Some(info) =>
                val liveIdHost = info.get(wholeRoomInfo.userId)
                if (liveIdHost.nonEmpty) {
                  ctx.self ! UpdateRTMP(liveIdHost.get.liveId)
                }
                else {
                  log.debug(s"${ctx.self.path} 没有主播的liveId,无法撤回主播流,roomId=$roomId")
                  dispatchTo(List(wholeRoomInfo.userId), AudienceJoinError)
                }
              case None =>
                log.debug(s"${ctx.self.path} no host liveId")
            }
            //            val liveList = liveInfoMap.toList.sortBy(_._1).flatMap(r => r._2).map(_._2.liveId)
            //            ProcessorClient.updateRoomInfo(wholeRoomInfo.roomId, liveList, wholeRoomInfo.layout, wholeRoomInfo.aiMode, 0l)
            dispatch(WsProtocol.AudienceDisconnect(liveInfoMap(Role.host)(wholeRoomInfo.userId).liveId))
            dispatch(RcvComment(-1l, "", s"the audience has shut the join in room $roomId"))
          case None =>
            log.debug(s"${ctx.self.path} no audience liveId")
        }
        Behaviors.same


      case JudgeLike(`userId`, `roomId`) =>
        dispatchTo(List(userId), JudgeLikeRsp(liker.contains(userId)))
        Behaviors.same

      case Comment(`userId`, `roomId`, comment, color, extension) =>
        UserDao.searchById(userId).onComplete {
          case Success(value) =>
            value match {
              case Some(v) =>
                dispatch(RcvComment(userId, v.name, comment, color, extension))
              case None =>
                log.debug(s"${ctx.self.path.name} the database doesn't have the user")
            }
            ctx.self ! SwitchBehavior("idle", idle(wholeRoomInfo, liveInfoMap, subscribers, liker, startTime, totalView, isJoinOpen))
          case Failure(e) =>
            log.debug(s"s${ctx.self.path.name} the search by userId error:$e")
            ctx.self ! SwitchBehavior("idle", idle(wholeRoomInfo, liveInfoMap, subscribers, liker, startTime, totalView, isJoinOpen))
        }
        switchBehavior(ctx, "busy", busy(), InitTime, TimeOut("busy"))


      case PingPackage =>
        Behaviors.same

      case x =>
        log.debug(s"${ctx.self.path} recv an unknown msg:$x")
        Behaviors.same
    }
  }


  private def dispatch(subscribers: mutable.HashMap[Long, ActorRef[UserActor.Command]])(msg: WsMsgRm)(implicit sendBuffer: MiddleBufferInJvm): Unit = {
    log.debug(s"${subscribers}分发消息：$msg")
    subscribers.values.foreach(_ ! UserActor.DispatchMsg(Wrap(msg.asInstanceOf[WsMsgRm].fillMiddleBuffer(sendBuffer).result()), msg.isInstanceOf[WsProtocol.HostCloseRoom]))
  }

  /**
   * subscribers:所有的订阅者
   * targetUserIdList：要发送的目标用户
   * msg：发送的消息
   **/
  private def dispatchTo(subscribers: mutable.HashMap[Long, ActorRef[UserActor.Command]])(targetUserIdList: List[Long], msg: WsMsgRm)(implicit sendBuffer: MiddleBufferInJvm): Unit = {
    log.debug(s"${subscribers}定向分发消息：$msg")
    targetUserIdList.foreach { k =>
      subscribers.get(k).foreach(r => r ! UserActor.DispatchMsg(Wrap(msg.asInstanceOf[WsMsgRm].fillMiddleBuffer(sendBuffer).result()), msg.isInstanceOf[WsProtocol.HostCloseRoom]))
    }
  }

}
