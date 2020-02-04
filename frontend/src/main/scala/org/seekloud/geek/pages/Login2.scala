package org.seekloud.geek.pages

import org.scalajs.dom
import org.scalajs.dom.html.Input
import org.seekloud.geek.Routes
import org.seekloud.geek.shared.ptcl.SuccessRsp
import org.seekloud.geek.shared.ptcl.UserProtocol.{SignInReq, SignInRsp}
import org.seekloud.geek.utils.{Http, JsFunc}
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.parser._
import scala.xml.Node
import scala.concurrent.ExecutionContext.Implicits.global
/**
  * User: xgy
  * Date: 2020/2/4
  * Time: 13:24
  */
object Login2 {

  val url = "#/" + "Login"

  private def userLogin(): Unit = {
    val userName = dom.document.getElementById("userName").asInstanceOf[Input].value
    val password = dom.document.getElementById("userPassword").asInstanceOf[Input].value
    Http.postJsonAndParse[SignInRsp](Routes.Login.userLogin, SignInReq(userName, password).asJson.noSpaces).map {
      rsp =>
        if (rsp.errCode == 0) {
          JsFunc.alert("登陆成功")
          dom.window.location.hash = "/List"
        }
        else {
          JsFunc.alert(s"登陆失败：${rsp.msg}")
        }
    }
  }
  private def userRegisterIn(): Unit = {
    

    //dom.window.location.hash = "/Register"
//    val userName = dom.document.getElementById("userName").asInstanceOf[Input].value
//    val password = dom.document.getElementById("userPassword").asInstanceOf[Input].value
//    Http.postJsonAndParse[SuccessRsp](Routes.Login.userLogin, UserLoginReq(userName, password).asJson.noSpaces).map {
//      case Right(rsp) =>
//        if (rsp.errCode == 0) {
          JsFunc.alert("进入注册页面")
          dom.window.location.hash = "/Register"
//        }
//        else {
//          JsFunc.alert(s"登陆失败：${rsp.msg}")
//        }
//      case Left(error) =>
//        JsFunc.alert(s"parse error,$error")
//    }

  }
//


  def render: Node =
    <div>
      <div>
        <div class="header">
          <div class="logo">
            <img src="编组@2x.png" />
          </div>
          <div class="logo2">课管中心</div>
          <button class="return"><i class="inner">退出</i></button>
          <div class="school">清华大学附属中学</div>


        </div>
      </div>
      <div class="LoginForm">
        <h2>登陆</h2>
        <div class="inputContent">
          <span>用户名</span>
          <input id="userName"></input>
        </div>
        <div class="inputContent">
          <span>密码</span>
          <input id="userPassword" type="password"></input>
        </div>
        <button class="mdui-btn mdui-btn-raised mdui-ripple" onclick={() => userLogin()}>登陆</button>
        <button class="mdui-btn mdui-btn-raised mdui-ripple" onclick={() => userRegisterIn()}>注册页面</button>

      </div>
    </div>
}

//<h2>欢迎登陆</h2>