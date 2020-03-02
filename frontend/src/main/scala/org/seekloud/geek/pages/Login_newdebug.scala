package org.seekloud.geek.pages

import org.scalajs.dom
import org.seekloud.geek.utils.Page
import org.seekloud.geek.common.Route
import mhtml.Var
import org.scalajs.dom
import org.scalajs.dom.html.{Button, Input}
import org.scalajs.dom.raw.KeyboardEvent
import org.seekloud.geek.utils.{Http, JsFunc, Modal, Page}
import io.circe.generic.auto._
import io.circe.syntax._
import org.seekloud.geek.common.Route
//import org.seekloud.geek.shared.ptcl.UserProtocol.{SignInReq, SignInRsp}
import org.seekloud.geek.shared.ptcl.CommonProtocol.{SignIn, SignInRsp}
import org.seekloud.geek.shared.ptcl.CommonProtocol.{SignUp, SignUpRsp}

//import io.circe.syntax._

import scala.concurrent.ExecutionContext.Implicits.global

import scala.xml.{Elem, Node}
//import scala.xml.Elem

object Login_newdebug extends Page {

  override val locationHashString: String = "#/login"

  private val windowHeight = dom.window.innerHeight

  private def signUp(): Unit = {
    val username = dom.document.getElementById("name").asInstanceOf[Input].value
    val password = dom.document.getElementById("pwd").asInstanceOf[Input].value
    if (username.nonEmpty && password.nonEmpty) {
      val data = SignUp(username, password,"").asJson.noSpaces
      Http.postJsonAndParse[SignUpRsp](Route.User.signUp, data).map {
        rsp =>
          rsp.errCode match {
            case 0 =>
              //              JsFunc.alert(s"注册成功！")
              dom.document.getElementById("username").asInstanceOf[Input].value = username
              dom.document.getElementById("password").asInstanceOf[Input].value = password
            case _ =>
              JsFunc.alert("注册失败，用户名冲突")
          }
      }
    } else {
      JsFunc.alert("输入不能为空!")
    }
  }


  private def signIn(): Unit = {
    val username = dom.document.getElementById("username").asInstanceOf[Input].value
    val password = dom.document.getElementById("password").asInstanceOf[Input].value
    if (username.nonEmpty && password.nonEmpty) {
      val data = SignIn(username, password).asJson.noSpaces
      Http.postJsonAndParse[SignInRsp](Route.User.signIn, data).map {
        rsp =>
          //          println("sss1")
          rsp.errCode match {
            case 0 =>
              //              JsFunc.alert(s"登陆成功！")
              val userId = rsp.userInfo.get.userId
              dom.window.localStorage.setItem("username", username)
              //              dom.window.localStorage.setItem("password", password)
              dom.window.localStorage.setItem("userId", userId.toString)
              dom.window.location.hash = s"#/home"
            //              println("sss2")
            case _ =>
              JsFunc.alert(rsp.msg)
              println("sss3")
          }
      }
    } else {
      JsFunc.alert("输入不能为空!")
    }
  }

  def logout(): Unit = {
    dom.window.localStorage.removeItem("userId")
    dom.window.localStorage.removeItem("username")
    dom.window.location.hash = "#/login"
  }

  def loginByEnter(event: KeyboardEvent): Unit = {
    if (event.keyCode == 13)
      dom.document.getElementById("logIn").asInstanceOf[Button].click()
  }




  def makeModal: Elem = {
    val title = <h4 class="modal-title" style="text-align: center;">注册</h4>
    val child =
      <div>
        <form style="border: 1px solid #dfdbdb;border-radius: 3px;padding:2rem 1rem 2rem 1rem;">
          <div class="row" style="padding: 1rem 1rem 1rem 1rem;">
            <label class="col-md-3" style="text-align:right">用户名</label>
            <div class="col-md-6">
              <input type="text" id="name" placeholder="用户名" class="form-control" autofocus="true"></input>
            </div>
          </div>
          <div class="row" style="padding: 1rem 1rem 1rem 1rem">
            <label class="col-md-3" style="text-align:right;">密码</label>
            <div class="col-md-6">
              <input type="password" id="pwd" placeholder="密码" class="form-control" onkeydown={e: KeyboardEvent => loginByEnter(e)}></input>
            </div>
          </div>
        </form>
      </div>
    new Modal(title, child, () => signUp(), s"signUp").render
  }

  def init(): Unit ={
    dom.document.body.style = "background-color:#191c2c;"
  }


  override def render: Elem = {
    init()
    <div>
      <div class="tyg-div">
        <ul style="    display: flex;flex-direction: column;">
          <li><div style="margin-left:20px;">创</div></li>
          <li><div style="margin-left:40px;">造</div></li>
          <li><div style="margin-left:60px;">美</div></li>
          <li><div style="margin-left:80px;">好</div></li>
          <li><div style="margin-left:100px;">生</div></li>
          <li><div style="margin-left:120px;">活</div></li>
        </ul>
      </div>
      <div id="contPar" class="contPar">
        <div id="page1"  style="z-index:1;">
          <div class="title0">GEEK</div>
          <div class="title1">多人视频会议系统</div>
          <div class="imgGroug">
            <ul>
              <img alt="1" class="img0 png" src={Route.imgPath("login/page1_0.png")}/>
              <img alt="2" class="img1 png" src={Route.imgPath("login/page1_1.png")}/>
              <img alt="3" class="img2 png" src={Route.imgPath("login/page1_2.png")}/>
            </ul>
          </div>
          <img alt="4" class="img3 png" src={Route.imgPath("login/page1_3.jpg")} />
        </div>
      </div>
      <div class="tyg-div-denglv">
        <div class="tyg-div-form">
          <form onsubmit = "return false">
            <p class="tyg-p">欢迎访问  GEEK</p>
            <div style="margin:5px 0px;">
              <input id="username" type="text" placeholder="请输入账号..."/>
            </div>
            <div style="margin:5px 0px;">
              <input type="password" id="password"  placeholder="请输入密码..."/>
            </div>
            <div style="display:flex;">
              <button id="logIn" onclick={() => signIn()}>登<span style="width:20px;"></span>录</button>
              <button  data-toggle="modal" data-target={s"#signUp"}>注<span style="width:20px;"></span>册</button>
            </div>

          </form>
        </div>
      </div>
      {makeModal}
    </div>
  }
}
