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


object Login_3 extends Page{
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
  Route.imgPath("login/page1_0.png")
  def init(): Unit ={
    dom.document.body.style = "background: url(/geek/static/img/login/b1.jpg);" +
      "background-repeat: no-repeat;" +
      "background-position: center;" +
      "background-size: cover;" +
      "-webkit-background-size: cover;" +
      "-moz-background-size: cover;" +
      "-o-background-size: cover;" +
      "box-sizing: border-box;" +
      "min-height: 100vh;"
  }



  override def render: Elem = {
    init()
    <div class="mid-class">
      <div class="art-right-w3ls">
      <h2>Sign In and Sign Up</h2>
      <form action="#" method="post">
        <div class="main">
          <div class="form-left-to-w3l">
            <input id="username" type="text" name="name" placeholder="Username"/>
            </div>
            <div class="form-left-to-w3l ">
              <input id="password"  type="password" name="password" placeholder="Password"/>
                <div class="clear"></div>
              </div>
            </div>
            <div class="left-side-forget">
              <input type="checkbox" class="checked"/>
                <span class="remenber-me">Remember me </span>
              </div>
              <div class="right-side-forget">
                <!--<a href="#" class="for">Forgot password...?</a> -->
              </div>
              <div class="clear"></div>
              <div class="btnn">
                <div id="logIn"  class="button-submit-login" onclick={() => signIn()}>Sign In</div>
                <div class="button-submit-login" data-toggle="modal" data-target={s"#signUp"}>Sign Up</div>
              </div>
            </form>
            <!-- popup-->
            <div id="content1" class="popup-effect">
              <div class="popup">
                <!--login form-->
                <div class="letter-w3ls">

                              <div class="clear"></div>
                            </div>
                            <!--//login form-->
                            <a class="close" href="#">{"&times;"}</a>
                          </div>
                        </div>
                        <!-- //popup -->
                      </div>
      <div class="art-left-w3ls">
        <h1 class="header-w3ls">{"GEEK sign up & login Page"}</h1>
    </div>
      {makeModal}
    </div>

    }

}
