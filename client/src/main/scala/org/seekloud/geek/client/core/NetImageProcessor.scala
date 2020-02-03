package org.seekloud.geek.client.core

import java.io.{File, FileInputStream, FileOutputStream}

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import javafx.scene.image.Image
import org.seekloud.geek.client.common.{Constants, Pictures}
import org.seekloud.geek.client.utils.HestiaClient
import org.slf4j.LoggerFactory
import org.seekloud.geek.client.Boot.executor

import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}

/**
  * User: TangYaruo
  * Date: 2019/9/10
  * Time: 15:37
  * 获取网络图片
  */
object NetImageProcessor {

  private val log = LoggerFactory.getLogger(this.getClass)

  sealed trait Command

  private case class TimeOut(msg: String) extends Command

  private final case object BehaviorChangeKey

  private final case class SwitchBehavior(
    name: String,
    behavior: Behavior[Command],
    durationOpt: Option[FiniteDuration] = None,
    timeOut: TimeOut = TimeOut("busy time error")
  ) extends Command

  private[this] def switchBehavior(ctx: ActorContext[Command],
    behaviorName: String, behavior: Behavior[Command], durationOpt: Option[FiniteDuration] = None, timeOut: TimeOut = TimeOut("busy time error"))
    (implicit stashBuffer: StashBuffer[Command],
      timer: TimerScheduler[Command]) = {
    timer.cancel(BehaviorChangeKey)
    durationOpt.foreach(timer.startSingleTimer(BehaviorChangeKey, timeOut, _))
    stashBuffer.unstashAll(ctx, behavior)
  }

  final case class GetNetImage(url: String) extends Command

  def create(): Behavior[Command] =
    Behaviors.setup[Command] { ctx =>
      log.info(s"NetImageProcessor is starting...")
      implicit val stashBuffer: StashBuffer[Command] = StashBuffer[Command](Int.MaxValue)
      Behaviors.withTimers[Command] { implicit timer =>
        idle()
      }
    }


  private def idle()(
    implicit timer: TimerScheduler[Command],
    stashBuffer: StashBuffer[Command]
  ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {

        case msg: GetNetImage =>
          //获取到之后缓存在用户目录下的.theia/pcClient/images文件夹下
          HestiaClient.getHestiaImage(msg.url).onComplete {
            case Success(rst) =>
              rst match {
                case Right(content) =>
                  val cachePath = Constants.imageCachePath
                  val fileName = msg.url.split("/").last
                  val cacheFile = new File(cachePath, fileName)
                  if (!cacheFile.exists()) {
                    cacheFile.createNewFile()
                  }

                  val outputStream = new FileOutputStream(cacheFile)
                  outputStream.write(content)
                  outputStream.close()

                  val image = new Image(new FileInputStream(cacheFile))
                  Pictures.loadingImages.filter(_._1 == msg.url).foreach(_._2.setImage(image))
                  Pictures.loadingImages = Pictures.loadingImages.filterNot(_._1 == msg.url)
                  Pictures.pictureMap.put(msg.url.split("/").last, image)

                case Left(error) =>
                  log.error(s"getHestiaImage ${msg.url} error: $error")
              }
              ctx.self ! SwitchBehavior("idle", idle())
            case Failure(ex) =>
              log.error(s"getHestiaImage ${msg.url} future failed: $ex")
              ctx.self ! SwitchBehavior("idle", idle())
          }
          busy()


        case x =>
          log.warn(s"unknown msg in idle: $x")
          Behaviors.unhandled
      }
    }

  private def busy()
    (
      implicit timer: TimerScheduler[Command],
      stashBuffer: StashBuffer[Command]
    ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case SwitchBehavior(name, b, durationOpt, timeOut) =>
          switchBehavior(ctx, name, b, durationOpt, timeOut)

        case TimeOut(m) =>
          log.debug(s"${ctx.self.path} is time out when busy, msg=$m")
          Behaviors.stopped

        case x =>
          stashBuffer.stash(x)
          Behavior.same

      }
    }


}
