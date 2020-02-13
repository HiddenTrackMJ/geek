package org.seekloud.geek.pages
import mhtml.{Rx, Var, emptyHTML}
import org.scalajs.dom
import org.scalajs.dom.html.{Div, Input, Select}
import org.seekloud.geek.common.Route
import org.seekloud.geek.utils.{Http, JsFunc, Modal, Page}
import org.seekloud.geek.shared.ptcl.RoomProtocol._
import io.circe.generic.auto._
import io.circe.syntax._
import org.scalajs.dom.KeyboardEvent
import org.seekloud.geek.Main
import org.seekloud.geek.shared.ptcl.CommonProtocol.{InvitationReq, InvitationRsp, Inviter, InviterAndInviteeReq}
import org.seekloud.geek.shared.ptcl.{ComRsp, SuccessRsp}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.xml.Elem
object InviterManagement extends Page{
  private val roomWrapper = Var(emptyHTML)
  private val inviterData: Var[Option[List[Inviter]]] = Var(None)
  private val inviteeData: Var[Option[List[Inviter]]] = Var(None)
  private val roomIdData: Var[List[RoomId]] = Var(List())

  def chooseVisitor(): Unit ={

  }

  def makeModal: Elem = {
    val title = <h4 class="modal-title" style="text-align: center;">邀请用户</h4>
    val child =
      <div>
        <form style="border: 1px solid #dfdbdb;border-radius: 3px;padding:2rem 1rem 2rem 1rem;">
          <div class="row" style="padding: 1rem 1rem 1rem 1rem;">
            <label class="col-md-3" style="text-align:right">用户名</label>
            <div class="col-md-6">
              <input type="text" id="name" placeholder="请输入被邀请用户名" class="form-control" autofocus="true"></input>
            </div>
          </div>
        </form>
      </div>
    new Modal(title, child, () => chooseVisitor(), s"chooseVisitor").render
  }

  private val primaryInfo =
  <div class="primaryInfo">
    <div class="row info">
      <div class="col-md-8">
        <img class="headImg" src={Route.imgPath("cat.jpg")}></img>
        <h2 class="username">
          {dom.window.localStorage.getItem("username")}
        </h2>
      </div>
    </div>
  </div>

  val inviterDetail : Rx[Elem] = inviterData.map{
    case None =>  <div class="row"><div class="save">暂无邀请 </div></div>
    case Some(info) => <div class="row">
     {
      val username= dom.window.localStorage.getItem("username")
      if(info.length<=1) <div class="save">暂无邀请 </div>
      else <div>{info.filter(_.inviterName != username).map{inviter =>
        <a href="#/inviterManage" title={"用户id:"+inviter.inviterId} class="save">{inviter.inviterName} </a>
      }}</div>

      }
      </div>
  }

  val inviteeDetail : Rx[Elem] = inviteeData.map{
    case None =>  <div class="row"><div class="save">暂无邀请 </div></div>
    case Some(info) => <div class="row">
      {
      val username= dom.window.localStorage.getItem("username")
      if(info.length<=1) <div class="save">暂无邀请 </div>
      else <div>{info.filter(_.inviterName != username).map{invitee =>
        <div  title="点击删除邀请" class="save" onclick={()=>delInvitee(invitee.inviterId);getInviteeInfo()}>{invitee.inviterName} </div>
      }}</div>
      }
    </div>
  }

  val roomIdDetail : Rx[Elem] = roomIdData.map{
    case Nil => <select id="modifyPeople" class="modify-people">暂无数据</select>
    case info => <select id="modifyPeople" class="modify-people">
      {
      info.map(r=>
      <option >{r.roomId}</option>
      )
      }
      </select>
  }


//    <div class="row">
//      <p class="label">示例：</p>
//    </div>
//    <div class="row">
//      <a href="#/home" title="点击无效" class="save">邱林辉 </a>
//    </div>



  val inviterInfo: Elem =
    <div class="roomInfo">
        <div class="row" style="margin-top:12px">
          <p class="label">邀请我的：</p>
        </div>
        {inviterDetail}
        <div class="row" style="margin-top:12px">
          <p class="label">我邀请的：</p>
        </div>
        {inviteeDetail}
        <div class="row" style="margin-top:12px">
          <p class="label">我的会议：</p>
        </div>
        <div class="row">
          {roomIdDetail}
          <button id="random" class="btn btn-info" style="margin: 0rem 1rem 0rem 1rem;"
                  data-toggle="modal" data-target={s"#chooseVisitor"}>选择</button>
          {makeModal}
        </div>
      </div>



  val bottom = <div style="height:50px"></div>

  private val infoArea =
    <div>
      {primaryInfo}{inviterInfo}{bottom}
    </div>

  def getInviterInfo(): Unit ={
    val userName = dom.window.localStorage.getItem("username")
    val userId = dom.window.localStorage.getItem("userId").toLong
    println(userName+"sas"+userId)
    Http.postJsonAndParse[InvitationRsp](Route.Invitation.getInviterList, InvitationReq(userId).asJson.noSpaces).map {
      rsp =>
        if (rsp.errCode == 0) {
          if(rsp.list.nonEmpty)
          inviterData := Some(rsp.list.get)
          else inviterData := None
        } else {
//          JsFunc.alert(rsp.msg)
          println(rsp.msg)
        }
    }
  }

  def getInviteeInfo(): Unit ={
    val userId = dom.window.localStorage.getItem("userId").toLong
    Http.postJsonAndParse[InvitationRsp](Route.Invitation.getInviteeList, InvitationReq(userId).asJson.noSpaces).map {
      rsp =>
        if (rsp.errCode == 0) {
          if(rsp.list.nonEmpty)
            inviteeData := Some(rsp.list.get)
          else inviteeData := None
        } else {
//          JsFunc.alert(rsp.msg)
          println(rsp.msg)
        }
    }
  }

  def delInvitee(invitee: Long): Unit ={
    val userId = dom.window.localStorage.getItem("userId").toLong
    Http.postJsonAndParse[SuccessRsp](Route.Invitation.delInvitee, InviterAndInviteeReq(userId,invitee).asJson.noSpaces).map {
      rsp =>
        if(rsp.errCode == 0) {
          JsFunc.alert("删除成功！")
        } else {
          println(rsp.msg)
        }
    }
  }

  def addInvitee(invitee: Long): Unit ={
    val userId = dom.window.localStorage.getItem("userId").toLong
    Http.postJsonAndParse[SuccessRsp](Route.Invitation.getInviteeList, InviterAndInviteeReq(userId,invitee).asJson.noSpaces).map {
      rsp =>
        if(rsp.errCode == 0) {
          JsFunc.alert("添加成功！")
        } else {
          JsFunc.alert("添加失败！")
          println(rsp.msg)
        }
    }
  }

  def getRoomSecList(): Unit ={
    val userId = dom.window.localStorage.getItem("userId").toLong
    Http.postJsonAndParse[GetRoomIdListRsp](Route.Room.getRoomIdList, GetRoomSectionListReq(userId).asJson.noSpaces).map {
      rsp =>
        if(rsp.errCode == 0) {
          roomIdData := rsp.roomList
        } else {
          println(rsp.msg)
        }
    }
  }






  def generate(): Unit = {//获取邀请人被邀请人，以及邀请人的录像地址
    roomWrapper := infoArea
  }

  def init() = {
    dom.document.body.style = "background-image: url('/geek/static/img/background1.jpg');" +
      "background-attachment: fixed;" +
      "background-size: cover;" +
      "background-position: center;" +
      "background-repeat: no-repeat;"
    getInviterInfo()
    getInviteeInfo()
    getRoomSecList()
  }

  override def render: Elem = {
    HomePage.init()
    init()
    generate()
    <div>
      {roomWrapper}
    </div>
  }

}
