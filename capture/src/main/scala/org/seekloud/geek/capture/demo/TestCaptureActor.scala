package org.seekloud.geek.capture.demo

import java.util.concurrent.{ScheduledThreadPoolExecutor, TimeUnit}

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import javafx.scene.canvas.GraphicsContext
import org.seekloud.geek.capture.Boot
import org.seekloud.geek.capture.core.CaptureManager
import org.seekloud.geek.capture.protocol.Messages
import org.seekloud.geek.capture.protocol.Messages._
import org.seekloud.geek.capture.Boot.{executor, system}
import org.slf4j.LoggerFactory

import concurrent.duration._
import language.postfixOps

/**
  * User: TangYaruo
  * Date: 2019/9/3
  * Time: 20:36
  */
object TestCaptureActor {

  private val log = LoggerFactory.getLogger(this.getClass)

  type TestCommand = Messages.ReplyToCommand

  private case object STOP_ENCODE_TIMER_KEY

  def create(gc: GraphicsContext): Behavior[TestCommand] =
    Behaviors.setup[TestCommand] { _ =>
      log.info(s"TestActor is starting...")
      idle(gc)
    }

  //replyTo对接收到的信息进行处理
  private def idle(gc: GraphicsContext): Behavior[TestCommand] =
    Behaviors.receive[TestCommand] { (ctx, msg) =>
      msg match {

        case msg: CaptureStartSuccess =>
          log.info(s"media capture start success!")
          val testAskLoop = new ScheduledThreadPoolExecutor(1)
          //循环请求图像
          val loop = testAskLoop.scheduleAtFixedRate(
            () => {
              msg.manager ! Messages.AskImage
            },
            0,
            ((1000.0 / 30) * 1000).toLong,
            TimeUnit.MICROSECONDS
          )

          system.scheduler.scheduleOnce(30.seconds, new Runnable {
            override def run(): Unit = {
//              msg.manager ! Messages.StopEncodeFile
//              msg.manager ! Messages.StopEncodeFile
            }
          })
          Behaviors.same

        case msg: CannotAccessSound =>
          log.info(s"Sound unavailable.")
          Behaviors.same

        case msg: CannotAccessImage =>
          log.info(s"Image unavailable.")
          Behaviors.same

        case CaptureStartFailed =>
          log.info(s"Media capture start failed. Review your settings.")
          Behaviors.same

        case StreamCannotBeEncoded =>
          log.info(s"Stream cannot be encoded to mpegts.")
          Behaviors.same

        case CannotSaveToFile =>
          log.info(s"Stream cannot be save to file.")
          Behaviors.same

        case msg: ImageRsp =>
          Boot.addToPlatform {
            //在画布上绘制获得图像，显示成视频
            gc.drawImage(msg.latestImage.image, 0, 0, gc.getCanvas.getWidth, gc.getCanvas.getHeight)
          }
          Behaviors.same

        case msg: SoundRsp =>
          Behaviors.same

        case NoImage =>
//          log.info(s"No images yet, try later.")
          Behaviors.same

        case NoSamples =>
          log.info(s"No sound yet, try later.")
          Behaviors.same

        case x =>
          log.warn(s"unknown msg in idle: $x")
          Behaviors.unhandled
      }
    }


}
