package org.seekloud.geek.capture.core

import java.nio.{ByteBuffer, ByteOrder, ShortBuffer}
import java.util.concurrent.{LinkedBlockingDeque, ScheduledFuture, ScheduledThreadPoolExecutor, TimeUnit}

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import javax.sound.sampled.TargetDataLine
import org.seekloud.geek.capture.protocol.Messages
import org.seekloud.geek.capture.protocol.Messages.{EncoderType, LatestSound}
import org.slf4j.LoggerFactory

import scala.collection.mutable

/**
  * User: TangYaruo
  * Date: 2019/8/28
  * Time: 17:47
  */
object SoundCapture {

  private val log = LoggerFactory.getLogger(this.getClass)
  var debug: Boolean = true

  def debug(msg: String): Unit = {
    if (debug) log.debug(msg)
  }

  sealed trait Command

  final case object StartSample extends Command

  final case object Sample extends Command

  final case object StopSample extends Command

  final case object AskSamples extends Command


  def create(
    replyTo: ActorRef[Messages.ReplyToCommand],
    line: TargetDataLine,
    encoders: mutable.HashMap[EncoderType.Value, ActorRef[EncodeActor.Command]],
    frameRate: Int,
    sampleRate: Float,
    channels: Int,
    sampleSize: Int,
    isDebug: Boolean
  ): Behavior[Command] =
    Behaviors.setup[Command] { ctx =>
      log.info(s"SoundCapture is staring...")
      debug = isDebug
      val audioBufferSize = sampleRate * channels
      val audioBytes = new Array[Byte](audioBufferSize.toInt)
      ctx.self ! StartSample
      working(replyTo, line, encoders, frameRate, sampleRate, channels, sampleSize, audioBytes)
    }


  private def working(
    replyTo: ActorRef[Messages.ReplyToCommand],
    line: TargetDataLine,
    encoders: mutable.HashMap[EncoderType.Value, ActorRef[EncodeActor.Command]],
    frameRate: Int,
    sampleRate: Float,
    channels: Int,
    sampleSize: Int,
    audioBytes: Array[Byte],
    audioExecutor: Option[ScheduledThreadPoolExecutor] = None,
    audioLoop: Option[ScheduledFuture[_]] = None,
    askFlag: Boolean = false,
  ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {

        case StartSample =>
          log.info(s"Media microphone started.")
          val audioExecutor = new ScheduledThreadPoolExecutor(1)
          val audioLoop =
            audioExecutor.scheduleAtFixedRate(
              () => {
                ctx.self ! Sample
              },
              0,
              ((1000 / frameRate) * 1000).toLong,
              TimeUnit.MICROSECONDS)
          working(replyTo, line, encoders, frameRate, sampleRate, channels, sampleSize, audioBytes, Some(audioExecutor), Some(audioLoop), askFlag)

        case Sample =>
          try {
            val nBytesRead = line.read(audioBytes, 0, line.available)
            val nSamplesRead = if (sampleSize == 16) nBytesRead / 2 else nBytesRead
            val samples = new Array[Short](nSamplesRead)
            sampleSize match {
              case 8 =>
                val shortBuff = ShortBuffer.wrap(audioBytes.map(_.toShort))
                debug(s"8-bit sample order: ${shortBuff.order()}")
                shortBuff.get(samples)
              case 16 =>
                ByteBuffer.wrap(audioBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer.get(samples)
              case _ => //invalid
            }

            val sp = ShortBuffer.wrap(samples, 0, nSamplesRead)
            if (askFlag) replyTo ! Messages.SoundRsp(LatestSound(sp, System.currentTimeMillis()))
            encoders.foreach(_._2 ! EncodeActor.EncodeSamples(sampleRate.toInt, channels, sp))
          } catch {
            case ex: Exception =>

              log.warn(s"sample sound error: $ex")
          }
          working(replyTo, line, encoders, frameRate, sampleRate, channels, sampleSize, audioBytes, audioExecutor, audioLoop, askFlag = false)

        case AskSamples =>
          working(replyTo, line, encoders, frameRate, sampleRate, channels, sampleSize, audioBytes, audioExecutor, audioLoop, askFlag = true)


        case StopSample =>
          log.info(s"Media microphone stopped.")
          audioLoop.foreach(_.cancel(false))
          audioExecutor.foreach(_.shutdown())
          line.stop()
          line.flush()
          line.close()
          Behaviors.stopped

        case x =>
          log.warn(s"unknown msg in working: $x")
          Behaviors.unhandled
      }
    }

}
