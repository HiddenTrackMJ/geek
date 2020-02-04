package org.seekloud.geek.front.pages

import mhtml.Var
import org.seekloud.geek.utils.{Http, JsFunc, Page}
import org.seekloud.geek.videoJs.VideoJSBuilderNew
import org.scalajs.dom
import org.seekloud.geek.Main
import org.seekloud.geek.common.Route
import org.seekloud.geek.videoJs._
import org.seekloud.geek.shared.ptcl.RoomProtocol.{GetRoomListReq, GetRoomListRsp, RoomData}
import io.circe.generic.auto._
import io.circe.syntax._
import org.seekloud.geek.pages.HomePage

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.Elem

/**
  * User: xgy
  * Date: 2020/2/5
  * Time: 1:06
  */

class WatchRecord(roomID: Long) extends Page{
  override val locationHashString: String = s"#/room/$roomID"

  var roomList: List[RoomData] = Main.roomList

  private val liveRoomInfo = Var(roomList)


  private def roomListRx(r: List[RoomData]) =
    <div class="col-md-3 col-sm-12 col-xs-12" style="background-color: #F5F5F5; margin-left:1%;margin-top:2px;height:416px">
      <div class="x_title">
        <h2>录像列表</h2>
      </div>
      <ul class="list-unstyled top_profiles scroll-view">
        {
        getRoomItem(r, roomID)
        }
      </ul>
    </div>

  private def getRoomItem(roomList: List[RoomData], selected: Long) = {
    roomList.map { room =>
      val isSelected = if (room.roomId == selected) "actived playerSelect" else ""
      <li class={"media event eyesight " + isSelected} style="text-align:left" onclick={() => switchEyesight(room.roomId)}>
        <a class="pull-left border-aero profile_thumb">
          <img class="player" src={Route.imgPath("room.png")}></img>
        </a>
        <div class="media-body">
          <a class="title" href="javascript:void(0)">
            {room.roomInfo.roomName}({room.roomId})
          </a>
          <p>
            {room.roomInfo.des}
          </p>
        </div>
      </li>
    }
  }

  private def switchEyesight(roomId: Long): Unit ={
    //    renderLive(dom.document.getElementById("my-video"))
    dom.window.location.hash = s"#/room/$roomId"
  }

  val container: Elem =
    VideoJSBuilderNew().buildElem("my-video")

  val background: Elem =
    <div id="home">
      <div class="home-wrapper"  >
        <div style="margin-left:15%;">
          <div class="x_content" >
            {
            liveRoomInfo.map( l =>
              if (l.isEmpty) <div>{container}{roomListRx(l)}</div>
              else {
//                dom.window.setTimeout(()=>renderLive(dom.document.getElementById("my-video"), l.filter(_.roomId == roomID).head.url), 1000)
                <div>{container}{roomListRx(l)}</div>
              }
            )
            }
          </div>
        </div>
      </div>

    </div>

  def getRoomList: Future[Unit] = {
    val url = Route.Room.getRoomList
    val data = GetRoomListReq().asJson.noSpaces
    Http.postJsonAndParse[GetRoomListRsp](url, data).map {
      rsp =>
        try {
          if (rsp.errCode == 0) {
            roomList = rsp.roomList
            liveRoomInfo := roomList
            println(s"got it : $rsp")
          }
          else {
            println("error======" + rsp.msg)
            JsFunc.alert(rsp.msg)
          }
        }
        catch {
          case e: Exception =>
            println(e)
        }
    }
  }

  def init(): Unit = {
    dom.document.body.style = "background-image: url('/geek/static/img/blueSky.jpg');" +
      "background-attachment: fixed;" +
      "background-size: cover;" +
      "background-position: center;" +
      "background-repeat: no-repeat;"
  }


  override def render: Elem = {
    HomePage.init()
    init()
    getRoomList
    <div >
      {background}
    </div>

  }
}
