package org.seekloud.geek.pages
import mhtml.{Rx, Var, emptyHTML}
import org.scalajs.dom
import org.scalajs.dom.html.{Button, Input}
import org.seekloud.geek.utils.{Http, JsFunc, Modal, Page, TimeTool}
import org.scalajs.dom.raw.{Event, FileList, FormData, HTMLElement, KeyboardEvent}
import io.circe.generic.auto._
import io.circe.syntax._
import org.seekloud.geek.common.Route
import org.seekloud.geek.shared.ptcl.FileProtocol._
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
object UserInfoPagetest extends Page{
  private val currentPage = Var(1)

  val types = Var(1) //2用户详情 1更新用户信息

  val pic = Var(HestiaImage("",""))
  var pic2 = HestiaImage("","")

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

                    setPic(HestiaImage(imgName,imgUrl,None,System.currentTimeMillis))
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

    dom.document.body.style = "background-image: url('/geek/static/img/bg1.png');" +
      "background-attachment: fixed;" +
      "background-size: cover;" +
      "background-position: center;" +
      "background-repeat: no-repeat;"
  }


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
            <img style="cursor:pointer;height:30px;width:30px;"  src={Route.hestiaPath("1001.jpg")} ></img>
            <input class="hiddenIcon" type="file" onchange={(e:Event) => println(s"配图===========");uploadFunc(e.target.asInstanceOf[Input],e.target.asInstanceOf[Input].files, pic, s"img/test")}></input>

          </div>
          <div class="createCourse-item">
            <div class="test">昵称：</div>
            <input placeholder="不限"  class="test-input"></input>
          </div>
          <div class="createCourse-item">
            <div class="test">id号：</div>
            <input placeholder="123456" disabled="disabled" style="background: #f0f0f0;" class="test-input"></input>
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


  def gotoPage(path: String): Unit = {
    dom.window.location.hash = s"#/$path"
  }

  override def render: Elem = {
    init()
    //    dom.document.documentElement.appendChild({renderWebm()})

    <div style="margin:0 0;">
      {loadingHtml}
      {courseLists}
    </div>

  }
}
