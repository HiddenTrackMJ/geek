package org.seekloud.geek.pages

import java.text.SimpleDateFormat

import mhtml.Var
import org.seekloud.geek.utils.{Http, JsFunc, Page}
import org.seekloud.geek.videoJs.VideoJSBuilderNew
import org.scalajs.dom
import org.seekloud.geek.Main
import org.seekloud.geek.common.Route
import org.seekloud.geek.videoJs._
import org.seekloud.geek.shared.ptcl.RoomProtocol.{GetRoomListReq, GetRoomListRsp, GetRoomSectionListReq, GetRoomSectionListRsp, RoomData, RoomInfoSection}
import io.circe.generic.auto._
import io.circe.syntax._
import org.seekloud.geek.pages.HomePage

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js.Date
import scala.xml.Elem

/**
  * User: xgy
  * Date: 2020/2/5
  * Time: 1:06
  */

class WatchRecord(roomID: Long,videoName :String) extends Page{
  override val locationHashString: String = s"#/room/$roomID/$videoName"

  var roomList: List[RoomData] = Main.roomList

  private val roomIdData: Var[List[RoomInfoSection]] = Var(Main.roomIdData)
  private val liveRoomInfo = Var(roomList)
  private val roomIdVar = Var(roomID)
  private val videoNameVar = Var(videoName)

  private def roomListRx(r: List[RoomInfoSection]) =
    <div class="col-md-3 col-sm-12 col-xs-12" style="background-color: #F5F5F5; margin-left:-11%;margin-right:1%;margin-top:2px;height:416px">
      <div class="x_title">
        <h2>录像列表</h2>
      </div>
      <ul class="list-unstyled top_profiles scroll-view">
        {
        getRoomItem(r, roomID)
        }
      </ul>
    </div>

  private def commentListRx(r: List[RoomInfoSection]) =
    <div class="col-md-3 col-sm-12 col-xs-12" style="background-color: #F5F5F5; margin-left:1%;margin-top:2px;height:416px">
      <div class="x_title">
        <h2>评论列表</h2>
      </div>
      <ul class="list-unstyled top_profiles scroll-view">
        {
        getCommitItem(r, roomID)
        }
      </ul>
    </div>

  private def commentRx =
    <div class="col-md-9" style="background-color: #F5F5F5; margin-left:1%;margin-top:2px;height:40px">
      <div class="comment-Video">
        <div class="commentTitle"></div>
        <input class="commentInput" placehoder="发个友善的评论">666</input>
        <div class="commentSubmit" onclick={() =>()}>发表评论</div>
      </div>
    </div>



  private def getRoomItem(roomList: List[RoomInfoSection], selected: Long) = {
    roomList.map { room =>
      val isSelected = if (room.roomId == selected) "actived playerSelect" else ""
      <li class={"media event eyesight " + isSelected} style="text-align:left" onclick={() => switchEyesight(room.roomId,room.fileName)}>
        <a class="pull-left border-aero profile_thumb">
          <img class="player" src={Route.imgPath("room.png")}></img>
        </a>
        <div class="media-body">
          <a class="title" href="javascript:void(0)">
            {"主讲人"+room.userName}({"会议号"+room.roomId})
          </a>
          <p>{"录像名："+room.fileName}</p>
          <p>{"保存录像时间："+room.time}</p>
        </div>
      </li>
    }
  }

  private def getCommitItem(roomList: List[RoomInfoSection], selected: Long) = {

    roomList.zipWithIndex.map { room=>
      val isSelected = if (room._1.roomId == selected) "" else ""
      <li class={"media event eyesight " + isSelected} style="text-align:left">

        <div class="media-body">
          {room._2 match {
          case 1=> <div><a class="title" href="javascript:void(0)">{"xue1"}({"2020-2-4 23:11:23"})</a><p>{"w t f?"}</p></div>
          case 2=> <div><a class="title" href="javascript:void(0)">{"xue2"}({"2020-2-5 13:15:03"})</a><p>{"666"}</p></div>
          case 3=> <div><a class="title" href="javascript:void(0)">{"xue2"}({"2020-2-5 14:32:45"})</a><p>{"666"}</p></div>
          case 4=> <div><a class="title" href="javascript:void(0)">{"xue3"}({"2020-2-5 14:45:05"})</a><p>{"什么鬼"}</p></div>
          case _=> <div></div>
        }}

        </div>
      </li>
    }
  }

  private def switchEyesight(roomId: Long,videoName: String): Unit ={
    renderTest(dom.document.getElementById("my-video"),roomIdVar.toString().drop(4).dropRight(1),videoNameVar.toString().drop(4).dropRight(1))
    dom.window.location.hash = s"#/room/$roomId/$videoName"
  }

  val container: Elem =
    VideoJSBuilderNew().buildElem("my-video")

  val background: Elem =
    <div id="home">
      <div class="home-wrapper"  >
        <div style="margin-left:15%;">
          <div class="x_content" >
            {
            roomIdData.map( l =>
              if (l.isEmpty) <div>{roomListRx(l)}{container}{commentListRx(l)}{commentRx}</div>
              else {
//                dom.window.setTimeout(()=>renderLive(dom.document.getElementById("my-video"), l.filter(_.roomId == roomID).head.url), 1000)
                <div>{roomListRx(l)}{container}{commentListRx(l)}{commentRx}</div>
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

  def getCommentList: Unit = {
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

  def getRoomSecList(): Unit ={
    val userId = dom.window.localStorage.getItem("userId").toLong
    Http.postJsonAndParse[GetRoomSectionListRsp](Route.Room.getRoomSectionList, GetRoomSectionListReq(userId).asJson.noSpaces).map {
      rsp =>
        if(rsp.errCode == 0) {
          roomIdData := rsp.roomList
        } else {
          println(rsp.msg)
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
//    getRoomList
    getRoomSecList()
    <div >
      {background}
    </div>

  }
}
