package org.seekloud.geek

/**
  * Created by haoshuhan on 2018/6/4.
  */
object Routes {
  val base = "/geek"

  object Login{
    val baseUrl = base + "/login"
    val userLogin = baseUrl + "/userLogin"
    val userLogout = baseUrl + "/userLogout"
  }
  object Register{
    val baseUrl = base + "/register"
    val userRegister = baseUrl + "/userRegister"
    val userRegisterIn = baseUrl + "/userRegisterIn"
    val userRegisterOut = baseUrl + "/userRegisterOut"
  }

  object List {
    val baseUrl = base + "/list"
    val getList = baseUrl + "/getList"
    val addRecord = baseUrl + "/addRecord"
    val delRecord = baseUrl + "/delRecord"
    val getLike = baseUrl + "/getLike"
  }

  object Visit {
    val baseUrl = base + "/visit"
    val getFollowList = baseUrl + "/getFollowList"
    val getList = baseUrl + "/getList"
    val addComment = baseUrl + "/addComment"
    val delComment = baseUrl + "/delComment"
    val delFollow = baseUrl + "/delFollow"
  }
  object Comment {
    val baseUrl = base + "/comment"
    val getList = baseUrl + "/getList"
    val getContent = baseUrl + "/getContent"
    val addComment = baseUrl + "/addComment"
    val delComment = baseUrl + "/delComment"
  }

  object Recent {
    val baseUrl = base + "/recent"
    val getRecentList = baseUrl + "/getRecentList"
    val addFollow = baseUrl + "/addFollow"
    val addLike = baseUrl + "/addLike"
    val delLike = baseUrl + "/delLike"
    val getSearchList = baseUrl + "/getSearchList"


  }

}
