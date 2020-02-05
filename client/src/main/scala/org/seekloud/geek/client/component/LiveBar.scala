package org.seekloud.geek.client.component

import javafx.animation.AnimationTimer
import javafx.geometry.{Insets, Pos}
import javafx.scene.Node
import javafx.scene.control.{Button, ToggleButton, Tooltip}
import javafx.scene.image.ImageView
import javafx.scene.layout.{HBox, Priority, VBox}
import javafx.scene.text.{Font, Text}
import org.seekloud.geek.client.common.Constants.WindowStatus

/**
  * Author: zwq
  * Date: 2019/9/19
  * Time: 10:38
  * Description: 屏幕下方的功能条：设备准备中，音频、视频选择，全屏
  */
class LiveBar(val windowStatus: Int, width: Double, height: Double, recDuration: Option[String] = None) {

  /**
    * live & record & play
    */

  protected var startLiveTime: Long = 0L  //开始直播的时间
  protected var startRecTime: Long = 0L   //开始录像的时间
  protected var startPlayTime: Long = 0L  //开始播放录像或直播的时间

  var hasplayedTime: Long = 0l  //点击继续播放之前，已经播放过的时间
  var showedPlayTime: Long = 0l           //显示在进度条上的时间

  protected var animationTimerStart = false

  var isLiving = false
  var isRecording = false
  var isPlaying = false

  val liveTimeText = new Text("设备准备中")
  liveTimeText.setFont(Font.font(15))
  liveTimeText.setWrappingWidth(100)

  val recTimeText = new Text("设备准备中")
  recTimeText.setFont(Font.font(15))
  recTimeText.setWrappingWidth(100)

  val playTimeText = new Text("播放准备中")
  playTimeText.setFont(Font.font(15))
  if(windowStatus == WindowStatus.AUDIENCE_REC) playTimeText.setWrappingWidth(130) else playTimeText.setWrappingWidth(200)

  val liveToggleButton = new ToggleButton("")
  liveToggleButton.getStyleClass.add("hostScene-rightArea-liveBtn")
  liveToggleButton.setDisable(true)
  Tooltip.install(liveToggleButton, new Tooltip("点击开始直播"))

  val recordToggleButton = new ToggleButton("")
  recordToggleButton.getStyleClass.add("hostScene-rightArea-recordBtn")
  recordToggleButton.setDisable(true)
  Tooltip.install(recordToggleButton, new Tooltip("点击开始录像"))

  val playToggleButton = new ToggleButton("")
  playToggleButton.getStyleClass.add("audienceScene-rightArea-playBtn")
  playToggleButton.setSelected(true)
  Tooltip.install(playToggleButton, new Tooltip("点击暂停"))

  val forwardButton = new Button("")
  forwardButton.setMaxWidth(29)
  forwardButton.getStyleClass.add("audienceScene-rightArea-forward")
  val backwardButton = new Button("")
  backwardButton.setMaxWidth(29)
  backwardButton.getStyleClass.add("audienceScene-rightArea-backward")


  private val animationTimer = new AnimationTimer() {
    override def handle(now: Long): Unit = {
      windowStatus match{
        case WindowStatus.HOST =>
          writeLiveTime()
          writeRecTime()
        case WindowStatus.AUDIENCE_REC =>
        //          writePlayTime()
        case _ =>
          //do nothing
      }

    }
  }

  def writeLiveTime(): Unit = {
    if(isLiving) {
      val liveTime = System.currentTimeMillis() - startLiveTime
      val hours = liveTime / 3600000
      val minutes = (liveTime % 3600000) / 60000
      val seconds = (liveTime % 60000) / 1000
      liveTimeText.setText(s"${hours.toInt}:${minutes.toInt}:${seconds.toInt}")
    } else{
      liveTimeText.setText(s"未直播")
    }


  }

  def writeRecTime(): Unit = {
    if(isRecording){
      val recTime = System.currentTimeMillis() - startRecTime
      val hours = recTime / 3600000
      val minutes = (recTime % 3600000) / 60000
      val seconds = (recTime % 60000) / 1000
      recTimeText.setText(s"${hours.toInt}:${minutes.toInt}:${seconds.toInt}")
    } else{
      recTimeText.setText(s"未录像")
    }

  }

//  def writePlayTime(): Unit = {
//    showedPlayTime = if(isPlaying){
//      hasplayedTime + System.currentTimeMillis() - startPlayTime
//    } else {
//      hasplayedTime
//    }
//    val hours = (showedPlayTime / 3600000).toInt
//    val minutes = ((showedPlayTime % 3600000) / 60000).toInt
//    val seconds = ((showedPlayTime % 60000) / 1000).toInt
//
//    playTimeText.setText(s"$hours:$minutes:$seconds / ${recDuration.getOrElse("")}")
//
//  }

  def startTimer(): Unit = {
    if(!animationTimerStart){
      animationTimer.start()
      animationTimerStart = true

      windowStatus match{
        case WindowStatus.AUDIENCE_REC =>
          resetStartPlayTime(System.currentTimeMillis())
        case _ =>
        //do nothing
      }
    }
  }

  def resetStartLiveTime(startTime: Long): Unit = {
    isLiving = true
    imageToggleButton.setDisable(true)
    soundToggleButton.setDisable(true)
    startLiveTime = startTime
  }

  def resetStartRecTime(startTime: Long): Unit = {
    isRecording = true
    startRecTime = startTime
  }

  def resetStartPlayTime(startTime:Long): Unit = {
    isPlaying = true
    startPlayTime = startTime
  }

  /**
    * needSound & needImage & fullScreen
    */

  val soundToggleButton = new ToggleButton("")
  soundToggleButton.getStyleClass.add("hostScene-rightArea-soundBtn")
  soundToggleButton.setSelected(true)
  soundToggleButton.setDisable(false)
  Tooltip.install(soundToggleButton, new Tooltip("点击关闭直播声音"))


  val imageToggleButton = new ToggleButton("")
  imageToggleButton.getStyleClass.add("hostScene-rightArea-imageBtn")
  imageToggleButton.setSelected(true)
  imageToggleButton.setDisable(false)
  Tooltip.install(imageToggleButton, new Tooltip("点击关闭直播画面"))

  val fullScreenIcon = new Button("", new ImageView("img/full-screen.png"))
  fullScreenIcon.setPrefSize(32, 32)


  /**
    * barBox
    */

  // live & record
  val liveBox = new HBox(5, liveToggleButton, liveTimeText)
  liveBox.setAlignment(Pos.CENTER)

  val recBox = new HBox(5, recordToggleButton, recTimeText)
  recBox.setAlignment(Pos.CENTER)

  val playBox = new HBox(backwardButton, playToggleButton, forwardButton, playTimeText)
  playBox.setAlignment(Pos.CENTER_LEFT)

  val box1 = new HBox(10, liveBox, recBox)
  box1.setPadding(new Insets(0,130,0,0))
  box1.setAlignment(Pos.CENTER)

  // needSound & needImage
  val box2 = new HBox(5, soundToggleButton, imageToggleButton)
  box2.setPadding(new Insets(0,50,0,0))

  val barBox: HBox = windowStatus match{
    case WindowStatus.HOST => new HBox(box1, box2, fullScreenIcon)
    case WindowStatus.AUDIENCE_LIVE => new HBox(box2, fullScreenIcon)
    case WindowStatus.AUDIENCE_REC => new HBox()
  }

  HBox.setHgrow(barBox, Priority.ALWAYS)

  windowStatus match{
    case WindowStatus.HOST =>
      barBox.setAlignment(Pos.CENTER)
      barBox.setPadding(new Insets(2,0,2,0))

    case WindowStatus.AUDIENCE_LIVE =>
      barBox.setAlignment(Pos.CENTER_RIGHT)
      barBox.setPadding(new Insets(2,5,2,0))

    case WindowStatus.AUDIENCE_REC =>
      barBox.setAlignment(Pos.CENTER_LEFT)
      barBox.setPadding(new Insets(2,0,2,0))
      barBox.setSpacing(10)
  }

  barBox.setPrefWidth(width)
  barBox.setMaxHeight(height)
  barBox.setStyle("-fx-background-color: #66808080")

  def addNode4Bar(): Unit = {
    barVBox.getChildren.addAll(barBox)
  }

  def add4Rec(node1: Node, node2: Node) : Unit = {
    barBox.getChildren.addAll(playBox, node1, node2, box2, fullScreenIcon)
  }

  val barVBox = new VBox(-1,barBox)
  barVBox.setAlignment(Pos.BOTTOM_LEFT)
  VBox.setVgrow(barVBox, Priority.ALWAYS)

}
