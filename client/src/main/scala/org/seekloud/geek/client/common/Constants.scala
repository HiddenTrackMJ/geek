package org.seekloud.geek.client.common

import java.io.{File, FileInputStream}

import javafx.scene.image.Image
import javafx.scene.paint.Color


/**
  * User: Arrow
  * Date: 2019/7/16
  * Time: 14:54
  */
object Constants {

//  private val splitSymbol =

  val cachePath: String = s"${System.getProperty("user.home")}/.theia/pcClient"

  val cacheFile = new File(cachePath)

  if (!cacheFile.exists()) cacheFile.mkdirs()

  val imageCachePath: String = cachePath + "/images"
  val loginInfoCachePath: String = cachePath + "/login"
  val recordPath: String = System.getProperty("user.home") + "\\.theia\\pcClient\\record"

//  val imageCache = new File(imageCachePath)
//  if (!imageCache.exists()) {
//    imageCache.mkdirs()
//  } else {
//    imageCache.listFiles().foreach { file =>
//      val image = new Image(new FileInputStream(file))
//      Pictures.pictureMap.put(file.getName, image)
//    }
//  }

  //登录信息的临时文件
  val loginInfoCache = new File(loginInfoCachePath)
  if (!loginInfoCache.exists()) loginInfoCache.mkdirs()

  val record = new File(recordPath)
  if (!record.exists()) record.mkdirs()


  object StreamType extends Enumeration {
    val RTMP, RTP = Value
  }


  object AppWindow {
    val width = 1152
    val height = 864
  }
  object DefaultPlayer {
    val width = 640
    val height = 360
  }

  object HostStatus {
    val LIVE = 0
    val CONNECT = 1
  }

  object AudienceStatus {
    val LIVE = 0
    val CONNECT = 1
    val RECORD = 2
  }

  object WindowStatus{
    val HOST = 0
    val AUDIENCE_LIVE = 1
    val AUDIENCE_REC = 2
  }

  val barrageColors = List(
    Color.PINK,
    Color.HOTPINK,
    Color.WHITE,
    Color.RED,
    Color.ORANGE,
    Color.YELLOW,
    Color.GREEN,
    Color.CYAN,
    Color.BLUE,
    Color.PURPLE,
    Color.BROWN,
    Color.BURLYWOOD,
    Color.CHOCOLATE,
    Color.GOLD,
    Color.GREY
  )




}
