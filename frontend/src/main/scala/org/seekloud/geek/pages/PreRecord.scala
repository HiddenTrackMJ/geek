package org.seekloud.geek.pages

import org.scalajs.dom
import org.seekloud.geek.Main
import org.seekloud.geek.common.Route
import org.seekloud.geek.pages.Header.username
import org.seekloud.geek.pages.UserInfoPage.userDetail
import org.seekloud.geek.shared.ptcl.CommonProtocol.{CheckInviteeReq, Comment}
import org.seekloud.geek.shared.ptcl.RoomProtocol.{GetRoomSectionListReq, GetRoomSectionListRsp, RoomInfoSection}
import org.seekloud.geek.utils.{Http, JsFunc, Page}
import mhtml.Var

import scala.concurrent.ExecutionContext.Implicits.global
import io.circe.generic.auto._
import io.circe.syntax._
import org.seekloud.geek.shared.ptcl.SuccessRsp

import scala.xml.Elem

object PreRecord  {
  private val roomIdData: Var[List[RoomInfoSection]] = Var(Main.roomIdData)
//  private val CommentInfo: Var[Option[List[Comment]]] = Var(None)
//  private val userNameVar = Var("")
//  private val fileNameVar = Var("")
//  private val isinvitedVar = Var(false)

}
class PreRecord(c:String) extends Page{
  import PreRecord._

  def checkInvAndSkip(roomId:Long,fileName:String): Unit ={
    val userId = dom.window.localStorage.getItem("userId").toLong
    Http.postJsonAndParse[SuccessRsp](Route.Invitation.checkInvitee, CheckInviteeReq(userId,fileName).asJson.noSpaces).map {
      rsp =>
        if(rsp.errCode == 0) {
//          dom.document.location.href= s"#/room/"+roomId.toString + "/" + fileName
          dom.document.location.hash= s"#/room/"+roomId.toString + "/" + fileName.dropRight(4)
          println("ssssss")
        } else if(rsp.errCode == -1) {
          JsFunc.alert(rsp.msg)
          dom.document.location.hash= s"#/room/"+roomId.toString + "/" + fileName.dropRight(4)
          println("ssssss")
        }else{
          JsFunc.alert("抱歉，你没有查看该录像的权限")
          println(rsp.msg)
        }
    }

  }

  private val courseListRx1 =
    roomIdData.map {
    case Nil => <div style="margin: 30px; font-size: 17px; " class="courseTitleContainer">暂无录像信息
    </div>
    case list =>{<div class="courseContainer" >
      {list.sortBy(_.roomId).reverse.map { l =>

        <div class={if(l.roomId==1763 ||l.roomId==1762 ||l.roomId==1761) "courseItem2" else "courseItem"} onclick={()=>checkInvAndSkip(l.roomId,l.fileName);()}>
          <img class="courseItem-img" src={Route.imgPath("videoCover/video"+l.fileName.takeRight(5).replaceAll("mp4","png"))}></img>
          <div style="padding:0 20px">
            <div class="courseItem-title" onclick={() => ()}>
              <div class="courseItem-name">
                {l.fileName.split("_").last}
              </div>

            </div>
            <div class="courseItem-teacher">发起人：{l.userName}</div>
            <div class="courseItem-peopleNum">房间号：{l.roomId}</div>
            <div class="courseItem-peopleNum">房间描述：{if(l.desc.getOrElse("暂无")=="") "暂无" else l.desc.getOrElse("暂无")}</div>
            <div class="courseItem-peopleNum">发起时间：{l.time}</div>
          </div>
        </div>
      }}</div>
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

    override def render:Elem={
      HomePage.init()
      init()
      getRoomSecList()

      <div>
      <div style="display: flex;justify-content: center;margin-top: -2%;margin-bottom: 2%;">
        <div style="font-size: 25px;color: white;">全部录像</div>
      </div>
          {courseListRx1}
      </div>

    }
}
