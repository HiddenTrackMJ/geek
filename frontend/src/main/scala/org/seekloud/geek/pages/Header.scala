package org.seekloud.geek.pages

import org.seekloud.geek.utils.{Component, JsFunc}
import mhtml._
import org.scalajs.dom
import org.seekloud.geek.Main
import org.seekloud.geek.common.Route
import org.seekloud.geek.pages.HomePage.gotoPage

import scala.xml.Elem

/**
  * User: xgy
  * Date: 2020/2/4
  * Time: 18:17
  */
object Header extends Component {

  /*userInfo*/
  val userId: Var[String] = Var("")
  val username: Var[String] = Var("")

  def gotoLive(): Unit = {
    if (Main.roomList.nonEmpty) gotoPage(s"room/${Main.roomList.head.roomId}")
    else JsFunc.alert("当前没有录像！")
  }

  override def render: Elem =
    <nav id="nav" class="navbar nav-transparent">
      <div class="container" style="position: fixed;margin-left: 13%;">
        <div class="navbar-header">

          <div class="navbar-brand" >
            <a href="#/home">
              <img class="logo-alt" src={Route.imgPath("logo2.png")} alt="logo"></img>
            </a>
          </div>


          <div class="nav-collapse">
            <span></span>
          </div>

        </div>

        <ul class="main-nav nav navbar-nav navbar-right" >
          <li class="active">
            <a href="#/home">Home</a>
          </li>
          <li>
            <a href="#/inviterManage">Invite</a>
          </li>
          <li>
            <a href="javascript:void(0)" onclick={() =>
              Main.getRoomList()
              dom.window.setTimeout(() => Header.gotoLive(), 500)
              ()
            }>Watch</a>
          </li>
          <li class="has-dropdown">
            <a href="#/userInfo">
              <img style="width:25px;height:25px" src={Route.imgPath("cat.jpg")}></img>
            </a>
            <ul class="dropdown">
              <li>
                <p style="color:white">Signed in as</p>
                <strong style="color:white">
                  <p>{username}</p>
                </strong>
              </li>
              <li role="separator" class="divider"></li>
              <li>
                <a href="javascript:void(0)" onclick={() => Login.logout()}>log out</a>
              </li>
            </ul>
          </li>
        </ul>
      </div>
    </nav>


}

