package org.seekloud.geek.pages
import mhtml.{Rx, Var}
import org.scalajs.dom
import org.scalajs.dom.html.{Button, Input}
import org.scalajs.dom.raw.KeyboardEvent
import org.seekloud.geek.utils.{Http, JsFunc, Modal, Page, TimeTool}
import io.circe.generic.auto._
import io.circe.syntax._
import org.seekloud.geek.common.Route
//import org.seekloud.geek.shared.ptcl.UserProtocol.{SignInReq, SignInRsp}
import org.seekloud.geek.shared.ptcl.CommonProtocol.{SignIn, SignInRsp}
import org.seekloud.geek.shared.ptcl.CommonProtocol.{SignUp, SignUpRsp}

//import io.circe.syntax._

import scala.concurrent.ExecutionContext.Implicits.global

import scala.xml.{Elem, Node}

/**
  * User: xgy
  * Date: 2020/2/4
  * Time: 20:58
  */
object UserInfoPage extends Page{
  private val currentPage = Var(1)

  val types = Var(1) //2用户详情 1更新用户信息


  case class UserInfo(
                       id: Long,
                       courseTypeId: String,
                       genderLimit: Int,
                       courseName: String,
                       classRoom: String,
                       provider: String,
                       teacherId: Long,
                       grade: String,
                       address: String,
                       enrolmentNum: Int,
                       openTime: Long,
                       price: Double,
                       deadline: Option[Long],
                       totalHour: Int,
                       des: Option[String],
                       cover: Option[String]
                     )

  val userInfoDetail = Var(Option.empty[UserInfo])
  userInfoDetail.update(_=>Some(UserInfo(1: Long,
    "ss": String,
    1: Int,
    "ss": String,
    "ss": String,
    "ss": String,
    1: Long,
    "ss": String,
    "ss": String,
    1: Int,
    1: Long,
    1: Double,
    None,
    1: Int,
    None,
    None))
  )

  def init(): Unit = {
    val userId = dom.window.localStorage.getItem("userId")
    val userName = dom.window.localStorage.getItem("username")
    if (userId != null && userName != null) {
      Header.userId := userId
      Header.username := userName
    } else {
            gotoPage("login")
    }

    dom.document.body.style = "background-image: url('/geek/static/img/bg1.png');" +
      "background-attachment: fixed;" +
      "background-size: cover;" +
      "background-position: center;" +
      "background-repeat: no-repeat;"
  }

  val userInfoContent: Elem =
    <header id="home">

      <div class="home-wrapper">
        <div class="container">
          <div class="row" style="margin:0 auto">

            <div class="col-md-10 col-md-offset-1">
              <div class="home-content">
                <h1 class="white-text">We Create Miracles</h1>
                <p class="white-text">La Vita Nuova.
                </p>
                <button class="white-btn" style="width:145px" >Live</button>
                <button class="main-btn" style="width:145px" >Watch</button>
              </div>
            </div>

          </div>
        </div>
      </div>
    </header>

  def updateUserInfo: Unit ={

  }


  def closeCreateCourse(): Unit = {
    updateUserInfo//
    dom.document.location.href="#/home"
  }


  val courseLists: Elem =
      <div class="creatCourse">
        <div class="creatCourse-titleA">
          <div class="createCourse-title">个人详情</div>
          <img src="/geek/static/img/关闭.svg"  onclick={()=>closeCreateCourse()}></img>
        </div>
        <div class="createCouse-content">
          <div class="createCourse-list-1" style="width:40%;margin-left:10%">
            <div class="createCourse-item">
              <div class="test">头像选择：</div>
              <img style="cursor:pointer;height:30px;width:30px;"  src={Route.imgPath("cat.jpg")} ></img>
            </div>
            <div class="createCourse-item">
              <div class="test">昵称：</div>
              <input placeholder="不限" disabled="disabled" style="background: #f0f0f0;" class="test-input"></input>
            </div>
            <div class="createCourse-item">
              <div class="test">性别：</div>
              <select id="mType" class="arrow-img">
                <option value="-" disabled="true" selected="true" hidden="true">请选择</option>
                <option value="兴趣班">男</option>
                <option value="学课班">女</option>
                <option value="特长班">无</option>
              </select>
            </div>

            {""}
            <div class="createCourse-item">
              <div class="test">年龄：</div>
              <input placeholder="请输入年龄"   class="test-input"></input>
            </div>
            <div class="createCourse-item" style="align-items: flex-start;">
              <div class="test" style="margin-top: 4px">居住地址：</div>
              <div>
                {""}
                <input placeholder="请输入详细地址" id="classRoom"  class="test-input"></input>
              </div>
            </div>
            <div class="createCourse-item">
              <div class="test">服务提供方：</div>
              <input placeholder="geek" disabled="disabled" style="background: #f0f0f0;"  class="test-input"></input>
            </div>
          </div>

          <div class="createCourse-list-2" style="width:40%">
            <div class="createCourse-item">
              <div>房间号：</div>
              <input placeholder="201556541" disabled="disabled" style="background: #f0f0f0;"></input>
            </div>
            <div class="createCourse-item">
              <div>开房时间：</div>
              <div>
                <input placeholder="2020/2/5" disabled="disabled" style="background: #f0f0f0;"></input>
                </div>
            </div>
            <div class="createCourse-item">
              <div>房间截止时间：</div>
              <input placeholder="2020/2/6" disabled="disabled" style="background: #f0f0f0;"></input>
            </div>
            <div class="createCourse-item createCourse-textarea">
              <div style="margin-top: 4px;">房间描述：</div>
              <textarea placeholder="请描述下房间" ></textarea>
            </div>


          </div>
        </div>
        <div class="submit" onclick={()=>updateUserInfo}>更新信息</div>
      </div>


  private val pageTitle =
    currentPage.map {
      case 0 =>
        <div class="courseM-header">课程管理</div>

      case t if t == 1 || t == 2 =>
        <div class="orderPage-Title" style="display:flex">
          <a onclick={() => closeCreateCourse()} style="color:rgba(0,0,0,0.45);cursor:pointer">
            {"课程管理 "}
          </a>
          <div style="margin-left:3px">{if( t==1 ) " / 新建" else " / 详情"}</div>
        </div>

      case _ => <div></div>
    }


  def gotoPage(path: String): Unit = {
    dom.window.location.hash = s"#/$path"
  }

  override def render: Elem = {
    println("Render")
    init()
    //    dom.document.documentElement.appendChild({renderWebm()})

    <div style="margin:0 0;">

      {courseLists}
    </div>

  }
}
