package org.seekloud.geek.core

import java.io.OutputStream

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{Behaviors, StashBuffer, TimerScheduler}
import org.bytedeco.ffmpeg.global.{avcodec, avutil}
import org.bytedeco.javacv.{FFmpegFrameRecorder, Frame}
import org.seekloud.geek.Boot
import org.seekloud.geek.common.AppSettings
import org.seekloud.geek.shared.ptcl.Protocol.OutTarget
import org.slf4j.LoggerFactory

/**
 * Author: Jason
 * Date: 2020/1/28
 * Time: 12:20
 */
object Recorder {

  var audioChannels = 2 //todo 待议
  val sampleFormat = 1 //todo 待议
  var frameRate = 30
  val bitRate = 2000000

  private val log = LoggerFactory.getLogger(this.getClass)

  sealed trait Command

  case class UpdateRoomInfo(roomId: Long, layout: Int) extends Command

  case object Init extends Command

  case class GetGrabber(id: String, grabber: ActorRef[Grabber.Command]) extends Command

  case object RestartRecord extends Command

  case class StopRecorder(msg: String) extends Command

  case object CloseRecorder extends Command

  case class NewFrame(liveId: String, frame: Frame) extends Command

  case class UpdateRecorder(channel: Int, sampleRate: Int, frameRate: Double, width: Int, height: Int, liveId: String) extends Command


  def create(roomId: Long, pullLiveId:List[String], layout: Int, outTarget: Option[OutTarget] = None): Behavior[Command] = {
    Behaviors.setup[Command] { ctx =>
      implicit val stashBuffer: StashBuffer[Command] = StashBuffer[Command](Int.MaxValue)
      Behaviors.withTimers[Command] {
        implicit timer =>
          log.info(s"recorderActor-$roomId start----")
//          avutil.av_log_set_level(-8)
          val srcPath = AppSettings.rtmpServer + roomId
          val recorder4ts = new FFmpegFrameRecorder(srcPath, 640, 480, audioChannels)
          recorder4ts.setFrameRate(frameRate)
          recorder4ts.setVideoBitrate(bitRate)
          recorder4ts.setVideoCodec(avcodec.AV_CODEC_ID_MPEG2VIDEO)
          recorder4ts.setAudioCodec(avcodec.AV_CODEC_ID_MP2)
          recorder4ts.setMaxBFrames(0)
          recorder4ts.setFormat("mpegts")
//          try {
//            recorder4ts.startUnsafe()
//          } catch {
//            case e: Exception =>
//              log.error(s" recorder starts error:$e")
//          }
//          Boot.roomManager ! RoomManager.RecorderRef(roomId, ctx.self) //fixme 取消注释
//          ctx.self ! Init
//          single(roomId,  pullLiveId, layout, recorder4ts, null, null, null, null, srcPath, 30000, (0, 0))
//          create(roomId: Long, pullLiveId:List[String], layout: Int, outTarget)
          idle()
      }
    }
  }

  private def idle(
  )(
    implicit stashBuffer: StashBuffer[Command],
    timer: TimerScheduler[Command]
  ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case GetGrabber(id, grabber) =>
          grabber ! Grabber.GetRecorder(ctx.self)
          Behaviors.same
        case x@_ =>
          log.info(s"${ctx.self} got an unknown msg:$x")
          Behaviors.same
      }
    }

}
