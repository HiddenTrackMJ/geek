package org.seekloud.geek.shared

/**
  * User: Jason
  * Date: 2019/5/23
  * Time: 11:54
  */
package object ptcl {

  trait Request

  trait CommonRsp {
    val errCode: Int
    val msg: String
  }

  final case class ErrorRsp(
    errCode: Int,
    msg: String
  ) extends CommonRsp

  final case class SuccessRsp(
    errCode: Int = 0,
    msg: String = "ok"
  ) extends CommonRsp

  final case class ComRsp(
    errCode: Int = 0,
    msg: String = "ok"
  ) extends CommonRsp


  object CommonErrorCode {
        val jsonFormatError = ErrorRsp(msg = "json parse error.", errCode = 1000005)
        val noSessionError = ErrorRsp(msg = "no session,need to login", errCode = 1000006)
        val accountNoExitError = ErrorRsp(msg = "account no exit", errCode = 1000007)
        val passwordError = ErrorRsp(msg = "wrong password", errCode = 1000008)
  }

}
