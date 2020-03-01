package org.seekloud.geek.pages
import mhtml.{Rx, Var, emptyHTML}
import org.scalajs.dom
import org.scalajs.dom.html.{Div, Input, Select}
import org.seekloud.geek.common.Route
import org.seekloud.geek.utils.{Http, JsFunc, Modal, Page}
import org.seekloud.geek.shared.ptcl.RoomProtocol._
import io.circe.generic.auto._
import io.circe.syntax._
import org.scalajs.dom.raw._
import org.scalajs.dom.KeyboardEvent
import org.seekloud.geek.Main
import org.seekloud.geek.pages.UserInfoPage.userDetail
import org.seekloud.geek.shared.ptcl.CommonProtocol.{InvitationReq, InvitationRsp, Inviter, InviterAndInviteeAndRoomReq, InviterAndInviteeDetail, InviterAndInviteeDetailRsp, InviterAndInviteeReq, addInviteeReq}
import org.seekloud.geek.shared.ptcl.{ComRsp, SuccessRsp}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.xml.Elem

object InviterManagement extends Page{
  private val roomWrapper = Var(emptyHTML)
  private val inviterData: Var[Option[List[Inviter]]] = Var(None)
  private val inviteeData: Var[Option[List[Inviter]]] = Var(None)
  private val inviterDetailData: Var[List[InviterAndInviteeDetail]] = Var(List.empty)
  private val inviteeDetailData: Var[List[InviterAndInviteeDetail]] = Var(List.empty)
  private val roomIdData: Var[List[RoomId]] = Var(List())
  private val isMeetingChoose = Var(false)
  private val inviterVar = Var("")
  private val inviteeVar = Var("")




  def meetingModal: Elem = {
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
    new Modal(title, child, () => addInvitee(), s"chooseVisitor").render
  }
  def inviterModal: Elem = {
    val title = <h4 class="modal-title" style="text-align: center;">邀请人详情</h4>
    val child =
      <div>
        <form style="border: 1px solid #dfdbdb;border-radius: 3px;padding:2rem 1rem 2rem 1rem;">
          <div class="row" style="padding: 1rem 1rem 1rem 1rem;display:flex;flex-direction:column;text-align:center;">
            <label  style="margin:10px;">我被邀请的房间号</label>
            {
            inviterDetailData.map(va=>va.sortBy(_.roomId).map{inviter=>
              <div >
                <label title="点击删除邀请" style="cursor:pointer;" onclick ={()=>chooseToDeleteInviter(inviter.roomId)}>{inviter.roomId}</label>
              </div>
            }
            )
            }
          </div>
        </form>
      </div>
    new Modal(title, child, () => addInvitee(), s"inviterModal").render
  }

  def inviteeModal: Elem = {
    val title = <h4 class="modal-title" style="text-align: center;">被邀请人详情</h4>
    val child =
      <div>
        <form style="border: 1px solid #dfdbdb;border-radius: 3px;padding:2rem 1rem 2rem 1rem;">
          <div class="row" style="padding: 1rem 1rem 1rem 1rem;display:flex;flex-direction:column;text-align:center;">
            <label  style="margin:10px;">用户被邀请的房间号</label>
            {
            inviteeDetailData.map(va=>va.sortBy(_.roomId).map{invitee=>
              <div >
                <label title="点击删除邀请" style="cursor:pointer;" onclick ={()=>chooseToDeleteInvitee(invitee.roomId)}>{invitee.roomId}</label>
              </div>
            }
            )
            }
          </div>
        </form>
      </div>
    new Modal(title, child, () => addInvitee(), s"inviteeModal").render
  }

  private val primaryInfo =
  <div class="primaryInfo">
    <div class="row info">
      <div>
        {userDetail.map{user=>
        <img style="width:100px;height:100px" src={Route.hestiaPath(user.avatar.getOrElse("be8feec67e052403e26ec05559607f10.jpg"))}></img>
      }}
        <h2 class="username">
          <div>{"用户名："+dom.window.localStorage.getItem("username")}</div>
          <div>{"用户Id："+dom.window.localStorage.getItem("userId")}</div>
        </h2>
      </div>
    </div>
  </div>

  val inviterDetail : Rx[Elem] = inviterData.map{
    case None =>  <div class="row"><div class="save">暂无邀请 </div></div>
    case Some(info) => <div class="row">
     {
      val username= dom.window.localStorage.getItem("username")
      if(info.length==1 && info.head.inviterName == username) <div class="save">暂无邀请 </div>
      else <div>{info.filter(_.inviterName != username).map{inviter =>
        <a href="#/inviterManage" title={"用户id:"+inviter.inviterId} class="save" data-toggle="modal" data-target={s"#inviterModal"} onclick={()=>getInviterDetail(inviter.inviterId);()}>{inviter.inviterName} </a>
      }}</div>

      }
      </div>
  }

  val inviteeDetail : Rx[Elem] = inviteeData.map{
    case None =>  <div class="row"><div class="save">暂无邀请 </div></div>
    case Some(info) => <div class="row">
      {
      val username= dom.window.localStorage.getItem("username")
      if(info.length==1 && info.head.inviterName == username) <div class="save">暂无邀请 </div>
      else <div>{info.filter(_.inviterName != username).map{invitee =>
        <div  title="点击查看邀请人详情" class="save" data-toggle="modal" data-target={s"#inviteeModal"} onclick={()=>getInviteeDetail(invitee.inviterId);()}>{invitee.inviterName} </div>
      }}</div>
      }
    </div>
  }

  val roomIdDetail : Rx[Elem] = roomIdData.map{
    case Nil =>
      <div class="row">
        <select id="modifyPeople" class="modify-people"><option >暂无数据</option></select>
      </div>
    case info => <div class="row"><select id="modifyPeople" class="modify-people">
      {
      info.sortBy(_.roomId).map(r=>
      <option>{r.roomId}</option>
      )
      }
      </select>
      <button id="random" class="btn btn-info " style="margin: 0rem 1rem 0rem 1rem"
              data-toggle="modal" data-target={s"#chooseVisitor"}>选择</button>
    </div>
  }

  def chooseMeeting(e:Select): Unit ={
    println(isMeetingChoose)
    if(e.value=="-") isMeetingChoose:=false
    else isMeetingChoose:=true

  }



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
        <div >
          {roomIdDetail}

          {meetingModal}
          {inviterModal}
          {inviteeModal}
        </div>
      </div>



  val bottom = <div style="height:50px"></div>

  private val infoArea =
    <div>
      {primaryInfo}{inviterInfo}{bottom}
    </div>




  def chooseToDeleteInvitee(roomId:Long): Unit ={
    if(dom.window.confirm("确定要删除邀请吗???")){
      println("666")
      val userId = dom.window.localStorage.getItem("userId").toLong
      Http.postJsonAndParse[SuccessRsp](Route.Invitation.delInvitee, InviterAndInviteeAndRoomReq(userId,inviteeVar.toString.drop(4).dropRight(1).toLong,roomId).asJson.noSpaces).map {
        rsp =>
          if(rsp.errCode == 0) {
            JsFunc.alert("删除成功！")
            getInviteeDetail(inviteeVar.toString.drop(4).dropRight(1).toLong)
          } else {
            println(rsp.msg)
          }
      }
    }

    else println("555")
  }

  def chooseToDeleteInviter(roomId:Long): Unit ={
    if(dom.window.confirm("确定要删除邀请吗???")){
      println("666")
      val userId = dom.window.localStorage.getItem("userId").toLong
      Http.postJsonAndParse[SuccessRsp](Route.Invitation.delInvitee, InviterAndInviteeAndRoomReq(inviterVar.toString.drop(4).dropRight(1).toLong,userId,roomId).asJson.noSpaces).map {
        rsp =>
          if(rsp.errCode == 0) {
            JsFunc.alert("删除成功！")
            getInviterDetail(inviterVar.toString.drop(4).dropRight(1).toLong)
          } else {
            println(rsp.msg)
          }
      }
    }

    else println("555")
  }


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

  def getInviteeDetail(invitee: Long): Unit ={
    val userId = dom.window.localStorage.getItem("userId").toLong
    Http.postJsonAndParse[InviterAndInviteeDetailRsp](Route.Invitation.getInviteDetail, InviterAndInviteeReq(userId,invitee).asJson.noSpaces).map {
      rsp =>
        if(rsp.errCode == 0) {
          inviteeDetailData:=rsp.list
          inviteeVar:=invitee.toString
          println(rsp.list)
        } else {
          println(rsp.msg)
        }
    }
  }
  def getInviterDetail(inviter: Long): Unit ={
    val userId = dom.window.localStorage.getItem("userId").toLong
    Http.postJsonAndParse[InviterAndInviteeDetailRsp](Route.Invitation.getInviteDetail, InviterAndInviteeReq(inviter,userId).asJson.noSpaces).map {
      rsp =>
        if(rsp.errCode == 0) {
          inviterDetailData:=rsp.list
          inviterVar:=inviter.toString
          println(rsp.list)
        } else {
          println(rsp.msg)
        }
    }
  }

  def addInvitee(): Unit ={
    //先判断是否有该用户，再判断是否已经被邀请进房间，再邀请
    val inviteeName = dom.document.getElementById("name").asInstanceOf[Input].value
    val roomId=dom.document.getElementById("modifyPeople").asInstanceOf[Select].value.toLong
    val userId = dom.window.localStorage.getItem("userId").toLong
    Http.postJsonAndParse[SuccessRsp](Route.Invitation.addInvitee, addInviteeReq(userId,roomId,inviteeName).asJson.noSpaces).map {
      rsp =>
        if (rsp.errCode == 0) {
          JsFunc.alert("添加成功")
        } else {
          JsFunc.alert(rsp.msg)
//          println("ss"+rsp.msg)
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
