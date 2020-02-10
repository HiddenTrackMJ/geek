package org.seekloud.geek.common

/**
  * Created by dry on 2018/4/27.
  **/


object Constant {

  val authCodeCharset = Array(
    '2', '3', '4', '5', '6', '7', '8', '9',
    'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'L', 'M', 'N', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'
  )

  //支持文件管理类型
  val fileTypeMap = Map(
    1 -> AppSettings.pdfFilePath,
    2 -> AppSettings.imgFilePath,
    3 -> AppSettings.tourFilePath,
    4 -> AppSettings.videoFilePath
  )

  object vilinIdentity {

    val localManager = "LEAF_LOCAL_MANAGER"
    val user = "LEAF_USER"

  }

  //local管理员session值
//  object localManager {
//
//    val token = ""
//    val createTime = 0l
//    val expires = 86400l
//    val enterpriseId = ""
//    val userId = ""
//    val userName = managerAccount
//    val canAccessKnowLedge = false
//    val canAccessPortal = false
//
//  }

  //支持referer值
//  val refererList = List(
//    AppSettings.refererSetting,
//    AppSettings.refererSetting + "login/developerCenter",
//    AppSettings.refererSetting + "?language=zh",
//    AppSettings.refererSetting + "?language=fzh",
//    AppSettings.refererSetting + "?language=en"
//  )

  object mobileDataKey {
    val registerDocKey = "REGISTER_KEY"
  }


}
