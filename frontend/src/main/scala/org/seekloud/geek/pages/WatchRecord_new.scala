package org.seekloud.geek.pages

import java.text.SimpleDateFormat

import mhtml.Var
import org.seekloud.geek.utils.{Http, JsFunc, Page}
//import org.seekloud.geek.videoJs.VideoJSBuilderNew
import org.scalajs.dom
import org.scalajs.dom.html.{Button, Input}
import org.seekloud.geek.Main
import org.seekloud.geek.common.Route
//import org.seekloud.geek.videoJs._
import org.seekloud.geek.shared.ptcl.RoomProtocol.{GetRoomListReq, GetRoomListRsp, GetRoomSectionListReq, GetRoomSectionListRsp, RoomData, RoomInfoSection}
import org.seekloud.geek.shared.ptcl.CommonProtocol._
import io.circe.generic.auto._
import io.circe.syntax._
//import org.seekloud.geek.pages.HomePage
import org.seekloud.geek.shared.ptcl.SuccessRsp

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js.Date
import scala.xml.Elem

/**
  * User: xgy
  * Date: 2020/2/5
  * Time: 1:06
  */

class WatchRecord_new(roomID: Long,videoName_old :String) extends Page{
  private val videoName = videoName_old + ".mp4"
  override val locationHashString: String = s"#/room/$roomID/$videoName"

  var roomList: List[RoomData] = Main.roomList

  private val roomIdData: Var[List[RoomInfoSection]] = Var(Main.roomIdData)
  private val liveRoomInfo = Var(roomList)
  private val CommentInfo: Var[Option[List[Comment]]] = Var(None)
  private val roomIdVar = Var(roomID)
  private val videoNameVar = Var(videoName)
  private val userNameVar = Var("")
  private val fileNameVar = Var(videoName)
  private val isinvitedVar = Var(false)

//  private def roomListRx(r: List[RoomInfoSection]) =
//    <div class="col-md-3 col-sm-12 col-xs-12" style="background-color: #F5F5F5; margin-left:-11%;margin-right:1%;margin-top:2px;height:416px;overflow-y: auto;">
//      <div class="x_title">
//        <h2>录像列表</h2>
//      </div>
//      <ul class="list-unstyled top_profiles scroll-view">
//        {
//        getRoomItem(r, videoName)
//        }
//      </ul>
//    </div>

  private def roomListRx2(roomList: List[RoomInfoSection], selected: String) =
    roomList.map {o=>
        if (true)
          <div class="orderPage table" style="margin-top:17px;margin-left:24px;cursor: pointer;" onclick={() => checkInvAndSkip(o.roomId,o.fileName)}>
            <div class="orderPage-nowrap-flex">
              <div class="orderPage table-first">
                <img class="orderPage table-first-img" src={Route.imgPath("videoCover/video"+o.fileName.takeRight(5).replaceAll("mp4","png"))}></img>
                <div class="orderPage table-first-txt">
                  <div style="margin-top:6px;text-align: left;font-size:large">{o.fileName}</div>
                  <div style="margin-top:6px;text-align: left;">{"发布者："+o.userName}</div>
                  <div style="margin-top:6px;text-align: left;">{"发布时间:"+o.time}</div>
                </div>
              </div>
            </div>
          </div>
        else <div></div>


    }

//  private def commentListRx(r: List[RoomInfoSection]) =
//    <div class="col-md-3 col-sm-12 col-xs-12" style="background-color: #F5F5F5; margin-left:1%;margin-top:2px;height:377px;overflow-y: auto;">
//      <div class="x_title">
//        <h2>评论列表</h2>
//      </div>
//      <div class="comment-Video">
//        <div class="commentTitle"></div>
//        <input class="commentInput" placehoder="发个友善的评论" id="commentInput">666</input>
//        {isinvitedVar.map{r=>
//        if(r) <div class="commentSubmit" onclick={() =>addComment(fileNameVar.toString().drop(4).dropRight(1));}>发表评论</div>
//        else <div class="commentNoSubmit">禁止评论</div>
//      }}
//      </div>
//      <ul class="list-unstyled top_profiles scroll-view">
//        {
//        getCommitItem(videoName)
//        }
//      </ul>
//    </div>

  private def videoTitle(l:RoomInfoSection) =
    <div>
          <div class="orderPage table" style="margin-top:17px;margin-left:24px">
            <div class="orderPage-nowrap-flex">
              <div class="orderPage">
                <div >
                  <div style="margin-top:6px;font-size:large;text-align: left;">{"视频标题："+l.fileName}</div>
                  <div style="margin-top:6px;text-align: left;">{"发布者："+l.userName}</div>
                  <div style="margin-top:6px;text-align: left;">{"发布时间："+l.time}</div>
                </div>
              </div>
            </div>
          </div>
      <div class="orderPage table" style="margin-top:17px;margin-left:24px">

          <div >
            <div  style="padding: 1rem 1rem 1rem 1rem;display:flex;">
              <div style="width:92%;">
                <input type="text" id="commentInput" placeholder="说点什么吧" style="width:100%;height:35px;" autofocus="true"></input>
              </div>
              <div>
                <button id="random" class="btn btn-info" style="margin: 0rem 1rem 0rem 1rem;height:35px;"
                        onclick={()=>addComment(fileNameVar.toString().drop(4).dropRight(1));}>评论</button>
              </div>
            </div>
          </div>


      </div>

</div>


  private def commentRx(videoName:String) =
    CommentInfo.map {o=>

      o.getOrElse(List.empty).filter(_.commentContent!="").sortBy(_.commentId).reverse.map{ comment =>
            if (true)
              <div class="orderPage table" style="margin-top:17px;margin-left:24px">
                <div style="display: flex;background:rgba(243,247,251,1);">
                  <div class="orderPage table-time">{"发布时间：暂无"}</div>
                  <div class="orderPage table-time">{comment.commentId}</div>
                  <div class="orderPage table-id">{"评论者："+comment.invitationName}</div>
                  {if(comment.userId == dom.window.localStorage.getItem("userId").toLong)
                  <div class="orderPage table-id" style="color:rgb(42,140,128);cursor:pointer" onclick={() => delComment(comment.commentId);getCommentList(roomID,videoName)}>删除</div>
                else <div></div>
                  }
                </div>
                <div class="orderPage-wrap-flex">
                  <div class="orderPage table-first">
                    <img class="orderPage table-first-img2" src={Route.hestiaPath(comment.invitationAvatar.getOrElse("be8feec67e052403e26ec05559607f10.jpg"))}></img>
                    <div class="orderPage table-first-txt">
                      <div style="margin-top: 6px;width: 600px;display: inline-block;text-align: left;">
                        <p style="color:grey;">{"评论："+ comment.commentContent}</p>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            else <div></div>
          }

    }

  //  private def commentRx =
  //    <div class="col-md-7" style="background-color: #F5F5F5; margin-left:129px;margin-top:2px;height:40px">
  //      <div class="comment-Video">
  //        <div class="commentTitle"></div>
  //        <input class="commentInput" placehoder="发个友善的评论" id="commentInput">666</input>
  //        {isinvitedVar.map{r=>
  //        if(r) <div class="commentSubmit" onclick={() =>addComment(fileNameVar.toString().drop(4).dropRight(1));}>发表评论</div>
  //        else <div class="commentNoSubmit">禁止评论</div>
  //      }}
  //      </div>
  //    </div>



  private def getRoomItem(roomList: List[RoomInfoSection], selected: String) = {
    roomList.map { room =>
      if (room.fileName == selected) {userNameVar:=room.userName;fileNameVar:=room.fileName;isinvitedVar:=room.isInvited}
      val isSelected = if (room.fileName == selected) "actived playerSelect" else ""
      <li class={"media event eyesight " + isSelected} style="text-align:left" onclick={() => checkInvAndSkip(room.roomId,room.fileName)}>
        <a class="pull-left border-aero profile_thumb">
          <img class="player" src={Route.imgPath("room.png")}></img>
        </a>
        <div class="media-body">
          <a class="title" href="javascript:void(0)">
            {"主讲人"+room.userName}({"会议号"+room.roomId})
          </a>
          <p style="font-size:5px">{"录像名："+room.fileName}</p>
          <p style="font-size:5px">{"保存录像时间："+room.time}</p>
          {
          if(room.isInvited) <p style="font-size:5px"></p>
          else <p style="font-size:5px;color:red" ></p>
          }
        </div>
      </li>
    }
  }

  private def getCommitItem(videoName:String) = {

    CommentInfo.map{comment1=>
      comment1.getOrElse(List.empty).filter(_.commentContent!="").map{ room=>
        val isSelected =  ""
        <li class={"media event eyesight " + isSelected} style="text-align:left">

          <div class="media-body">
            {
            if(userNameVar.toString().drop(4).dropRight(1)==dom.window.localStorage.getItem("username").toString)
              <div title="删除" onclick={()=>delComment(room.commentId);getCommentList(roomID,videoName)}><a class="title" href="javascript:void(0)">{room.userId}</a><p>{room.commentContent}</p></div>
            else <div><a class="title" href="javascript:void(0)">{room.invitationName}</a><p>{room.commentContent}</p></div>
            }
          </div>
        </li>
      }
    }}

  def checkInvAndSkip(roomId:Long,fileName:String): Unit ={
    val userId = dom.window.localStorage.getItem("userId").toLong
    Http.postJsonAndParse[SuccessRsp](Route.Invitation.checkInvitee, CheckInviteeReq(userId,fileName).asJson.noSpaces).map {
      rsp =>
        if(rsp.errCode == 0) {
          //          dom.document.location.href= s"#/room/"+roomId.toString + "/" + fileName
          dom.document.location.hash= s"#/room/"+roomId.toString + "/" + fileName.dropRight(4)
          println("ssssss")
        } else {
          JsFunc.alert("抱歉，你没有查看该录像的权限")
          println(rsp.msg)
        }
    }

  }

//  private def switchEyesight(roomId: Long,videoName: String): Unit ={
//    //    val userId= dom.window.localStorage.getItem("userId").toString
//    //    renderTest(dom.document.getElementById("my-video"),userId,videoNameVar.toString().drop(4).dropRight(1))
//    //    renderTest(dom.document.getElementById("my-video"),userId,videoName)
//    //    dom.window.setTimeout(()=>println("sssss"),8000)
//    dom.window.location.hash = s"#/room/$roomId/$videoName"
//  }

//  private def refresh: Unit ={
//    dom.window.location.hash = s"#/room/$roomID/$videoName"
//  }

  //  val container: Elem =
  //    VideoJSBuilderNew().buildElem("my-video")
  val container:Elem = {
    val userId = dom.window.localStorage.getItem("userId")
      <div style="border: 1px solid rgb(232, 232, 232);margin-top: 17px;margin-left: 24px;">
        <video controls="controls" width="800px" height="450px" preload="metadata">
          <source src={"http://10.1.29.247:42075/geek/room/getRecord/" + userId + "/" + videoName} type="video/webm"/>
        </video>
      </div>
  }


  val background: Elem =
    <div style="height:auto;">
      <div style="position: static;left: 0;right: 0;top: 50%;text-align: center;" >
        <div style="margin-left:3%;margin-right:3%;">
          <div class="x_content" >
            {
            roomIdData.map( l =>
              if (l.isEmpty) <div></div>
              else {
                //                dom.window.setTimeout(()=>renderLive(dom.document.getElementById("my-video"), l.filter(_.roomId == roomID).head.url), 1000)
                  <div style="background: green; display: flex;">
                    <div style="flex-basis: auto; white-space: nowrap; background: white; ">
                      {container}{videoTitle(l.filter(e=>e.fileName==videoName).head)}{commentRx(videoName)}
                    </div>
                    <div style="width: 100%; background: white;">
                      {roomListRx2(l,videoName)}
                    </div>
                  </div>
              }
            )
            }
          </div>
        </div>
      </div>

    </div>

  //  def getRoomList: Future[Unit] = {
  //    val url = Route.Room.getRoomList
  //    val data = GetRoomListReq().asJson.noSpaces
  //    Http.postJsonAndParse[GetRoomListRsp](url, data).map {
  //      rsp =>
  //        try {
  //          if (rsp.errCode == 0) {
  //            roomList = rsp.roomList
  //            liveRoomInfo := roomList
  //            println(s"got it : $rsp")
  //          }
  //          else {
  //            println("error======" + rsp.msg)
  //            JsFunc.alert(rsp.msg)
  //          }
  //        }
  //        catch {
  //          case e: Exception =>
  //            println(e)
  //        }
  //    }
  //  }

  def getCommentList(roomID:Long,videoName:String): Unit = {
    val url = Route.Room.getCommentList
    val data = GetCommentReq(roomID,videoName).asJson.noSpaces
    println(s"ssss start : ")
    Http.postJsonAndParse[GetCommentRsp](url, data).map {
      rsp =>
        try {
          if (rsp.errCode == 0) {
            CommentInfo :=rsp.roomId
            println(s"ssss got it : $rsp")
          }
          else {
            println("ssss error======" + rsp.msg)
          }
        }
        catch {
          case e: Exception =>
            println("ssss error======")
        }

    }

  }

  def addComment(fileName:String): Unit = {
    val url = Route.Room.addComment
    val userId=dom.window.localStorage.getItem("userId").toLong
    if(dom.document.getElementById("commentInput").asInstanceOf[Input].value == "")
      JsFunc.alert("不能发表空评论")
    else{
    val commentContent = dom.document.getElementById("commentInput").asInstanceOf[Input].value
    val data = addCommentReq(fileName,userId,commentContent).asJson.noSpaces
    Http.postJsonAndParse[SuccessRsp](url, data).map {
      rsp =>
        try {
          if (rsp.errCode == 0) {
            JsFunc.alert("评论成功")
            getCommentList(roomID,videoName)
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
    }}
  }
  def delComment(commentID:Long): Unit = {
    val url = Route.Room.delComment
    val data = delCommentReq(commentID).asJson.noSpaces
    Http.postJsonAndParse[SuccessRsp](url, data).map {
      rsp =>
        try {
          if (rsp.errCode == 0) {
            JsFunc.alert("删除评论成功")
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
    getCommentList(roomID,videoName)
    <div >
      {background}
    </div>

  }
}
