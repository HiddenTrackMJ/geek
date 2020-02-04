package org.seekloud.geek.pages

import mhtml.Var
import org.scalajs.dom
import org.scalajs.dom.html.{Button, Input}
import org.scalajs.dom.raw.KeyboardEvent
import org.seekloud.geek.utils.{Http, JsFunc, Modal, Page}
import io.circe.generic.auto._
import io.circe.syntax._
import org.seekloud.geek.common.Route
import org.seekloud.geek.shared.ptcl.UserProtocol.{SignInReq, SignInRsp}
//import io.circe.syntax._

import scala.concurrent.ExecutionContext.Implicits.global

import scala.xml.{Elem, Node}

/**
  * User: Jason
  * Date: 2019/5/29
  * Time: 15:47
  */
object Login extends Page {


  override val locationHashString: String = "#/login"

  private val windowHeight = dom.window.innerHeight

  private def signUp(): Unit = {
    val username = dom.document.getElementById("name").asInstanceOf[Input].value
    val password = dom.document.getElementById("pwd").asInstanceOf[Input].value
    if (username.nonEmpty && password.nonEmpty) {
      val data = SignInReq(username, password).asJson.noSpaces
      Http.postJsonAndParse[SignInRsp](Route.User.signUp, data).map {
        rsp =>
          rsp.errCode match {
            case 0 =>
              //              JsFunc.alert(s"注册成功！")
              dom.document.getElementById("username").asInstanceOf[Input].value = username
              dom.document.getElementById("password").asInstanceOf[Input].value = password
            case _ =>
              JsFunc.alert(rsp.msg)
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
      val data = SignInReq(username, password).asJson.noSpaces
      Http.postJsonAndParse[SignInRsp](Route.User.signIn, data).map {
        rsp =>
          rsp.errCode match {
            case 0 =>
              //              JsFunc.alert(s"登陆成功！")
              val userId = rsp.userId.get
              dom.window.localStorage.setItem("username", username)
              //              dom.window.localStorage.setItem("password", password)
              dom.window.localStorage.setItem("userId", userId.toString)
              dom.window.location.hash = s"#/home"
            case _ =>
              JsFunc.alert(rsp.msg)
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

  val Email: Var[Node] = Var(
    <div class="row" style="padding: 1rem 1rem 1rem 1rem;">
      <label class="col-md-3" style="text-align:right">用户名</label>
      <div class="col-md-6">
        <input type="text" id="username" placeholder="用户名" class="form-control" autofocus="true"></input>
      </div>
    </div>
  )

  val PassWord: Var[Node] = Var(
    <div class="row" style="padding: 1rem 1rem 1rem 1rem">
      <label class="col-md-3" style="text-align:right;">密码</label>
      <div class="col-md-6">
        <input type="password" id="password" placeholder="密码" class="form-control" onkeydown={e: KeyboardEvent => loginByEnter(e)}></input>
      </div>
    </div>
  )

  def loginByEnter(event: KeyboardEvent): Unit = {
    if (event.keyCode == 13)
      dom.document.getElementById("logIn").asInstanceOf[Button].click()
  }

  val Title: Var[Node] = Var(
    <div class="row" style="padding:10px">
      <div class="col-md-8 col-md-offset-2" style="text-align: center;font-size: 4rem;">
        geek
      </div>
    </div>
  )

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

  val Btn: Var[Node] = Var(
    <div class="row" style="padding:20px;text-align:center;">
      <div class="col-md-8 col-md-offset-2">
        <button id="logIn" class="btn btn-info" style="margin: 0rem 1rem 0rem 1rem;" onclick={() => signIn()}>
          登陆
        </button>
        <button id="random" class="btn btn-info" style="margin: 0rem 1rem 0rem 1rem;"
                data-toggle="modal" data-target={s"#signUp"}>注册</button>
        <div>
          {makeModal}
        </div>
      </div>
    </div>

  )

  val Form: Var[Node] = Var(
    <div class="row">
      <form class="col-md-8 col-md-offset-2" style="border: 1px solid #dfdbdb;border-radius: 6px;padding:2rem 1rem 2rem 1rem;">
        {Email}{PassWord}
      </form>
    </div>
  )

  override def render: Elem = {
    <div style={s"height:${windowHeight}px;background-color:white;"}>
      <div class="container" style="padding:100px">
        {Title}{Form}{Btn}
      </div>
    </div>
  }

}
