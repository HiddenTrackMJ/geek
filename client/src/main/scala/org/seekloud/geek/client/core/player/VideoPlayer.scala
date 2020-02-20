package org.seekloud.geek.client.core.player

import akka.actor.typed.scaladsl.{Behaviors, StashBuffer, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import org.seekloud.geek.player.core.PlayerGrabber
import org.seekloud.geek.player.protocol.Messages
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{Behaviors, StashBuffer, TimerScheduler}
import javafx.scene.canvas.GraphicsContext
import org.seekloud.geek.player.core.PlayerGrabber
import org.seekloud.geek.player.protocol.Messages
import org.seekloud.geek.player.protocol.Messages._
import org.slf4j.LoggerFactory

import scala.collection.immutable
import scala.concurrent.duration._

/**
  * Author: zwq
  * Date: 2019/8/31
  * Time: 21:53
  */
object VideoPlayer {

  private val log = LoggerFactory.getLogger(this.getClass)

  type PlayCommand = Messages.RTCommand

  //videoPlayer to self
  final case class StartTimers(grabActor: ActorRef[PlayerGrabber.MonitorCmd]) extends PlayCommand

  final case object TryAskPicture extends PlayCommand

  final case object TryAskSamples extends PlayCommand

  // keys
  private object IMAGE_TIMER_KEY

  private object SOUND_TIMER_KEY

  //init
  var frameRate: Double = _
  var hasAudio: Boolean = _
  var hasVideo: Boolean = _
  var needImage: Boolean = _
  var needSound: Boolean = _

  def create(
    id: String,
    imageQueue: Option[immutable.Queue[AddPicture]] = None,
    samplesQueue: Option[immutable.Queue[Array[Byte]]] = None
  ): Behavior[PlayCommand] =
    Behaviors.setup[PlayCommand] { ctx =>
      log.info(s"VideoPlayer is starting......")
      implicit val stashBuffer: StashBuffer[PlayCommand] = StashBuffer[PlayCommand](Int.MaxValue)
      Behaviors.withTimers[PlayCommand] { implicit timer =>
        idle(id, imageQueue, samplesQueue)
      }
    }


  private def idle(
    id: String,
    imageQueue: Option[immutable.Queue[AddPicture]] = None,
    samplesQueue: Option[immutable.Queue[Array[Byte]]] = None
  ): Behavior[PlayCommand] =
    Behaviors.receive[PlayCommand] { (ctx, msg) =>
      msg match {
        case msg: GrabberInitialed =>
          log.info(s"VideoPlayer-$id got GrabberInitialed.")
          frameRate = msg.mediaInfo.frameRate
          hasAudio = msg.mediaInfo.hasAudio
          hasVideo = msg.mediaInfo.hasVideo
          needImage = msg.mediaSettings.needImage
          needSound = msg.mediaSettings.needSound
          val grabActor = msg.playerGrabber

          if (msg.gc.isEmpty) {
            ctx.self ! StartTimers(grabActor)
          } else {
            //            log.debug(s"Grabber-$id has initialed and played automatically.")
          }
          Behaviors.same

        case StartTimers(grabActor) =>
          Behaviors.withTimers[PlayCommand] { implicit timer =>
            var pF = true
            var sF = true
            if (needImage && hasVideo) {
              log.info(s"VideoPlayer-$id start ImageTimer.")
              timer.startPeriodicTimer(
                IMAGE_TIMER_KEY,
                TryAskPicture,
                (1000 / frameRate) millis
              )
              pF = false
            }
            if (needSound && hasAudio) {
              log.info(s"VideoPlayer-$id start SoundTimer.")
              timer.startPeriodicTimer(
                SOUND_TIMER_KEY,
                TryAskSamples,
                (1000 / frameRate) millis
              )
              sF = false
            }
            working(id, grabActor, imageQueue, samplesQueue, pF, sF)

          }

        case GrabberInitFailed(playId, ex) =>
          log.warn(s"VideoPlayer-$id got GrabberInitFailed:$ex")
          Behaviors.stopped

        case StopVideoPlayer =>
          log.info(s"VideoPlayer is stopped.")
          Behaviors.stopped

        case RecorderInitialed(playId) =>
          log.debug(s"record start: $playId")
          Behaviors.same

        case RecorderInitFailed(playId, ex) =>
          log.debug(s"recorder init failed：playId:$playId, ex:$ex")
          Behaviors.stopped

        case RecordStopped() =>
          log.debug(s"record stopped.")
          Behaviors.same

        case x =>
          log.warn(s"unknown msg in idle: $x")
          Behaviors.unhandled
      }
    }


  def working(
    id: String,
    grabActor: ActorRef[PlayerGrabber.MonitorCmd],
    imageQueue: Option[immutable.Queue[AddPicture]],
    samplesQueue: Option[immutable.Queue[Array[Byte]]],
    pictureFinish: Boolean = true,
    soundFinish: Boolean = true
  )(
    implicit timer: TimerScheduler[PlayCommand]
  ): Behavior[PlayCommand] = Behaviors.receive { (ctx, msg) =>

    msg match {
      case TryAskPicture =>
        grabActor ! PlayerGrabber.AskPicture(Right(ctx.self))
        log.debug(s"VideoPlayer-$id AskPicture to GrabActor.")
        Behaviors.same

      case TryAskSamples =>
        grabActor ! PlayerGrabber.AskSamples(Right(ctx.self))
        log.debug(s"VideoPlayer-$id AskSamples to GrabActor.")
        Behaviors.same

      case msg: AddPicture =>
        log.debug(s"VideoPlayer-$id got AddPicture.")
        val newImageQueue = imageQueue.map(_.enqueue(msg))
        working(id, grabActor, newImageQueue, samplesQueue, pictureFinish, soundFinish)
        Behaviors.same

      case AddSamples(samples, ts) =>
        log.debug(s"VideoPlayer-$id got AddSample.")
        val newSamplesQueue = samplesQueue.map(_.enqueue(samples))
        working(id, grabActor, imageQueue, newSamplesQueue, pictureFinish, soundFinish)
        Behaviors.same

      case msg: PictureFinish =>
        log.debug(s"VideoPlayer-$id got PictureFinish.")
//        msg.resetFunc.foreach(f => f())
        timer.cancel(IMAGE_TIMER_KEY)
        //todo: 重置画面
//        audienceScene.foreach(_.autoReset())
        log.info(s"VideoPlayer-$id cancel ImageTimer.")
        if (soundFinish) {
          Behaviors.stopped
        } else {
          working(id, grabActor, imageQueue, samplesQueue, pictureFinish = true, soundFinish = soundFinish)
        }


      case SoundFinish =>
        log.debug(s"VideoPlayer-$id got SoundFinish.")
        timer.cancel(SOUND_TIMER_KEY)
        log.info(s"VideoPlayer-$id cancel SoundTimer.")
        if (pictureFinish) {
          Behaviors.stopped
        } else {
          working(id, grabActor, imageQueue, samplesQueue, pictureFinish, soundFinish = true)
        }

      case PauseAsk =>
        log.info(s"VideoPlayer-$id got PauseAsk.")
        timer.cancelAll()
        log.info(s"cancel all Timers in VideoPlayer-$id.")
        Behaviors.same

      case ContinueAsk =>
        log.info(s"VideoPlayer-$id got ContinueAsk.")
        if (needImage && hasVideo) {
          log.info(s"VideoPlayer-$id start ImageTimer again.")
          timer.startPeriodicTimer(
            IMAGE_TIMER_KEY,
            TryAskPicture,
            (1000 / frameRate) millis
          )
        }
        if (needSound && hasAudio) {
          log.info(s"VideoPlayer-$id start SoundTimer again.")
          timer.startPeriodicTimer(
            SOUND_TIMER_KEY,
            TryAskSamples,
            (1000 / frameRate) millis
          )
        }
        Behaviors.same

      case StopVideoPlayer =>
        log.info(s"VideoPlayer-$id got StopSelf.")
        timer.cancelAll()
        log.info(s"VideoPlayer-$id cancel all Timers.")
        Behaviors.stopped

      case x =>
        log.warn(s"unknown msg in working: $x")
        Behaviors.unhandled

    }

  }


}
