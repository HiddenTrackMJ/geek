package org.seekloud.geek.player.core

import akka.actor.typed.scaladsl.{Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import javax.sound.sampled.{AudioFormat, AudioSystem, SourceDataLine}
import org.seekloud.geek.player.protocol.Messages.{AddSamples, SoundFinish}
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.concurrent.duration._
/**
  * Author: zwq
  * Date: 2019/8/28
  * Time: 21:59
  */
object SoundActor {
  private val log = LoggerFactory.getLogger(this.getClass)
  private var debug: Boolean = true

  private def debug(str: String): Unit = {
    if(debug) log.debug(str)
  }

  /*settings*/
  val BIT_PER_SAMPLE = 16
  val BIG_ENDIAN = true

  trait SoundCmd

  final case object PausePlaySound extends SoundCmd

  final case object ContinuePlaySound extends SoundCmd

  final object TryPlaySoundTick extends SoundCmd

  final object AUDIO_TIMER_KEY



  def create(
    id: String,
    playerGrabber: ActorRef[PlayerGrabber.MonitorCmd],
    sampleRate: Int,
    channels: Int,
    nbSamples: Int,
    imageActor: Option[ActorRef[ImageActor.ImageCmd]],
    isDebug: Boolean = true
  ): Behavior[SoundCmd] = Behaviors.setup { _ =>
    log.info(s"SoundActor-$id is starting......")
    debug = isDebug
//    debug(s"sampleRate: $sampleRate, channels: $channels, nbSamples: $nbSamples")
    val audioFormat = new AudioFormat(sampleRate, BIT_PER_SAMPLE, channels, true, BIG_ENDIAN)
    //todo open失败处理
    //TODO 声音大小调整
    val sdl = AudioSystem.getSourceDataLine(audioFormat)
    sdl.open(audioFormat)
    sdl.start()
    Behaviors.withTimers[SoundCmd] { implicit timer =>
//      log.info(s"start Sound Timer in SoundActor-$id.")
      timer.startPeriodicTimer(
        AUDIO_TIMER_KEY,
        TryPlaySoundTick,
        (1000 * nbSamples / sampleRate).millisecond
      )
      val bytesPerSecond = ((audioFormat.getSampleSizeInBits + 7) / 8 * audioFormat.getChannels * audioFormat.getSampleRate).toInt
      working(id, sdl, nbSamples, sampleRate, playerGrabber, mutable.Queue[Array[Byte]](), 0L, sdl.available(), imageActor, bytesPerSecond)
    }
  }

  def working(
    id: String,
    sdl: SourceDataLine,
    nbSamples: Int,
    sampleRate: Int,
    playerGrabber: ActorRef[PlayerGrabber.MonitorCmd],
    samplesQueue: mutable.Queue[Array[Byte]],
    playedSamplesByte: Long,
    lastAvailable: Int,
    imageActorOpt: Option[ActorRef[ImageActor.ImageCmd]],
    bytePerSecond: Int
  )(
    implicit timer: TimerScheduler[SoundCmd]
  ): Behavior[SoundCmd] = Behaviors.receive { (ctx, msg) =>

    msg match {
      case PausePlaySound =>
        log.info(s"SoundActor-$id got PausePlay.")
        timer.cancel(AUDIO_TIMER_KEY)
        log.info(s"SoundActor-$id cancel Sound Timer.")
//        sdl.drain()
        Behaviors.same

      case ContinuePlaySound =>
        log.info(s"SoundActor-$id got ContinuePlay.")
        log.info(s"start Sound Timer in SoundActor-$id.")
        timer.startPeriodicTimer(
          AUDIO_TIMER_KEY,
          TryPlaySoundTick,
          (1000 * nbSamples / sampleRate).millisecond
        )
        Behaviors.same

      case AddSamples(samples, ts) =>
        //TODO first sample is not begin at 0 timestamp.
        val newPlayedSamplesByte =
          if (playedSamplesByte == 0 && ts > 0) {
            ts * bytePerSecond / 1000000
          } else playedSamplesByte

        //println(s"soundPlayer got $m")
        samplesQueue.enqueue(samples)
//        println(s"SoundActor get samples, queue size : ${samplesQueue.length}")
        //        ctx.self ! PlayBufferedSample
        working(
          id,
          sdl,
          nbSamples,
          sampleRate,
          playerGrabber,
          samplesQueue,
          newPlayedSamplesByte,
          lastAvailable,
          imageActorOpt,
          bytePerSecond
        )

      case TryPlaySoundTick =>
        //        debug(s"AudioPlayer-$id got PlayBufferedSample")
        //FIXME available is a sync func
        if (samplesQueue.size < 3) playerGrabber ! PlayerGrabber.AskSamples(Left(ctx.self))

        if (samplesQueue.nonEmpty) {
          val available = sdl.available()
//          val played = sdl.getMicrosecondPosition
          val newPlayedSamples = available - lastAvailable + playedSamplesByte

          val playedTime = newPlayedSamples * 1000000 / bytePerSecond // in us
          imageActorOpt.foreach(_ ! ImageActor.AudioPlayedTimeUpdated(playedTime))

          val newAvailable =
            if (available > samplesQueue.head.length) {
              val head = samplesQueue.dequeue
              //FIXME write is a sync func.
              sdl.write(head, 0, head.length)
              available - head.length
            } else {
              available
            }
          working(
            id,
            sdl,
            nbSamples,
            sampleRate,
            playerGrabber,
            samplesQueue,
            newPlayedSamples,
            newAvailable,
            imageActorOpt,
            bytePerSecond
          )
        } else {
          playerGrabber ! PlayerGrabber.AskSamples(Left(ctx.self))
//          log.warn(s"no samples in the soundQueue of SoundActor-$id !!!")
          Behaviors.same
        }

      case SoundFinish =>
        log.info(s"SoundActor-$id got SoundFinish.")
        imageActorOpt.foreach(_ ! ImageActor.AudioPlayedTimeUpdated(Long.MaxValue))
        timer.cancelAll()
        log.info(s"SoundActor-$id cancel Sound Timer.")
        try {
          if (sdl.isOpen) sdl.close()
          log.info(s"sdl-$id closed.")
        } catch {
          case ex: Exception =>
            log.error(s"sdl-$id close error: $ex")
        }
        Behaviors.stopped

      case x =>
        log.warn(s"unknown msg in working: $x")
        Behaviors.unhandled
    }


  }
}
