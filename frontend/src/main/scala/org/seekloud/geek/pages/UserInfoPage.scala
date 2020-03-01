package org.seekloud.geek.pages
import mhtml.{Rx, Var, emptyHTML}
import org.scalajs.dom
import org.scalajs.dom.html.{Button, Input,Select}
import org.seekloud.geek.utils.{Http, JsFunc, Modal, Page, TimeTool}
import org.scalajs.dom.raw.{Event, FileList, FormData, HTMLElement, KeyboardEvent}
import io.circe.generic.auto._
import io.circe.syntax._
import org.seekloud.geek.common.Route
import org.seekloud.geek.shared.ptcl.CommonProtocol.{GetUserReq, GetUserRsp, UpdateUserReq, UserInfoDetail, UpdateAvatarReq}
import org.seekloud.geek.shared.ptcl.FileProtocol._
import org.seekloud.geek.shared.ptcl.SuccessRsp

import org.seekloud.geek.utils.RegEx
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

  val pic = Var(HestiaImage("",""))
  var pic2 = HestiaImage("","")
  val userDetail = Var(UserInfoDetail(1,"",Some(""),Some(1),Some(1),Some("")))

  val loadingState = Var(0)//上传状态



  case class HestiaImage(
                          fileName: String = "",
                          hestiaUrl: String = "",
                          redirectUrl: Option[String] = None,
                          updateTime: Long = System.currentTimeMillis()
                        )

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

  userInfoDetail.update(_=>Some(UserInfo(1: Long, "ss": String, 1: Int, "ss": String, "ss": String, "ss": String, 1: Long, "ss": String, "ss": String, 1: Int, 1: Long, 1: Double, None, 1: Int, None, None)))

  private val loadingHtml = loadingState.map{
    case 0 =>
      emptyHTML
    case 1=>
      <div id="tosat" style="z-index:10001">
        <div class="weui-mask weui-mask_transparent" style="z-index:10001"></div>
        <div class="weui-toast" style="z-index:10001">
          <i class="weui-icon_toast weui-loading"></i>
          <p class="weui-toast__content">正在上传</p>
        </div>
      </div>
    case _=>
      emptyHTML
  }//上传文件判断过程

  def setPic(newPic:HestiaImage): Unit ={
    pic2 = newPic
    pic := newPic
  }

  def uploadFunc(input:Input, files:FileList ,pic:Var[HestiaImage], path: String):Unit = {
    if(input.value != null){
      if(input.value.length > 0) {
        val attachName = input.value.split("\\\\").last
        val file = files(0)
        val fileSize = file.size / 1024
        if(fileSize >= 500 || (!attachName.contains("png") && !attachName.contains("jpg") && !attachName.contains("svg"))) {
          JsFunc.alert("格式必须为png/jpg/svg，且大小必须小于500K！")
          input.value = ""
        }
        else {
          val form = new FormData()
          form.append("fileUpload", file)
          loadingState := 1
            Http.postFormAndParse[uploadSuccessRsp](Route.File.upload(path), form).map {
              case Right(rsp) =>
                loadingState := 0
                if (rsp.errCode != 0) {
                  println(s"upload error ${rsp.errCode} ")
                  JsFunc.alert("图片上传出错，请稍后再试")
                } else {
                  try {
                    var imgName = rsp.fileName
                    var  imgUrl = rsp.fileUrl
//                    println(imgName+" sss"+imgUrl)
                    setPic(HestiaImage(imgName,imgUrl,None,System.currentTimeMillis))
                    updateAvatar(imgUrl)
                    input.value = ""
                  } catch {
                    case e: Exception =>
                      println(s"catch upload rsp add error ${e.getMessage}")
                  }
                }
              case Left(e) =>
                loadingState := 0
                println(s"upload parse error in login $e ")
                JsFunc.alert("上传图片出错")
            }
        }
      }
    }
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
//    getUserInfo

    dom.document.body.style = "background-image: url('/geek/static/img/bg1.png');" +
      "background-attachment: fixed;" +
      "background-size: cover;" +
      "background-position: center;" +
      "background-repeat: no-repeat;"
  }

  def gotoPage(path: String): Unit = {
    dom.window.location.hash = s"#/$path"
  }

  def getUserInfo: Unit ={
    println("start getuserinfo")
    val userId = dom.window.localStorage.getItem("userId").toLong
    Http.postJsonAndParse[GetUserRsp](Route.User.getUserDetail, GetUserReq(userId).asJson.noSpaces).map {
      rsp =>
        println(rsp)
        if (rsp.errCode == 0) {
            userDetail :=rsp.userInfo.get

            dom.document.getElementById("userName").asInstanceOf[Input].value = rsp.userInfo.get.userName
            dom.document.getElementById("userGender").asInstanceOf[Select].value =rsp.userInfo.get.gender.get.toString
            dom.document.getElementById("userAge").asInstanceOf[Input].value =rsp.userInfo.get.age.get.toString
            dom.document.getElementById("userAddress").asInstanceOf[Input].value =rsp.userInfo.get.address.get

          println("sss"+rsp)
        } else {
          //          JsFunc.alert(rsp.msg)
          println(rsp.msg)
        }
        println("end getuserinfo")
    }
  }



//  def getUserInfo: Unit ={
//    println("start getuserinfo")
//    val userId = dom.window.localStorage.getItem("userId").toLong
//    Http.postJsonAndParse2[GetUserRsp](Route.User.getUserDetail, GetUserReq(userId).asJson.noSpaces).map {
//      case Right(rsp) =>
//        println(rsp)
//        if (rsp.errCode == 0) {
//          userDetail :=rsp.userInfo.get
//          println("sss"+rsp)
//        } else {
//          //          JsFunc.alert(rsp.msg)
//          println(rsp.msg)
//        }
//        println("end getuserinfo")
//
//    case Left(error) =>
//    println(s"request sent complete, but error2 happen: $error")
//    throw new IllegalArgumentException(s"parse error: $error")
//  }}
//  userId: Long,
//  userName: String,
//  //                            avatar:String,
//  gender:Int,
//  age:Int,
//  address:String

  def updateUserInfo: Unit ={
    val userId = dom.window.localStorage.getItem("userId").toLong
    var username = ""
    var gender = ""
    var age = ""
    var address = ""



    username = dom.document.getElementById("userName").asInstanceOf[Input].value.replace(" ","")
    gender = dom.document.getElementById("userGender").asInstanceOf[Select].value.replace("-","")
    age = dom.document.getElementById("userAge").asInstanceOf[Input].value.replace(" ","")
    address = dom.document.getElementById("userAddress").asInstanceOf[Input].value.replace(" ","")

    if(username=="" || gender=="" || age == "" || address ==""){

      JsFunc.alert("信息没有填写完整！")
//      println(username,gender,age,address)
//      println("这是UserDetail"+userDetail)

    }else if(RegEx.checkAge(age)){
      JsFunc.alert("年龄严格限制在1~199")
    }
    else{
      Http.postJsonAndParse[SuccessRsp](Route.User.updateUserDetail, UpdateUserReq(userId,username,gender.toInt,age.toInt,address).asJson.noSpaces).map {
        rsp =>
          if (rsp.errCode == 0) {
            //          userDetail :=rsp.userInfo.get
            //          dom.window.localStorage.setItem("username",username)
            getUserInfo
          } else {
            JsFunc.alert(rsp.msg)
//            println(rsp.msg)
          }
      }

    }


  }


  def updateAvatar(imgUrl:String): Unit ={
    val userId = dom.window.localStorage.getItem("userId").toLong
    Http.postJsonAndParse[SuccessRsp](Route.User.updateAvatar, UpdateAvatarReq(userId,imgUrl).asJson.noSpaces).map {
      rsp =>
        if (rsp.errCode == 0) {
          getUserInfo
          println("updateAvatarSuccess")
        } else {
          //          JsFunc.alert(rsp.msg)
          println(rsp.msg)
        }
    }

  }


  def closeUserInfo(): Unit = {
    dom.document.location.href="#/home"
  }


  val userDetailLists: Rx[Elem] =userDetail.map {user=>
    val gender = user.gender.getOrElse("") match {
      case 0 => "男"
      case 1 => "女"
      case _ => ""
    }
    <div class="creatCourse">
      <div class="creatCourse-titleA">
        <div class="createCourse-title">个人详情</div>
        <img src="/geek/static/img/关闭.svg" onclick={() => closeUserInfo()}></img>
      </div>
      <div class="createCouse-content">
        <div class="createCourse-list-1" style="width:40%;margin-left:10%">
          <div class="createCourse-item">
            <div class="test">头像选择：</div>
            <img style="cursor:pointer;height:30px;width:30px;" src={Route.hestiaPath(user.avatar.getOrElse("be8feec67e052403e26ec05559607f10.jpg"))}></img>
            <div class="hiddenIcon">
              <input class="hiddenIcon" type="file" onchange={(e: Event) => println(s"配图==========="); uploadFunc(e.target.asInstanceOf[Input], e.target.asInstanceOf[Input].files, pic, s"img/test")}></input>
            </div>

          </div>
          <div class="createCourse-item">
            <div class="test" >昵称：</div>
            <input placeholder={"请输入昵称"} class="test-input" id="userName"></input>
          </div>
          <div class="createCourse-item">
            <div class="test">id号：</div>
            <input placeholder={user.userId.toString} disabled="disabled" style="background: #f0f0f0;" class="test-input"></input>
          </div>
          <div class="createCourse-item">
            <div class="test">性别：</div>
            <select id="userGender" class="arrow-img">
              <option value="-" disabled="true" selected="true" hidden="true">{gender}</option>
              <option value="0">男</option>
              <option value="1">女</option>
            </select>
          </div>{""}<div class="createCourse-item">
          <div class="test">年龄：</div>
          <input maxlength="3" placeholder={"请输入年龄"} class="test-input" id="userAge"></input>
        </div>
          <div class="createCourse-item" style="align-items: flex-start;">
            <div class="test" style="margin-top: 4px">居住地址：</div>
            <div>
              {""}<input placeholder={"请输入地址"} id="userAddress" class="test-input"></input>
            </div>
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
            <textarea placeholder="请描述下房间" disabled="disabled"></textarea>
          </div>


        </div>
      </div>
      <div class="submit" onclick={() => updateUserInfo; getUserInfo}>更新信息</div>
    </div>
  }



  override def render: Elem = {
    init()
    getUserInfo
    //    dom.document.documentElement.appendChild({renderWebm()})

    <div style="margin:0 0;">
      {loadingHtml}
      {userDetailLists}
    </div>

  }
}
