package org.seekloud.geek

import mhtml.{Cancelable, Rx, Var, emptyHTML, mount}
import org.scalajs.dom
import org.seekloud.geek.common.Route
import org.seekloud.geek.pages._
import org.seekloud.geek.utils.{Http, JsFunc, PageSwitcher}
import org.seekloud.geek.shared.ptcl.RoomProtocol._
import io.circe.generic.auto._
import io.circe.syntax._
import org.seekloud.geek.common.Route
import org.seekloud.geek.common.Route
import org.seekloud.geek.pages.WatchRecord
import org.seekloud.geek.utils.{Http, JsFunc, PageSwitcher}
import org.seekloud.geek.utils.{Http, JsFunc, PageSwitcher}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.xml.{Elem, Node}

/**
  * User: Jason
  * Date: 2019/5/24
  * Time: 15:44
  */
object Main extends PageSwitcher{

  var roomList: List[RoomData] = Nil
  var roomIdData: List[RoomInfoSection] = List()
  val numList = Var((1 to 5).toList)

  def modeOption(mode: Int): Rx[List[Elem]] = numList.map {
    _.map {
      options =>
        if (options.equals(mode))
          <option value={options.toString} selected="selected">
            {options}.createCourse-item input
          </option>
        else
          <option value={options.toString}>
            {options}
          </option>
    }
  }

  val currentPage: Rx[Node] = currentHashVar.map { ls =>
    println(s"currentPage change to ${ls.mkString(",")}")
    ls match {
      case "home" :: Nil => HomePage.render
      case "userInfo" :: Nil => UserInfoPage.render
      case "inviterManage" :: Nil => InviterManagement.render
      case "room" :: r :: v :: Nil => new WatchRecord(r.toLong,v).render
      case "login" :: Nil => Login.render
//      case "info" :: Nil => LiveHouse.render //fixme delete
      case _ => HomePage.render
    }

  }

  val header: Rx[Node] = currentHashVar.map {
    //    case "Login" :: Nil => <div></div>
    case "login" :: Nil => emptyHTML
//    case _ => Header.render
    case _ => Header.render
  }

  def show(): Cancelable = {
    switchPageByHash()

    val page =
      <div class="overlay">
        {header}
        {currentPage}
      </div>
    mount(dom.document.body, page)
  }

  def getRoomList(): Unit = {
    val url = Route.Room.getRoomList
    val data = GetRoomListReq().asJson.noSpaces
    Http.postJsonAndParse[GetRoomListRsp](url, data).map {
      rsp =>
        try {
          if (rsp.errCode == 0) {
            roomList = rsp.roomList
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
          roomIdData = rsp.roomList
        } else {
          println(rsp.msg)
        }
    }
  }

  def main(args: Array[String]): Unit ={
    show()

  }

}
