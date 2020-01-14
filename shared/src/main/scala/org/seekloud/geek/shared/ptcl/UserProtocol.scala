package org.seekloud.geek.shared.ptcl

/**
  * User: TangYaruo
  * Date: 2019/6/3
  * Time: 14:55
  */
object UserProtocol {

  trait Request

  trait Response {
    val errCode: Int
    val msg: String
  }


  /**
    * 用户注册登录相关
    *
    * errCode: 2000001 ~
    *
    * */


  case class SignInReq(
    username: String,
    password: String
  ) extends Request

  case class SignInRsp(
    userId: Option[Long] = None,
    errCode: Int = 0,
    msg: String = "ok"
  ) extends Response

  /*sign up*/
  val NicknameInvalid = SignInRsp(errCode = 200001, msg = "该用户名已被注册！")

  val SignUpFail = SignInRsp(errCode = 200002, msg = "服务器出错，请重试！")

  val PasswordError = SignInRsp(errCode = 200003, msg = "密码错误！")

  val NicknameError = SignInRsp(errCode = 200004, msg = "该用户名不存在！")








}
