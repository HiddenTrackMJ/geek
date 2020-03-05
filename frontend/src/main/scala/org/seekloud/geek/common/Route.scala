package org.seekloud.geek.common

/**
  * User: Jason
  * Date: 2019/5/24
  * Time: 15:34
  */

object Route {

  val baseUrl = "/geek"

  def imgPath(fileName: String): String = baseUrl + "/static/img/" + fileName
//  def hestiaPath(fileName: String): String =  "http://127.0.0.1:30226/hestia/files/image/OnlyForTest/" + fileName
//  def hestiaPath(fileName: String): String =  "http://10.1.29.247:30226/hestia/files/image/OnlyForTest/" + fileName
  def hestiaPath(fileName: String): String =  "http://47.92.170.2:42076/hestia/files/image/OnlyForTest/" + fileName
//  val hestiaPath = "http://10.1.29.247/hestia/files/image/OnlyForTest/"
  object Admin {
    val base: String = baseUrl + "/admin"
  }

  object Room {
    val base: String = baseUrl + "/room"
    val createRoom: String = base + "/createRoom"
    val startLive: String = base + "/startLive"
    val stopLive: String = base + "/stopLive"
    val getRoomList: String = base + "/getRoomList"
    val getUserInfo: String = base + "/getUserInfo"
    val updateRoomInfo: String = base + "/updateRoomInfo"
    val getRoomSectionList:String =base +"/getRoomSectionList"
    val getRoomIdList:String =base +"/getRoomIdList"
    val getCommentList:String =base +"/getRoomCommentList"
    val addComment:String =base +"/addRoomComment"
    val delComment:String =base +"/delComment"

  }

  object User {
    val base: String = baseUrl + "/user"
    val signUp: String = base + "/signUp"
    val signIn: String = base + "/signIn"
    val getUserDetail:String = base + "/getUserDetail"
    val updateUserDetail:String = base + "/updateUserDetail"
    val updateAvatar:String = base + "/updateAvatar"
  }

  object Invitation {
    val base: String = baseUrl + "/invitation"
    val getInviterList: String = base + "/getInviterList"
    val getInviteeList: String = base + "/getInviteeList"
    val getInviteDetail: String = base + "/getInviteDetail"
    val delInvitee: String = base + "/delInvitee"
    val signIn: String = base + "/signIn"
    val addInvitee:String = base + "/addInvitee"
    val checkInvitee:String =base +"/checkInvitee"
  }

  object Test {
    val base: String = baseUrl + "/tests"
    val getInviterList: String = base + "/delete"
    val getInviteeList: String = base + "/resizeGet"
  }

  object File {
    val base = baseUrl + "/file"
    def upload(path: String) = base + s"/uploadFile?targetDir=$path&fileType=2"
    def uploadSlice(path: String, fileType: Int, fileName: String, size: Long, sliceSize: Long, end: Double, pointer: Int) = base + s"/uploadSliceFile?targetDir=$path&fileType=2&fileName=$fileName&size=$size&sliceSize=$sliceSize&end=$end&pointer=$pointer"
    def delete(name: String, fileType: Int) = base + s"/deleteFile?targetName=$name&fileType=2"
    def deleteTmpFile(path: String, fileType: Int, fileName: String) = base + s"/deleteTmpFile?targetDir=$path&fileType=2&fileName=$fileName"
    def checkPdf(tName: String) = base + s"/checkPdf?name=$tName&timestamp=${System.currentTimeMillis()}"
  }



}
