package org.seekloud.geek.player.sdk

import java.io.{File, InputStream}

import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.{ActorRef, DispatcherSelector}
import akka.actor.{ActorSystem, Scheduler}
import akka.dispatch.MessageDispatcher
import akka.stream.ActorMaterializer
import akka.util.Timeout
import javafx.scene.canvas.GraphicsContext
import org.seekloud.geek.player.core.PlayerManager
import org.seekloud.geek.player.core.PlayerManager.MediaSettings
import org.seekloud.geek.player.protocol.Messages
import org.seekloud.geek.shared.ptcl.CommonProtocol.{RoomInfo, UserInfo}
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * User: TangYaruo
  * Date: 2019/8/28
  * Time: 14:25
  *
  *
  * 媒体流播放SDK - 非阻塞式交互
  * @author zwq
  */

object MediaPlayer {

  import org.seekloud.geek.player.common.AppSettings._

  val log = LoggerFactory.getLogger(this.getClass)

  implicit val system: ActorSystem = ActorSystem("PlayerSystem", config)
  implicit val executor: MessageDispatcher = system.dispatchers.lookup("akka.actor.my-blocking-dispatcher")
  val blockingDispatcher: DispatcherSelector = DispatcherSelector.fromConfig("akka.actor.my-blocking-dispatcher")

  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val scheduler: Scheduler = system.scheduler
  implicit val timeout: Timeout = Timeout(20 seconds)

  var playerManager : ActorRef[PlayerManager.SupervisorCmd] = _

  var roomInfo: Option[RoomInfo] = None

  def apply(): MediaPlayer = new MediaPlayer()

}

class MediaPlayer (
  roomInfo: Option[RoomInfo] = None,
  currentUser:Option[UserInfo] = None
) {

  //初始化，这里拿到的是对象的地址，值会随着rmManger更新而更新
  MediaPlayer.roomInfo = roomInfo

  import MediaPlayer._

  private[this] val log = LoggerFactory.getLogger(this.getClass)

  log.info(s"MediaPlayer is starting...")

  private[this] var inputStream: InputStream = _
  private[this] var inputString: String = _

  /*画面配置*/
  private var imageWidth = 640
  private var imageHeight = 360
  private var frameRate = 30
  private var needImage = true

  /*声音配置*/
  private var needSound = true


  /*录制选项*/
  private var outputFile: Option[File] = None



  def setImageWidth(imageWidth: Int): Unit = {
    this.imageWidth = imageWidth
  }

  def getImageWidth: Int = this.imageWidth

  def setImageHeight(imageHeight: Int): Unit = {
    this.imageHeight = imageHeight
  }

  def getImageHeight: Int = this.imageHeight

  def setFrameRate(frameRate: Int): Unit = {
    this.frameRate = frameRate
  }

  def getFrameRate: Int = this.frameRate

  def needImage(needOrNot: Boolean): Unit = {
    this.needImage = needOrNot
  }

  def isImageNeeded: Boolean = this.needImage

  def needSound(needOrNot: Boolean): Unit = {
    this.needSound = needOrNot
  }

  def isSoundNeeded: Boolean = this.needSound

  def setOutputFile(file: File): Unit = {
    this.outputFile = Some(file)
  }

  def getOutputFileName: Option[String] = this.outputFile.map(_.getName)


  /**
    * 初始化
    *
    * */
  def init(isDebug: Boolean = true, needTimestamp: Boolean = true): Unit = {

    if(playerManager == null){
      playerManager = system.spawn(PlayerManager.create(isDebug, needTimestamp), "playerManager")
    }
  }

  /**
    * 开始播放
    *
    * */
  def start(playId: String, userId: String, replyTo: ActorRef[Messages.RTCommand], input:Either[String, InputStream], graphContext: Option[GraphicsContext], mediaSettings: Option[MediaSettings] = None): Unit = {
    log.info(s"开始播放器playerId:$playId")
    if(mediaSettings.isEmpty){
      playerManager ! PlayerManager.StartPlay(playId, replyTo, graphContext, input, MediaSettings(imageWidth, imageHeight, frameRate, needImage, needSound, outputFile), userId)
    } else{
      playerManager ! PlayerManager.StartPlay(playId, replyTo, graphContext, input, mediaSettings.get, userId)

    }

  }

  /**
    * 暂停播放
    *
    * */
  def pause(playId: String): Unit = {
    if(playerManager != null){
      playerManager ! PlayerManager.PausePlay(playId)
    }

  }

  /**
    * 继续播放
    *
    * */
  def continue(playId: String): Unit = {
    if(playerManager != null){
      playerManager ! PlayerManager.ContinuePlay(playId)
    }

  }


  /**
    * 停止播放
    *
    * */
  def stop(playId: String, resetFunc: () => Unit): Unit = {
    if(playerManager != null){
      log.info(s"停止playerId:$playId")
      playerManager ! PlayerManager.StopPlay(playId, resetFunc)

    }

  }

  /**
    * 开始录制
    *
    * */
  def startRecord(outFilePath: String) : Unit = {
    if(playerManager != null){
      playerManager ! PlayerManager.StartRec(outFilePath)

    }
  }

  /**
    * 结束录制
    *
    * */
  def stopRecord() : Unit = {
    if(playerManager != null){
      playerManager ! PlayerManager.StopRec()

    }
  }

  /*开启同步rtpServer时间戳*/
  def setTimeGetter(playId: String, func: () => Long): Unit = {
    if (playerManager != null) {
      playerManager ! PlayerManager.SetTimeGetter(playId, func)
    }
  }


}

