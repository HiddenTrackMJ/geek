package org.seekloud.geek.client.core.collector

import java.util.concurrent.{ScheduledFuture, ScheduledThreadPoolExecutor, TimeUnit}

import akka.actor.typed.scaladsl.{Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import org.seekloud.geek.capture.core.CaptureManager
import org.seekloud.geek.capture.protocol.Messages
import org.seekloud.geek.capture.protocol.Messages._
import org.seekloud.geek.capture.sdk.MediaCapture
import org.seekloud.geek.client.Boot
import org.seekloud.geek.client.core.RmManager
import org.seekloud.geek.shared.ptcl.CommonProtocol.ModeStatus
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * User: TangYaruo
  * Date: 2019/9/4
  * Time: 14:04
  * Description: rmManager -->> CaptureActor -->> captureManager
  */
object ClientCaptureActor {

  private val log = LoggerFactory.getLogger(this.getClass)

  type CaptureCommand = ReplyToCommand

  final case class StartEncode(rtmp: String) extends CaptureCommand

  final case class StopEncode(encoderType: EncoderType.Value) extends CaptureCommand

  final case object StopCapture extends CaptureCommand

  final case class GetMediaCapture(mediaCapture: MediaCapture) extends CaptureCommand

  /*drawer*/
  sealed trait DrawCommand

  final case class DrawImage(image: Image) extends DrawCommand

  //切换
  final case class SwitchMode(isJoin: Boolean, reset: () => Unit) extends DrawCommand with CaptureCommand

  final case class ReSet(reset: () => Unit, offOrOn: Boolean) extends DrawCommand

  final case object StopDraw extends DrawCommand

  private object ENCODE_RETRY_TIMER_KEY


  def create(frameRate: Int, gc: GraphicsContext, isJoin: Boolean, callBackFunc: Option[() => Unit] = None): Behavior[CaptureCommand] =
    Behaviors.setup[CaptureCommand] { ctx =>
      log.info("CaptureActor is starting...")
      Behaviors.withTimers[CaptureCommand] { implicit timer =>
        idle(frameRate, gc, isJoin, callBackFunc)
      }
    }

  private def idle(
    frameRate: Int,
    gc: GraphicsContext,
    isJoin: Boolean = false,
    callBackFunc: Option[() => Unit] = None,
    resetFunc: Option[() => Unit] = None,
    mediaCapture: Option[MediaCapture] = None,
    reqActor: Option[ActorRef[Messages.ReqCommand]] = None,
    loopExecutor: Option[ScheduledThreadPoolExecutor] = None,
    imageLoop: Option[ScheduledFuture[_]] = None,
    drawActor: Option[ActorRef[DrawCommand]] = None
  )(
    implicit timer: TimerScheduler[CaptureCommand]
  ): Behavior[CaptureCommand] =
    Behaviors.receive[CaptureCommand] { (ctx, msg) =>
      msg match {
        case msg: GetMediaCapture =>
          idle(frameRate, gc, isJoin, callBackFunc, resetFunc, Some(msg.mediaCapture), reqActor, loopExecutor, imageLoop, drawActor)

        case msg: CaptureStartSuccess =>
          log.info(s"MediaCapture start success!")
          CaptureManager.setLatestFrame()
          val drawActor = ctx.spawn(drawer(gc, isJoin), s"CaptureDrawer-${System.currentTimeMillis()}")
          val executor = new ScheduledThreadPoolExecutor(1)
          val askImageLoop = executor.scheduleAtFixedRate(
            () => {
              msg.manager ! Messages.AskImage
            },
            0,
            ((1000.0 / frameRate) * 1000).toLong,
            TimeUnit.MICROSECONDS
          )
          callBackFunc.foreach(func => func())
          idle(frameRate, gc, isJoin, callBackFunc, resetFunc, mediaCapture, Some(msg.manager), Some(executor), Some(askImageLoop), Some(drawActor))

        case msg: CannotAccessSound =>
          log.info(s"Sound unavailable.")
          Behaviors.same

        case msg: CannotAccessImage =>
          log.info(s"Image unavailable.")
          Behaviors.same

        case CaptureStartFailed =>
          log.info(s"Media capture start failed. Review your settings.")
          Behaviors.same

        case ManagerStopped =>
          log.info(s"Capture Manager stopped.")
          if (resetFunc.nonEmpty) {
            resetFunc.foreach(func => func())
            mediaCapture.foreach(_.start())
          }
          idle(frameRate, gc, isJoin, callBackFunc, None, mediaCapture, reqActor, loopExecutor, imageLoop, drawActor)

        case StreamCannotBeEncoded =>
          log.info(s"Stream cannot be encoded to mpegts.")
          Behaviors.same

        case CannotSaveToFile =>
          log.info(s"Stream cannot be save to file.")
          Behaviors.same

        case msg: ImageRsp =>
          drawActor.foreach(_ ! DrawImage(msg.latestImage.image))
          Behaviors.same

        case msg: SoundRsp => //no need yet
          Behaviors.same

        case NoImage =>
//          log.info(s"No images yet, try later.")
          Behaviors.same

        case NoSamples =>
          log.info(s"No sound yet, try later.")
          Behaviors.same

        case msg: StartEncode =>
//          msg.output match {
//            case Right(outputStream) =>
//              if (reqActor.nonEmpty) {
//                reqActor.foreach(_ ! StartEncodeStream(outputStream))
//              } else {
//                timer.startSingleTimer(ENCODE_RETRY_TIMER_KEY, msg, 500.millis)
//              }
//            case Left(file) =>
//              if (reqActor.nonEmpty) {
//                reqActor.foreach(_ ! StartEncodeFile(file))
//              } else {
//                timer.startSingleTimer(ENCODE_RETRY_TIMER_KEY, msg, 500.millis)
//              }
//          }
          if (reqActor.nonEmpty) {
            reqActor.foreach(_ ! StartEncodeRtmp(msg.rtmp))
          } else {
            timer.startSingleTimer(ENCODE_RETRY_TIMER_KEY, msg, 500.millis)
          }

          Behaviors.same

        case msg: StopEncode =>
//          msg.encoderType match {
//            case EncoderType.STREAM => reqActor.foreach(_ ! StopEncodeStream)
//            case EncoderType.FILE => reqActor.foreach(_ ! StopEncodeFile)
//          }

          reqActor.foreach(_ ! StopEncodeRtmp)
          Behaviors.same

        case msg: SwitchMode =>
          drawActor.foreach(t =>t ! msg)
          //todo 关掉摄像头的drawer
//          drawActor.foreach(_ ! StopDraw)
          Behaviors.same

        case StopCapture =>
          log.info(s"Media capture is stopping...")
          imageLoop.foreach(_.cancel(false))
          loopExecutor.foreach(_.shutdown())
          reqActor.foreach(_ ! StopMediaCapture)
          drawActor.foreach(_ ! StopDraw)
          Behaviors.stopped

        case x =>
          log.warn(s"unknown msg in idle: $x")
          Behaviors.unhandled
      }
    }


  private def drawer(
    gc: GraphicsContext,
    isJoin: Boolean,
    needImage: Boolean = true
  ): Behavior[DrawCommand] =
    Behaviors.receive[DrawCommand] { (ctx, msg) =>
      msg match {
        case msg: DrawImage =>
          //canvas和图像不一定成比例
          val canvas_all_width = gc.getCanvas.getWidth
          val canvas_all_height = gc.getCanvas.getHeight

          val image_w = msg.image.getWidth //图像的宽度
          val image_h = msg.image.getHeight //图像的高度

//          log.info("imgage宽度" + sWidth + "高度" + sHeight)
          if (needImage) {
            if (!isJoin) {//没有开始会议的时候
              Boot.addToPlatform {
                gc.drawImage(msg.image, 0.0, 0.0, canvas_all_width, canvas_all_height)
              }
            } else {
              //开启会议的时候，判断是自由模式还是发言模式
              Boot.addToPlatform {
                if (RmManager.roomInfo.get.modeStatus == ModeStatus.FREE){
                  if (RmManager.userInfo.get.isHost.get){
                    //画在左侧
                    val canvas_distribute_w = canvas_all_width /2
                    val canvas_distribute_h = canvas_all_height //画在左侧占用整个左侧的画布，以画布的宽为基准，因为画布的宽一定是小于高的
                    //为了保险可以做个画布宽高判断，以便确定以谁为基准
                    if (canvas_distribute_w < canvas_distribute_h){//以宽度为基准
                      val canvas_last_h = canvas_distribute_w * image_h / image_w
                      val x = 0
                      val y = (canvas_distribute_h - canvas_last_h) / 2
                      gc.drawImage(msg.image, x, y, canvas_distribute_w, canvas_last_h)
                    }else{//以高为基准
                      val canvas_last_w = canvas_distribute_h * image_w / image_h
                      val x = (canvas_distribute_w  - canvas_last_w) / 2
                      val y = 0
                      gc.drawImage(msg.image, x, y,canvas_last_w, canvas_distribute_h)
                    }
                  }else{
                    //画在右侧第一格
                    val canvas_distribute_w = canvas_all_width /4 //左侧一半，右侧是4格，每行两格，一共4格两行
                    val canvas_distribute_h = (canvas_all_height - 0) /4  //这里可以sHeight-1/4高度，那么绘画起点就可以从0 -> 1/8 height的位置
                    if (canvas_distribute_w < canvas_distribute_h){//以宽度为基准
                      val canvas_last_h = canvas_distribute_w * image_h / image_w
                      val x = canvas_all_width / 2 + 0
                      val y = canvas_distribute_h - canvas_last_h / 2
                      gc.drawImage(msg.image, x, y,canvas_distribute_w, canvas_last_h)
                    }else{


                      gc.drawImage(msg.image, 0.0, canvas_all_height / 4, canvas_all_width / 2, canvas_all_height / 2)
                    }
                  }
                }else{//发言模式下
                  if (RmManager.userInfo.get.isAllow.get){
                    gc.drawImage(msg.image, 0.0, canvas_all_height / 4, canvas_all_width / 2, canvas_all_height / 2)
                  }else{
                    gc.drawImage(msg.image, 0.0, canvas_all_height / 4, canvas_all_width / 2, canvas_all_height / 2)
                  }
                }
              }

            }
          }
          Behaviors.same

        case msg: SwitchMode =>
          log.debug(s"Capture Drawer switch mode.")
          CaptureManager.setLatestFrame()
          Boot.addToPlatform (msg.reset())
//          Behaviors.same
          drawer(gc, msg.isJoin, needImage)


        case msg: ReSet =>
          log.info("drawer reset")
          Boot.addToPlatform(msg.reset())
          drawer(gc, isJoin, !msg.offOrOn)

        case StopDraw =>
          log.info(s"Capture Drawer stopped.")
          Behaviors.stopped

        case x =>
          log.warn(s"unknown msg in drawer: $x")
          Behaviors.unhandled
      }
    }


}
