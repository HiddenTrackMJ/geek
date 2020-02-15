package org.seekloud.geek

import scala.scalajs.js
import scala.scalajs.js.annotation.{ScalaJSDefined, _}
import scala.scalajs.js.{UndefOr, |}
import com.karasiq.videojs._
import com.karasiq.videojs._
import org.scalajs.dom
import org.scalajs.dom.Element
import org.seekloud.geek.common.Route
//import org.seekloud.geek.front.common.Route
//import org.seekloud.piscu.front.common.Route

import scala.language.postfixOps
import scala.scalajs.js.JSApp
import scala.scalajs.js.annotation.JSExport
import scala.xml.Elem

/**
  * User: Jason
  * Date: 2019/5/24
  * Time: 15:44
  */
package object videoJs {

  def renderWebm(): Element = {
    VideoJSBuilder()
      .sources(VideoSource("video/mp4", "http://vjs.zencdn.net/v/oceans.mp4"))
      .controls(true)
      .poster("http://www.webmfiles.org/wp-content/uploads/2010/05/webm-files.jpg")
      .dimensions(640, 360)
      .ready { player ⇒
        player.bigPlayButton.el().setAttribute("style", "color: red;")
        player.on("ended", () ⇒ dom.console.log("Video on ended"))
      }
      .build()
  }

  def renderTest(videoContainer: Element,userId:String,videoName:String): Unit = {
    VideoJSBuilderNew()
      .sources(VideoSource("video/mp4", s"http://127.0.0.1:42075/geek/room/getRecord/${userId.toString}/${videoName.toString}"))
//      .sources(VideoSource("video/mp4", s"http://10.1.29.247:42075/geek/room/getRecord/${userId.toString}/${videoName.toString}"))
      .controls(true)
      .poster(Route.imgPath("black.jpg"))
      .dimensions(640, 360)
      .ready { player ⇒
        player.bigPlayButton.el().setAttribute("style", "color: red;")
        player.on("ended", () ⇒ dom.console.log("Video on ended"))
      }
      .setting(videoContainer)
  }
//  http://vjs.zencdn.net/v/oceans.mp4

  def renderLive(videoContainer: Element, url: String = "rtmp://10.1.29.245:1935/live/1000"): Unit = {
    VideoJSBuilderNew()
      .sources(VideoSource("rtmp/flv", url))
      .controls(true)
      .poster(Route.imgPath("background2.jpg"))
      .dimensions(750, 420)
      .ready { player ⇒
        player.bigPlayButton.el().setAttribute("style", "color: red;")
        player.on("ended", () ⇒ dom.console.log("Video on ended"))
      }
      .setting(videoContainer)
  }

//  def renderYoutube(): Element = {
//    VideoJSBuilder()
//      .techOrder("youtube")
//      .sources(VideoSource("video/youtube", "https://www.youtube.com/watch?v=YlRCsfzXAVk"))
//      .controls(true)
//      .dimensions(640, 360)
//      .ready { video ⇒
//        video.playbackRate(0.5)
//        video.play()
//      }
//      .options("iv_load_policy" → 1)
//      .build()
//  }

}
