package org.seekloud.geek.shared.ptcl


object FileProtocol {

  def uploadTourFileError(errorMsg: String) = ErrorRsp(msg = errorMsg,errCode = 90000)

  def deleteFileError(errorMsg: String) = ErrorRsp(msg = errorMsg,errCode = 90001)

  case class uploadSuccessRsp(
                             fileUrl:String,
                             fileName:String,
                             errCode:Int=0,
                             msg:String="ok"
                             ) extends CommonRsp

}
