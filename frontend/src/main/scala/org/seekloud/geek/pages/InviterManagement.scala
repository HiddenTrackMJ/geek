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
import org.seekloud.geek.shared.ptcl.{ComRsp, SuccessRsp}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.xml.Elem
object InviterManagement extends Page{
  private val roomWrapper = Var(emptyHTML)

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

  val inviterInfo: Elem =
      <div class="roomInfo">
        <div class="row">
          <p class="label">我邀请的：</p>
        </div>
        <div class="row">
          <a href="#/home" title="点击删除邀请" class="save">邱林辉 </a>
        </div>
        <div class="row" style="margin-top:12px">
          <p class="label">邀请我的：</p>
        </div>
        <div class="row">
          <a href="#/home" title="点击查看录像" class="save">何炜 </a>
        </div>
        <div class="row" style="margin-top:12px">
          <p class="label">我的会议：</p>
        </div>
        <div class="row">
          <select id="modifyPeople" class="modify-people">
          </select>
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



  def getInviterInfo(): Unit = {//获取邀请人被邀请人，以及邀请人的录像地址
    roomWrapper := infoArea
  }

  def init() = {
    dom.document.body.style = "background-image: url('/geek/static/img/background1.jpg');" +
      "background-attachment: fixed;" +
      "background-size: cover;" +
      "background-position: center;" +
      "background-repeat: no-repeat;"
  }

  override def render: Elem = {
    HomePage.init()
    init()
    getInviterInfo()
    <div>
      {roomWrapper}
    </div>
  }

}
