package org.seekloud.geek.pages

import org.seekloud.geek.utils.{Component, JsFunc}
import org.scalajs.dom
import org.seekloud.geek.Main
import org.seekloud.geek.common.Route
import org.seekloud.geek.pages.HomePage.gotoPage
import org.seekloud.geek.pages.UserInfoPage.userDetail
import mhtml._
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
  val Show = Var("")
  def gotoLive(): Unit = {
//    if (Main.roomList.nonEmpty) gotoPage(s"room/${Main.roomList.head.roomId}")
//    else JsFunc.alert("当前没有录像！")
    if (Main.roomIdData.nonEmpty) gotoPage(s"room/${Main.roomIdData.head.roomId}/${Main.roomIdData.head.fileName}")
    else JsFunc.alert("当前没有录像！")
  }

  def Show(flag: String): Unit = {
    Show := flag
  }

  override def render: Elem =
    <nav id="nav" class="navbar nav-transparent">
      <div style="height:104px;">
        <div class =  "header-container">
          <div class = "top-header" id="head" >
            <img   class ="header-logo" onclick={()=> dom.window.location.href = "/"}/>
            <ul style="margin-left:50px;display:flex;">
              <li class="top-text top-text-opacity">
                <a href="#/home">
                  <img style="height:50px" src={Route.imgPath("logo3.png")}/>
                </a>
              </li>
              </ul>
            <ul class = "ul-list" style="display:flex;">
              <li class="top-text top-text-opacity" style="width:30px;" onmouseover={() => Show("house")} onmouseleave={() => Show(" ")}>
                <a href="#/home">
                  <img style="width:25px;height:25px" src={Route.imgPath("house.png")}/>
                  <span class="header-word-tip" style={Show.map { show =>  if (show=="house") "display:block;" else "display:none;" }}>我的主页</span>
                </a>
              </li>
              <li class="top-text top-text-opacity" style="width:30px;" onmouseover={() => Show("invite")} onmouseleave={() => Show(" ")}>
                <a  href="#/inviterManage">
                  <img style="width:25px;height:25px" src={Route.imgPath("invite.png")}/>
                  <span class="header-word-tip" style={Show.map { show =>  if (show=="invite") "display:block;" else "display:none;" }}>邀请详情</span>
                </a>
              </li>
              <li class="top-text top-text-opacity" style="width:30px;" onmouseover={() => Show("video")} onmouseleave={() => Show(" ")}>
                <a href="javascript:void(0)" onclick={() =>Main.getRoomSecList();dom.window.setTimeout(() => Header.gotoLive(), 2000);()}>
                  <img style="width:25px;height:25px" src={Route.imgPath("video.png")}/>
                  <span class="header-word-tip" style={Show.map { show =>  if (show=="video") "display:block;" else "display:none;" }}>查看录像</span>
                </a>
              </li>
              <li class="top-text top-text-opacity" style="width:30px;" onmouseover={() => Show("logout")} onmouseleave={() => Show(" ")}>
                <a href="javascript:void(0)" onclick={() =>()}>
                  <img style="width:25px;height:25px" src={Route.imgPath("logout.png")}/>
                  <span class="header-word-tip" style={Show.map { show =>  if (show=="logout") "display:block;" else "display:none;" }}>退出登录</span>
                </a>
              </li>
              <li class="top-text top-text-opacity" style="width:30px;" onmouseover={() => Show("mycard")} onmouseleave={() => Show(" ")}>
                <a href="#/userInfo">
                  {userDetail.map{user=>
                  <img style="width:25px;height:25px" src={Route.hestiaPath(user.avatar.getOrElse("be8feec67e052403e26ec05559607f10.jpg"))}></img>
                }}
                  <span class="header-word-tip" style={Show.map { show =>  if (show=="mycard") "display:block;" else "display:none;" }}>我的信息</span>
                </a>
              </li>






            </ul>
          </div>
        </div>

      </div>



    </nav>


}

