package org.seekloud.geek.core

import java.io.{BufferedReader, File, InputStreamReader}

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import org.bytedeco.javacpp.Loader
import org.seekloud.geek.Boot.{executor, scheduler}
import org.seekloud.geek.shared.ptcl.RoomProtocol.{RoomUserInfo, RtmpInfo}
import org.slf4j.LoggerFactory
import org.seekloud.geek.Boot.grabManager
import org.seekloud.geek.common.AppSettings
import org.seekloud.geek.models.SlickTables
import org.seekloud.geek.models.dao.VideoDao


import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._

/**
 * Author: Jason
 * Date: 2020/1/25
 * Time: 15:25
 */
object RoomActor {
  private val log = LoggerFactory.getLogger(this.getClass)

  sealed trait Command

  final case class StartLive(rtmpInfo: RtmpInfo, hostCode: String, hostId: Long) extends Command

  final case class StartLive4Client(rtmpInfo: RtmpInfo, selfCode: String) extends Command

  final case class StopLive(rtmpInfo: RtmpInfo) extends Command

  final case class StopLive4Client(userId: Long, selfCode: String) extends Command

  final case class StoreVideo(video: SlickTables.rVideo) extends Command

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

        case msg: StartLive =>
          grabManager ! GrabberManager.StartLive(roomId, msg.hostId, msg.rtmpInfo, msg.hostCode, ctx.self)
          switchBehavior(ctx, "idle", idle(roomId, roomInfo, Some(msg.rtmpInfo)))

        case msg: StartLive4Client =>
          grabManager ! GrabberManager.StartLive4Client(roomId, msg.rtmpInfo, msg.selfCode, ctx.self)
          switchBehavior(ctx, "idle", idle(roomId, roomInfo, Some(msg.rtmpInfo)))

        case msg: StopLive =>
          log.info(s"RoomActor-$roomId is stopping...")
          grabManager ! GrabberManager.StopLive(roomId, msg.rtmpInfo, ctx.self)
          Behaviors.same

        case msg: StopLive4Client =>
          log.info(s"RoomActor-$roomId userId-${msg.userId} is stopping...")
          grabManager ! GrabberManager.StopLive4Client(roomId, msg.userId, msg.selfCode, ctx.self)
          Behaviors.same

        case msg: StoreVideo =>
          def fun(): Unit ={
            log.info(s"RoomActor-$roomId is storing video...")
            var d = ""
            val file = new File(s"${AppSettings.videoPath}${msg.video.filename}.flv")
            if(file.exists()){
              d = getVideoDuration(msg.video.filename)
              log.info(s"duration:$d")
              val video = msg.video.copy(length = d)
              VideoDao.addVideo(video)
            }else{
              log.info(s"no record for roomId:$roomId and startTime:${msg.video.timestamp}")
            }
          }
          scheduler.scheduleOnce(1.seconds)(() => fun())
          Behaviors.stopped

        case x =>
          log.warn(s"unknown msg: $x")
          Behaviors.unhandled
      }
    }

  private def getVideoDuration(fileName: String) ={
    val ffprobe = Loader.load(classOf[org.bytedeco.ffmpeg.ffprobe])
    //容器时长（container duration）
    val pb = new ProcessBuilder(ffprobe,"-v","error","-show_entries","format=duration", "-of","csv=\"p=0\"","-i", s"${AppSettings.videoPath}${fileName}.flv")
    val processor = pb.start()
    val br = new BufferedReader(new InputStreamReader(processor.getInputStream))
    val s = br.readLine()
    var duration = 0
    if(s!= null){
      duration = (s.toDouble * 1000).toInt
    }
    br.close()
    //    if(processor != null){
    //      processor.destroyForcibly()
    //    }
    millis2HHMMSS(duration)
  }

  def millis2HHMMSS(sec: Double): String = {
    val hours = (sec / 3600000).toInt
    val h =  if (hours >= 10) hours.toString else "0" + hours
    val minutes = ((sec % 3600000) / 60000).toInt
    val m = if (minutes >= 10) minutes.toString else "0" + minutes
    val seconds = ((sec % 60000) / 1000).toInt
    val s = if (seconds >= 10) seconds.toString else "0" + seconds
    val dec = ((sec % 1000) / 10).toInt
    val d = if (dec >= 10) dec.toString else "0" + dec
    s"$h:$m:$s.$d"
  }
}
