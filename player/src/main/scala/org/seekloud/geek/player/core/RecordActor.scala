package org.seekloud.geek.player.core

import java.io.{File, InputStream}
import java.util.concurrent.LinkedBlockingDeque

import akka.actor.typed.scaladsl.{Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import org.bytedeco.javacv.{FFmpegFrameGrabber, FFmpegFrameRecorder, Frame}
import org.seekloud.geek.player.core.PlayerManager.MediaSettings
import org.seekloud.geek.player.protocol.Messages
import org.seekloud.geek.player.sdk.MediaPlayer
import org.slf4j.LoggerFactory
import org.seekloud.geek.player.sdk.MediaPlayer.executor

import concurrent.duration._
import scala.collection.immutable
import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * Author: zwq
  * Date: 2019/9/4
  * Time: 16:56
  */
object RecordActor {

  private val log = LoggerFactory.getLogger(this.getClass)
  private var debug: Boolean = true

  private def debug(str: String): Unit = {
    if(debug) log.debug(str)
  }


  /*recordCmd*/
  sealed trait recordCmd

  final case class StartedGrabberAndRecorder(grabber: FFmpegFrameGrabber, recorder:FFmpegFrameRecorder, replyTo: ActorRef[Messages.RTCommand]) extends recordCmd

  final case class FailedStartRecGrabber(grabber: FFmpegFrameGrabber, replyTo: ActorRef[Messages.RTCommand], exception: Throwable) extends recordCmd

  final case class FailedStartRecorder(recorder: FFmpegFrameRecorder, replyTo: ActorRef[Messages.RTCommand], exception: Throwable) extends recordCmd

  final case object RecordingFinish extends  recordCmd

  final case object RecWorkerStopped extends recordCmd

  final case object StartRecord extends recordCmd with RecWorkCmd

  final case object StopRecord extends recordCmd with RecWorkCmd

  final case object StopRecWorkerTimeout extends recordCmd with RecWorkCmd

  final case object StopSelf extends recordCmd with RecWorkCmd


  /*worker*/
  sealed trait RecWorkCmd


  def create(
    playId: String,
    replyTo: ActorRef[Messages.RTCommand],
    input: Either[String, InputStream],
    supervisor: ActorRef[PlayerManager.SupervisorCmd],
    settings: MediaSettings,
    outFile: File,
    isDebug: Boolean = true
  ): Behavior[recordCmd] =
    Behaviors.setup[recordCmd] { ctx =>
      debug = isDebug
      val recGrabber = input match {
        case Left(rtmpString) =>
          new FFmpegFrameGrabber(rtmpString)
        case Right(inputStream) =>
          new FFmpegFrameGrabber(inputStream)
      }
      recGrabber.setFrameRate(settings.frameRate)
      Future {
        log.info(s"recGrabber-$playId is starting...")
        recGrabber.start()
        recGrabber
      }.onComplete {
        case Success(grab) => {
          log.info(s"recGrabber-$playId started success.")
          val recorder = new FFmpegFrameRecorder(outFile, recGrabber.getImageWidth, recGrabber.getImageHeight, recGrabber.getAudioChannels)
          recorder.setFormat("mpegts")
          /*recorder video settings*/
          recorder.setVideoOption("tune", "zerolatency")
          recorder.setVideoOption("preset", "ultrafast")
          recorder.setVideoOption("crf", "25")
//          recorder.setGopSize(10)
          recorder.setVideoBitrate(2000000)
//          recorder.setVideoBitrate(grab.getVideoBitrate)
          recorder.setFrameRate(30)
//          recorder.setFrameRate(grab.getFrameRate)

          /*recorder audio settings*/
          recorder.setAudioOption("crf", "0")
          recorder.setAudioQuality(0)
          recorder.setAudioBitrate(192000)
//          recorder.setAudioBitrate(grab.getAudioBitrate)
          recorder.setSampleRate(44100)
//          recorder.setSampleRate(grab.getSampleRate)
          recorder.setAudioChannels(2)
//          recorder.setAudioChannels(grab.getAudioChannels)
          Future {
            log.info(s"recorder-$playId is starting...")
            recorder.start()
            recorder
          }.onComplete{
            case Success(rec) =>
              log.info(s"recorder-$playId started success.")
              ctx.self ! StartedGrabberAndRecorder(grab, rec, replyTo)
            case Failure(exception) =>
              log.info(s"recorder-$playId start failed: $exception")
              ctx.self ! FailedStartRecorder(recorder, replyTo, exception)
          }
        }
        case Failure(exception) =>
          log.info(s"recGrabber-$playId start failed: $exception.")
          ctx.self ! FailedStartRecGrabber(recGrabber, replyTo, exception)
      }

      init(playId, supervisor)
    }



  private def init(
    id: String,
    supervisor: ActorRef[PlayerManager.SupervisorCmd],
  ): Behavior[recordCmd] =
    Behaviors.receive[recordCmd] { (ctx, msg) =>
      msg match {
        case StartedGrabberAndRecorder(grabber, recorder, replyTo) =>
          //创建worker
//          val pFQ = new java.util.concurrent.LinkedBlockingDeque[Frame]()
//          val sFQ = new java.util.concurrent.LinkedBlockingDeque[Frame]()
          supervisor ! PlayerManager.RecInitialed(id, replyTo)

          val workActor = ctx.spawn(worker(id, ctx.self, grabber, recorder), "worker", MediaPlayer.blockingDispatcher)
          ctx.self ! StartRecord

          Behaviors.withTimers[recordCmd] { implicit timer =>
            grabbingAndRecording(id, grabber, recorder, supervisor, workActor)
          }

        case FailedStartRecGrabber(grab, replyTo, ex) =>
          log.info(s"recGrabber-$id init failed: ${ex.getMessage}")
          supervisor ! PlayerManager.RecGrabberFailInit(id, replyTo, ex)
          Behaviors.stopped

        case FailedStartRecorder(recorder, replyTo, ex) =>
          log.info(s"recorder-$id init failed: ${ex.getMessage}")
          supervisor ! PlayerManager.RecorderFailInit(id, replyTo, ex)
          Behaviors.stopped

        case x =>
          log.warn(s"unknown msg in init: $x")
          Behaviors.unhandled

      }
    }


  private def grabbingAndRecording(
    id: String,
    grabber: FFmpegFrameGrabber,
    recorder: FFmpegFrameRecorder,
    supervisor: ActorRef[PlayerManager.SupervisorCmd],
    workActor: ActorRef[RecWorkCmd]
  )(
    implicit timer: TimerScheduler[recordCmd]
  ): Behavior[recordCmd] =
    Behaviors.receive[recordCmd] { (ctx, msg) =>
      msg match {
        case StartRecord =>
          workActor ! StartRecord
          log.info(s"recWorker start......")
          Behaviors.same

        case StopRecord =>
          log.info(s"grabbingAndRecording-$id got StopRecord.")
          workActor ! StopRecord
          timer.startSingleTimer(
            StopRecWorkerTimeout,
            StopRecWorkerTimeout,
            5.seconds
          )
          Behaviors.same

        case StopRecWorkerTimeout =>
          log.info(s"grabbingAndRecording-$id hard stop.")
          ctx.stop(workActor)
          timer.startSingleTimer(
            StopSelf,
            StopSelf,
            1.second
          )
          Behaviors.same

        case StopSelf =>
          log.info(s"grabbingAndRecording-$id hard stopped.")
          Behaviors.stopped

        case RecWorkerStopped =>
          log.info(s"RecWorker-$id is stopped.")
          Behaviors.stopped

        case RecordingFinish =>
          debug(s"grabbingAndRecording-$id got RecordingFinish.")
          workActor ! StopRecord
          Behaviors.same

        case x =>
          log.warn(s"unknown msg in grabbing: $x")
          Behaviors.unhandled
      }
    }



  /*work*/
  private def worker(
    id: String,
    monitor: ActorRef[recordCmd],
    grabber: FFmpegFrameGrabber,
    recorder:FFmpegFrameRecorder,
  ): Behavior[RecWorkCmd] = {
    debug(s"worker-$id is starting...")
    var recordFinish = false

    Behaviors.withTimers[RecWorkCmd] { timer =>
      debug(s"RecWorker-$id is prepared.")

      Behaviors.receive[RecWorkCmd] { (ctx, msg) =>
        msg match {
          case StartRecord =>
//            debug(s"recWorker-$id work.")
            if (!recordFinish) {
//              var recordImage = false
//              var recordSound = false
              val frame = grabber.grab()
              if(frame != null){
                if(frame.image != null){
                  recorder.setTimestamp(frame.timestamp)
                  recorder.record(frame)
                  debug(s"record ImageFrame:${frame.timestamp}")
                }
                if(frame.samples != null){
                  recorder.record(frame)
                  debug(s"record SamplesFrame:${frame.timestamp}")
                }

              } else{
                recordFinish = true
              }
              ctx.self ! StartRecord
            }
            else {
              log.info(s"record-$id finish: [$recordFinish]")
              monitor ! RecordingFinish
            }

            Behaviors.same

          case StopRecord =>
            log.info(s"recWorker-$id got StopRecord.")
            try {
              grabber.stop()
              grabber.close()
              grabber.release()
              monitor ! RecWorkerStopped
            } catch {
              case ex: Exception =>
                log.error(s"grabber-$id close error: $ex")
            }
            try{
              recorder.stop()
              recorder.close()
              recorder.release()
            } catch{
              case ex: Exception =>
                log.error(s"recorder-$id close error: $ex")
            }
            Behaviors.stopped

          case x =>
            log.warn(s"unknown msg in worker: $x")
            Behaviors.unhandled
        }
      }
    }
  }

}
