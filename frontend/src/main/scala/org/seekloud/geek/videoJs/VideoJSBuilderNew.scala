package org.seekloud.geek.videoJs
//
///**
//  * User: Jason
//  * Date: 2019/5/27
//  * Time: 14:51
//  */
//import com.karasiq.videojs.{Player, VideoJS, VideoJSOptions, VideoSource, VjsUtils}
//import org.scalajs.dom
//import org.scalajs.dom.Element
//
//import scala.scalajs.js
//import scala.scalajs.js.UndefOr
//import scala.xml.Elem
//
//case class VideoJSBuilderNew(sources: Seq[VideoSource] = Nil, controls: Boolean = true, autoplay: Boolean = false, loop: Boolean = false, fluid: Boolean = false, preload: String = "auto", poster: UndefOr[String] = js.undefined, width: UndefOr[Int] = js.undefined, height: UndefOr[Int] = js.undefined, techOrder: Seq[String] = Nil, readyHandlers: Seq[Player ⇒ Unit] = Nil, additional: Map[String, js.Any] = Map.empty) {
//  def sources(value: VideoSource*): VideoJSBuilderNew = copy(sources = value)
//  def controls(value: Boolean): VideoJSBuilderNew = copy(controls = value)
//  def autoplay(value: Boolean): VideoJSBuilderNew = copy(autoplay = value)
//  def loop(value: Boolean): VideoJSBuilderNew = copy(loop = value)
//  def fluid(value: Boolean): VideoJSBuilderNew = copy(fluid = value)
//  def preload(value: String): VideoJSBuilderNew = copy(preload = value)
//  def poster(value: String): VideoJSBuilderNew = copy(poster = value)
//  def dimensions(width: Int, height: Int): VideoJSBuilderNew = copy(width = width, height = height)
//  def width(value: Int): VideoJSBuilderNew = copy(width = value)
//  def height(value: Int): VideoJSBuilderNew = copy(height = value)
//  def techOrder(value: String*): VideoJSBuilderNew = copy(techOrder = value)
//  def ready(value: Player ⇒ Unit): VideoJSBuilderNew = copy(readyHandlers = readyHandlers :+ value)
//  def options(opts: (String, js.Any)*): VideoJSBuilderNew = copy(additional = additional ++ opts)
//
//  def build(): dom.Element = {
//    val videoContainer = dom.document.createElement("video")
//    videoContainer.setAttribute("class", "video-js")
//    val wrapper = dom.document.createElement("div")
//    wrapper.appendChild(videoContainer)
//
//    val settings = VideoJSOptions(sources, controls, autoplay, loop, fluid, preload, poster, width, height, techOrder, additional.toSeq)
//    VideoJS(videoContainer, settings, VjsUtils.ready(player ⇒ readyHandlers.foreach(_(player))))
//    wrapper
//  }
//  def buildElem(id: String): Elem = {
//    val elem =
//    <div style="float: left">
//      <video id={id} class ="video-js" style="width:750px;height:420px;">
//      </video>
//    </div>
////    val settings = VideoJSOptions(sources, controls, autoplay, loop, fluid, preload, poster, width, height, techOrder, additional.toSeq)
////    VideoJS(dom.document.getElementById(id), settings, VjsUtils.ready(player ⇒ readyHandlers.foreach(_(player))))
//    elem
//  }
//
//  def setting(videoContainer: Element): Unit = {
//    val settings = VideoJSOptions(sources, controls, autoplay, loop, fluid, preload, poster, width, height, techOrder, additional.toSeq)
//    VideoJS(videoContainer, settings, VjsUtils.ready(player ⇒ readyHandlers.foreach(_(player))))
//  }
//
//}