package org.seekloud.geek.pages

import org.scalajs.dom
import org.scalajs.dom.html.Input
import org.seekloud.geek.Routes
import org.seekloud.geek.shared.ptcl.SuccessRsp
import org.seekloud.geek.shared.ptcl.UserProtocol.{SignInReq, SignUpReq}
import org.seekloud.geek.utils.{Http, JsFunc}
import io.circe.generic.auto._
import io.circe.syntax._
import scala.xml.Node
import scala.concurrent.ExecutionContext.Implicits.global
/**
  * User: XueGanYuan
  * Date: 2019/9/22
  * Time: 11:49
  */
object Signup{

  val url = "#/" + "Register"

//  private def userRegister(): Unit ={
//    val userName = dom.document.getElementById("userName").asInstanceOf[Input].value
//    val password = dom.document.getElementById("userPassword").asInstanceOf[Input].value
//    val email = dom.document.getElementById("userEmail").asInstanceOf[Input].value
//    Http.postJsonAndParse[SuccessRsp](Routes.Register.userRegister, UserRegisterReq(userName, password, email).asJson.noSpaces).map{
//      case Right(rsp) =>
//        if(rsp.errCode == 0){
//          JsFunc.alert("注册成功")
//          dom.window.location.hash = "/Login"
//        }
//        else{
//          JsFunc.alert(s"注册失败：${rsp.msg}")
//        }
//      case Left(error) =>
//        JsFunc.alert(s"parse error,$error")
//    }
//  }


  private def userRegister(): Unit = {
    val userName = dom.document.getElementById("userName").asInstanceOf[Input].value
    val password = dom.document.getElementById("userPassword").asInstanceOf[Input].value
    //    val email = dom.document.getElementById("userEmail").asInstanceOf[Input].value
    Http.postJsonAndParse[SuccessRsp](Routes.Register.userRegister, SignUpReq(userName, password).asJson.noSpaces).map {
      rsp =>
        if (rsp.errCode == 0) {
          JsFunc.alert("注册成功")
          dom.window.location.hash = "/Login"
        }
        else {
          JsFunc.alert(s"注册失败：${rsp.msg}")
        }
    }
  }
  private def userRegisterOut(): Unit ={
          JsFunc.alert("返回登陆页面")
          dom.window.location.hash = "/Login"
  }


//  <span>邮箱</span>
//    <input id = "userPassword" type = "password"></input>


  def app: Node =
    <div>
      <div class = "LoginForm">
        <h2>注册</h2>
        <div class = "inputContent">
          <span>用户名</span>
          <input id = "userName"></input>
        </div>
        <div class = "inputContent">
          <span>密码</span>
          <input id = "userPassword" type = "password"></input>
        </div>
        <div class = "inputContent">
          <span>邮箱</span>
          <input id = "userEmail"></input>
        </div>
        <button class="mdui-btn mdui-btn-raised mdui-ripple" onclick = {()=> userRegister()}>注册</button>
        <button class="mdui-btn mdui-btn-raised mdui-ripple" onclick={()=>userRegisterOut()}>返回登陆</button>

      </div>
    </div>
}

