package org.seekloud.geek.player.core

import akka.actor.typed.scaladsl.{Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import javafx.application.Platform
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import org.seekloud.geek.player.common.Constants
import org.seekloud.geek.player.protocol.Messages._
import org.seekloud.geek.player.sdk.MediaPlayer
import org.seekloud.geek.player.util.GCUtil
import org.slf4j.LoggerFactory

import scala.collection.immutable
import scala.concurrent.duration._

/**
  * Author: zwq
  * Date: 2019/8/28
  * Time: 21:59
  */
object ImageActor {

  private val log = LoggerFactory.getLogger(this.getClass)
  private var debug = true

  private def debug(str: String): Unit = {
    if (debug) log.debug(str)
  }

  trait ImageCmd

  final case object PausePlayImage extends ImageCmd

  final case object ContinuePlayImage extends ImageCmd

  final case class AudioPlayedTimeUpdated(audioPlayedTime: Long) extends ImageCmd // in us

  private final object TryPlayImageTick extends ImageCmd

  final object FRAME_RATE_TIMER_KEY

  final case object Count extends ImageCmd

  final case object TimerKey4Count

  private var frameCount = 0


  //init state
  private[this] var frameRate = -1
  private var hasPictureTs = false
  private var hasAudio = false
  private var needSound = true
  //  private var hasTimer = false
  //  val waiting = new Image("img/waiting.gif")

  def create(
    id: String,
    gc: GraphicsContext,
    playerGrabber: ActorRef[PlayerGrabber.MonitorCmd],
    _frameRate: Int,
    _hasPictureTs: Boolean,
    _hasAudio: Boolean,
    _needSound: Boolean,
    isDebug: Boolean = true
  ): Behavior[ImageCmd] = Behaviors.setup { _ =>

    log.info(s"ImageActor-$id is starting......")
    debug = isDebug
    frameRate = _frameRate
    hasPictureTs = _hasPictureTs
    hasAudio = _hasAudio
    needSound = _needSound
    debug(s"frameRate: $frameRate, timeBetweenFrames: ${(1000 / frameRate).millis}")
    Behaviors.withTimers[ImageCmd] { implicit timer =>
      //      log.info(s"start Image Timer in ImageActor-$id.")
      timer.startPeriodicTimer(
        FRAME_RATE_TIMER_KEY,
        TryPlayImageTick,
        (1000 / frameRate).millis //interval between two frames
      )
      timer.startPeriodicTimer(TimerKey4Count, Count, 1.seconds)
      //      hasTimer = true
      playing(id, gc, playerGrabber, immutable.Queue[AddPicture](), 0, 0L, 0L, 0L)
    }
  }


  def playing(
    id: String,
    gc: GraphicsContext,
    playerGrabber: ActorRef[PlayerGrabber.MonitorCmd],
    queue: immutable.Queue[AddPicture],
    playedImages: Int,
    lastPlayTimeInWallClock: Long,
    ImagePlayedTime: Long,
    audioPlayedTime: Long
  )(
    implicit timer: TimerScheduler[ImageCmd]
  ): Behavior[ImageCmd] = Behaviors.receive { (ctx, msg) =>

    msg match {
      case PausePlayImage =>
        //todo check detail 播放时长计算是否正确
        log.info(s"ImageActor-$id got PausePlay.")
        timer.cancel(FRAME_RATE_TIMER_KEY)
        //        hasTimer = false
        log.info(s"ImageActor-$id cancel Image Timer.")
        //给当前位置画一个没有摄像头的图像
        val user =  MediaPlayer.roomInfo.get.userList.find(_.userId == id.toLong)
        if (user nonEmpty){
          val position = user.get.position
          GCUtil.draw(gc,new Image(Constants.getAvatarSrc(user.get.headImgUrl)),position)
        }else{
          log.info("当前用户已经退出房间了")
        }

        Behaviors.same

      case ContinuePlayImage =>
        log.info(s"ImageActor-$id got ContinuePlay.")
        log.info(s"start Image Timer in ImageActor-$id.")
        timer.startPeriodicTimer(
          FRAME_RATE_TIMER_KEY,
          TryPlayImageTick,
          (1000 / frameRate).millis
        )
        //        hasTimer = true
        Behaviors.same

      case m: AddPicture =>
        //        debug(s"PicturePlayActor got $m")
        //        Behaviors.same
        val newQueue = queue.enqueue(m)

        //        println(s"ImageActor get picture, queue size : ${newQueue.length}")
        playing(
          id,
          gc,
          playerGrabber,
          newQueue,
          playedImages,
          lastPlayTimeInWallClock,
          ImagePlayedTime,
          audioPlayedTime
        )

      case AudioPlayedTimeUpdated(apt) =>

        //        debug(s"--------------- audioTime[$apt] - videoTime[$videoPlayedTime] = ${apt - videoPlayedTime}")
        playing(
          id,
          gc,
          playerGrabber,
          queue,
          playedImages,
          lastPlayTimeInWallClock,
          ImagePlayedTime,
          apt
        )

      case TryPlayImageTick =>
        //        playerGrabber ! PlayerGrabber.AskPicture(Left(ctx.self))
        //        Behaviors.same

        if (queue.length < 2) playerGrabber ! PlayerGrabber.AskPicture(Left(ctx.self))

        //        debug(s"TryPlayTick: vt[$videoPlayedTime] - at[$audioPlayedTime] = ${videoPlayedTime - audioPlayedTime}, playedImages[$playedImages]")

        if (needSound && hasAudio && ImagePlayedTime - audioPlayedTime > 50000) {
          //skip picture play, do nothing.
          //          println("skip image")
          Behaviors.same
        } else {
          //          if (needSound && hasAudio && (audioPlayedTime != Long.MaxValue) && audioPlayedTime - ImagePlayedTime > 50000) {
          ////            debug(s"audioPlayedTime: $audioPlayedTime, imagePlayedTime: $ImagePlayedTime, diff: ${audioPlayedTime - ImagePlayedTime}")
          //            ctx.self ! TryPlayImageTick
          //          }
          if (queue.nonEmpty) {
            if (needSound && hasAudio && (audioPlayedTime != Long.MaxValue) && audioPlayedTime - ImagePlayedTime > 50000) {
              //            debug(s"audioPlayedTime: $audioPlayedTime, imagePlayedTime: $ImagePlayedTime, diff: ${audioPlayedTime - ImagePlayedTime}")
              //              println("quick run image")
              ctx.self ! TryPlayImageTick
            }
            //            println("draw image")
            val (newQueue, newImagePlayedTime, playTimeInWallClock) = drawPicture(id, gc, queue, ImagePlayedTime)
            playing(
              id,
              gc,
              playerGrabber,
              newQueue,
              playedImages + 1,
              playTimeInWallClock,
              newImagePlayedTime,
              audioPlayedTime
            )
          } else {
            //            println("ask image")
            playerGrabber ! PlayerGrabber.AskPicture(Left(ctx.self))
            //            log.warn(s"no pic in the imageQueue of ImageActor-$id!!!")
            Behaviors.same
          }

        }

      case msg: PictureFinish =>
        log.info(s"ImageActor-$id got PictureFinish")
        timer.cancelAll()
        msg.resetFunc.foreach(f => f())
        //        hasTimer = false
        log.info(s"ImageActor-$id cancel Image Timer.")
        Behaviors.stopped

      case Count =>
        //        log.info(s"frameRate = $frameCount")
        frameCount = 0
        Behaviors.same

      case x =>
        log.warn(s"unknown msg in playing: $x")
        Behaviors.unhandled
    }

  }


  private def drawPicture(id: String, gc: GraphicsContext, queue: immutable.Queue[AddPicture], imagePlayedTime: Long) = {
    //draw picture
    //    val (AddPicture(img, pictureTs), newQueue) = queue.dequeue
    frameCount += 1
    val res = queue.dequeue
    val img = res._1.img
    val newQueue = res._2
    val playTimeInWallClock = System.currentTimeMillis() //实际播放时间
    Platform.runLater { () =>
      //根据需要拉流的map里面的用户身份画到画布的不同位置

      //      log.info("房间信息" + MediaPlayer.roomInfo)
      //      log.info("用户id" + id)
      //id的值是当前拉流的用户userId
      val user =  MediaPlayer.roomInfo.get.userList.find(_.userId == id.toLong)
      if (user nonEmpty){
        val position = user.get.position
        GCUtil.draw(gc,img,position)
      }else{
        log.info("当前用户已经退出房间了")
      }

    }
    val newImagePlayedTime = //时间戳
      if (hasPictureTs) {
        res._1.timestamp
      } else {
        imagePlayedTime + (1000000 / frameRate)
      }
    (newQueue, newImagePlayedTime, playTimeInWallClock)
  }

  //  private def drawWaiting(gc: GraphicsContext): Unit = {
  //    val sWidth = gc.getCanvas.getWidth
  //    val sHeight = gc.getCanvas.getHeight
  //    gc.drawImage(waiting, sWidth/2 +25, sHeight/2 + 25, 50, 50)
  //  }

}
