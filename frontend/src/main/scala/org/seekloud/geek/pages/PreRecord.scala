package org.seekloud.geek.pages

import org.scalajs.dom
import org.seekloud.geek.Main
import org.seekloud.geek.common.Route
import org.seekloud.geek.pages.Header.username
import org.seekloud.geek.pages.UserInfoPage.userDetail
import org.seekloud.geek.utils.Page

import scala.xml.Elem

object PreRecord extends Page{
    override def render:Elem={
      <div class="container" style="position: fixed;margin-left: 13%;">
        <div class="navbar-header">

          <div class="navbar-brand" >
            <a href="#/home">
              <h1 class="logo-alt" alt="logo" style="font-color:blue;">Geek</h1>
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
              Main.getRoomSecList()
              dom.window.setTimeout(() => Header.gotoLive(), 2000)
              ()
            }>Watch</a>
          </li>
          <li class="has-dropdown">
            <a href="#/userInfo">
              {userDetail.map{user=>
              <img style="width:25px;height:25px" src={Route.hestiaPath(user.avatar.getOrElse("be8feec67e052403e26ec05559607f10.jpg"))}></img>
            }}
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

    }
}
