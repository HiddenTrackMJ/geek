package org.seekloud.geek.utils

import java.io.File

import org.seekloud.geek.common.AppSettings._
import org.seekloud.geek.utils.HttpUtil.Imports._
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by Anjiansan on 2018/03/26.
  **/
object HestiaClient {

  case class uploadRsp(fileName: String, errCode: Int, msg: String)

  case class uploadRep(appId: String, sn: String, timestamp: String, nonce: String, signature: String, url: String, fileName: String = "")

  private val hestiaBaseUrl = hestiaProtocol + "://" + hestiaDomain

  private val appId = hestiaAppId
  private val secureKey = hestiaSecureKey
  //  private val hestiaImgPrefix = hestiaProtocol + "://" + hestiaDomain + s"/hestia/files/image/$appId/"
  private val hestiaImgPrefix = hestiaBaseUrl + s"/hestia/files/image/$appId/"

  def genImgUrl(uri: String) = hestiaImgPrefix + uri

  def upload(url: String) = {
    val uploadUrl = hestiaBaseUrl + "/hestia/files/uploadByUrl"
    val sn = appId + System.currentTimeMillis().toString
    val (timestamp, nonce, signature) = SecureUtil.generateSignatureParameters(List(appId, sn), secureKey)

    val params = uploadRep(appId, sn, timestamp, nonce, signature, url).asJson.noSpaces

    postJsonRequestSend(s"upload $url", uploadUrl,
      List(
        "appId" -> appId,
        "sn" -> sn,
        "timestamp" -> timestamp,
        "nonce" -> nonce,
        "signature" -> signature,
        "url" -> url
      ), params).map {
      case Right(str) =>
        decode[uploadRsp](str) match {
          case Right(rsp) =>
            if (rsp.errCode == 0)
              Right(rsp.fileName)
            else {
              log.error(s"upload $url error.error:${rsp.msg}")
              Left(s"${rsp.msg}")
            }

          case Left(e) =>
            log.error(s"upload $url parse error.$e")
            Left(s"Error.$e")
        }

      case Left(e) =>
        log.error(s"upload $url failed:" + e)
        Left(s"Error.$e")
    }
  }

  def upload(file: File, fileName: String) = {
    val uploadUrl = hestiaBaseUrl + "/hestia/files/upload"
    log.info(s"url :${uploadUrl}")
    val sn = appId + System.currentTimeMillis().toString
    val (timestamp, nonce, signature) = SecureUtil.generateSignatureParameters(List(appId, sn), secureKey)
    postFileRequestSend(s"upload ${file.getName}", uploadUrl,
      List(
        "appId" -> appId,
        "sn" -> sn,
        "timestamp" -> timestamp,
        "nonce" -> nonce,
        "signature" -> signature
      ), file, fileName).map {
      case Right(str) =>
        decode[uploadRsp](str) match {
          case Right(rsp) =>
            if (rsp.errCode == 0) {
              Right(rsp.fileName)
            }
            else {
              log.error(s"upload ${file.getName}  error.error:${rsp.msg}")
              Left(s"${rsp.msg}")
            }

          case Left(e) =>
            log.error(s"upload ${file.getName}  parse error.$e")
            Left(s"Error.$e")
        }

      case Left(e) =>
        log.error(s"upload ${file.getName} failed:" + e)
        Left(s"Error.$e")
    }
  }


  def main(args: Array[String]): Unit = {
    val f = new File("C:\\Users\\Arrow\\Desktop\\门户-云视讯客户端banner_slices\\fzh_ysxbanner.png")
    upload(f, "fzh_ysxbanner.png").map {
      case Right(rst) => println(s"rst: $rst")
      case Left(e) =>
    }
  }

}
