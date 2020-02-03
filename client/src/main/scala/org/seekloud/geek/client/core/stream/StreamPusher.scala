package org.seekloud.geek.client.core.stream

import java.nio.ByteBuffer
import java.nio.channels.{Channels, Pipe}

import akka.actor.typed.scaladsl.{Behaviors, StashBuffer, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import org.seekloud.geek.shared.rtp.Protocol
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

/**
  * User: TangYaruo
  * Date: 2019/8/20
  * Time: 13:41
  */
object StreamPusher {

  private val log = LoggerFactory.getLogger(this.getClass)

  type PushCommand = Protocol.Command

//  final case class InitRtpClient(pushClient: PushStreamClient) extends PushCommand

  final case object PushAuth extends PushCommand

  final case object AuthTimeOut extends PushCommand

  final case object PushStream extends PushCommand

  final case object StopPush extends PushCommand

  final case object StopSelf extends PushCommand

  private object PUSH_STREAM_TIMER_KEY

  def create(
    liveId: String,
    liveCode: String,
    parent: ActorRef[LiveManager.LiveCommand],
//    captureActor: ActorRef[CaptureActor.CaptureCommand]
  ): Behavior[PushCommand] =
    Behaviors.setup[PushCommand] { ctx =>
      log.info(s"StreamPusher-$liveId is staring.")
      implicit val stashBuffer: StashBuffer[PushCommand] = StashBuffer[PushCommand](Int.MaxValue)
      Behaviors.withTimers[PushCommand] { implicit timer =>
        init(liveId, liveCode, parent)
      }
    }

  private def init(
    liveId: String,
    liveCode: String,
    parent: ActorRef[LiveManager.LiveCommand],
//    captureActor: ActorRef[CaptureActor.CaptureCommand],
//    pushClient: Option[PushStreamClient],
  )(
    implicit timer: TimerScheduler[PushCommand],
    stashBuffer: StashBuffer[PushCommand]
  ): Behavior[PushCommand] =
    Behaviors.receive[PushCommand] { (ctx, msg) =>
      Behaviors.unhandled
//      msg match {
//        case msg: InitRtpClient =>
//          log.info(s"StreamPusher-$liveId init rtpClient.")
//          Try(msg.pushClient.authStart()) match {
//            case Success(_) =>
//              log.info(s"rtp client initial successfully, ip: ${msg.pushClient.getIp}")
//              ctx.self ! PushAuth
//              init(liveId, liveCode, parent, captureActor, Some(msg.pushClient))
//            case Failure(e) =>
//              log.info(s"rtp client initial failed, ip: ${msg.pushClient.getIp}, error msg: ${e.getMessage}")
//              parent ! LiveManager.InitRtpFailed
//              Behaviors.same
//          }
//
//
//        case PushAuth =>
//          pushClient.foreach(_.auth(liveId, liveCode))
//          timer.startSingleTimer(AuthTimeOut, AuthTimeOut, 10.seconds)
//          Behaviors.same
//
//        case msg: AuthRsp =>
//          timer.cancel(AuthTimeOut)
//          if (msg.ifSuccess) {
//            log.info(s"StreamPusher-$liveId auth success!")
//            val mediaPipe = Pipe.open() //client -> sink -> source -> server
//            val sink = mediaPipe.sink()
//            val source = mediaPipe.source()
//            val dataBuff = ByteBuffer.allocate(7 * TsPacket.tsPacketSize)
//            captureActor ! CaptureActor.StartEncode(Right(Channels.newOutputStream(sink)))
//            ctx.self ! PushStream
//            pushing(liveId, liveCode, parent, pushClient.get, captureActor, source, dataBuff)
//          } else {
//            log.debug(s"Push liveId-$liveId liveCode-$liveCode auth failed.")
//            Behaviors.same
//          }
//
//        case AuthTimeOut =>
//          log.info(s"StreamPusher-$liveId auth timeout, try again.")
//          ctx.self ! PushAuth
//          Behaviors.same
//
//        case StopPush =>
//          log.info(s"StreamPusher-$liveId StopPush in init.")
//          parent ! LiveManager.PusherStopped
//          Behaviors.stopped
//
//        case x =>
//          log.warn(s"unHandled msg in init: $x")
//          Behaviors.unhandled
//      }
    }
  private object ENSURE_STOP_PUSH
//  private def pushing(
//    liveId: String,
//    liveCode: String,
//    parent: ActorRef[LiveManager.LiveCommand],
////    pushClient: PushStreamClient,
//    //    mediaActor: ActorRef[MediaActor.MediaCommand],
////    captureActor: ActorRef[CaptureActor.CaptureCommand],
//    mediaSource: Pipe.SourceChannel,
//    dataBuff: ByteBuffer
//  )(
//    implicit timer: TimerScheduler[PushCommand],
//    stashBuffer: StashBuffer[PushCommand]
//  ): Behavior[PushCommand] =
//    Behaviors.receive[PushCommand] { (ctx, msg) =>
//      msg match {
//        case PushStream =>
//          try {
//            dataBuff.clear()
//            val bytesRead = mediaSource.read(dataBuff)
//            dataBuff.flip()
//
//            if (bytesRead != -1) {
//              //              log.debug(s"bytesRead: $bytesRead")
//              //              log.debug(s"data length: ${dataBuff.remaining()}")
//              pushClient.pushStreamData(liveId, dataBuff.array().take(dataBuff.remaining()))
//            }
//          } catch {
//            case e: Exception =>
//              log.warn(s"StreamPusher-$liveId PushStream error: $e")
//          }
//          ctx.self ! PushStream
//          Behaviors.same
//
//        case msg: PushStreamError =>
//          log.info(s"StreamPusher-$liveId push ${msg.liveId} error: ${msg.msg}")
//          ctx.self ! PushAuth
//          init(liveId, liveCode, parent, captureActor, Some(pushClient))
//
//        case StopPush =>
//          log.info(s"StreamPusher-$liveId is stopping.")
//          captureActor ! CaptureActor.StopEncode(EncoderType.STREAM)
//          pushClient.close()
//          dataBuff.clear()
//          timer.startPeriodicTimer(ENSURE_STOP_PUSH,StopPush,5000.milliseconds)
//          Behaviors.same
//
//        case CloseSuccess =>
//          log.info(s"StreamPusher-$liveId is stopped.")
//          timer.cancel(ENSURE_STOP_PUSH)
//          parent ! LiveManager.PusherStopped
//          Behaviors.stopped
//
//        case x =>
//          log.warn(s"unknown msg in pushing: $x")
//          Behaviors.unhandled
//      }
//    }


}
