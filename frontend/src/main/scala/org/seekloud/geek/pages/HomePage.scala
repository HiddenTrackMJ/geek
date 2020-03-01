package org.seekloud.geek.pages

import mhtml._
import org.scalajs.dom
import org.seekloud.geek.Main
import org.seekloud.geek.common.Route
import org.seekloud.geek.pages.UserInfoPage.userDetail
import org.seekloud.geek.shared.ptcl.CommonProtocol.{GetUserReq, GetUserRsp}
import org.seekloud.geek.utils.{Http, Page}
import io.circe.generic.auto._
import io.circe.syntax._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.xml.Elem

object HomePage extends Page {
  def getUserInfo: Unit ={
    println("start getuserinfo")
    val userId = dom.window.localStorage.getItem("userId").toLong
    Http.postJsonAndParse[GetUserRsp](Route.User.getUserDetail, GetUserReq(userId).asJson.noSpaces).map {
      rsp =>
        println(rsp)
        if (rsp.errCode == 0) {
          userDetail :=rsp.userInfo.get
          println("sss"+rsp)
        } else {
          //          JsFunc.alert(rsp.msg)
          println(rsp.msg)
        }
        println("end getuserinfo")
    }
  }
  override val locationHashString: String = "#/home"
  val modalDiv = Var(emptyHTML)

  val homeWrapper: Elem =
    <header id="home">

      <div class="home-wrapper">
        <div class="container">
          <div class="row" style="margin:0 auto">

            <div class="col-md-10 col-md-offset-1">
              <div class="home-content">
                <h1 class="white-text">多人视频会议系统</h1>
                <button class="white-btn" style="width:145px" onclick={() => gotoPage("inviterManage")}>Invite</button>
                <button class="main-btn" style="width:145px" onclick={() =>
                  dom.document.location.href="#/preRecord/-"
                  ()
                }>Watch</button>
              </div>
            </div>

          </div>
        </div>
      </div>

    </header>



  def gotoPage(path: String): Unit = {
    dom.window.location.hash = s"#/$path"
  }

  def init(): Unit = {
    val userId = dom.window.localStorage.getItem("userId")
    val userName = dom.window.localStorage.getItem("username")
    if (userId != null && userName != null) {
      Header.userId := userId
      Header.username := userName
    } else {
      gotoPage("login")
    }
    getUserInfo
    dom.document.body.style = "background-image: url('/geek/static/img/bg1.png');" +
      "background-attachment: fixed;" +
      "background-size: cover;" +
      "background-position: center;" +
      "background-repeat: no-repeat;"
  }

  override def render: Elem = {
    println("Render")
    init()
    //    dom.document.documentElement.appendChild({renderWebm()})

    <div style="margin:0 0;">
      {homeWrapper}
    </div>

  }

}
