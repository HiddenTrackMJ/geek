package org.seekloud.geek.utils

import java.math.BigInteger
import java.net.{URLDecoder, URLEncoder}
import java.security.MessageDigest
import java.util
import java.util.concurrent.ThreadLocalRandom
import javax.crypto.spec.SecretKeySpec
import javax.crypto.{Cipher, Mac}
import javax.xml.bind.DatatypeConverter

import akka.http.scaladsl.model.headers.HttpCookie
import akka.http.scaladsl.server.Directives.{optionalCookie, setCookie}
import akka.http.scaladsl.server.directives.{BasicDirectives, CookieDirectives}

import scala.util.{Success, Try}

/**
  * User: Taoz
  * Date: 12/4/2016
  * Time: 2:05 PM
  * Most code in this file is come from github akka-http-session,
  * https://github.com/softwaremill/akka-http-session
  * I just simplify it.
  *
  *
  * normal conf:  path should be "/",
  * domain should be none,
  * name should be changed or PLAY_SESSION if necessary,
  * serverSecret must be changed.
  *
  * edit by zhangtao, add "addSession function", 12/20/2016
  * edit by zhangtao, add "removeSession function", 12/21/2016
  * edit by zhangtao, if session decode error in "optionalSession", clean the session, 12/21/2016
  *
  */
trait SessionSupport {


  import SessionSupport._
  import akka.http.scaladsl.server.{Directive0, Directive1}

  val sessionEncoder: SessionEncoder

  val sessionConfig: SessionConfig


  lazy val invalidateSession: Directive0 = optionalCookie(sessionConfig.cookieName).flatMap {
    case Some(_) => CookieDirectives.deleteCookie(createCookieWithString(""))
    case None => BasicDirectives.pass
  }

  def setSession(v: Map[String, String]): Directive0 = setCookie(createCookie(v))

  def addSession(v: Map[String, String]): Directive0 = optionalSession.flatMap {
    case Right(session) =>
      val s = session ++ v
      setCookie(createCookie(s))
    case Left(_) => setCookie(createCookie(v))
  }

  def removeSession(keys: Set[String]): Directive0 = optionalSession.flatMap {
    case Right(session) =>
      val s = session -- keys
      setCookie(createCookie(s))
    case Left(_) => BasicDirectives.pass
  }

  lazy val optionalSession: Directive1[Either[String, Map[String, String]]] =
    optionalCookie(sessionConfig.cookieName).map(c => c.map(p => p.value)).flatMap{
      case Some(data) =>
        decode(data) match {
          case r@Right(_) => BasicDirectives.provide(r)
          case Left(error) =>
            CookieDirectives.deleteCookie(createCookieWithString("")).tflatMap{ _ =>
              BasicDirectives.provide(Left(s"session decode error: $error"))
            }
        }
      case None =>
        BasicDirectives.provide(Left(s"can not find session: ${sessionConfig.cookieName}"))
    }


  /*  lazy val optionalSession: Directive1[Either[String, Map[String, String]]] =
      optionalCookie(sessionConfig.cookieName).map(c => c.map(p => p.value)).map{
        case Some(data) => decode(data)
        case None =>
          Left(s"can not find session: ${sessionConfig.cookieName}")
      }*/


  private def createCookie(v: Map[String, String]) = {
    val dataStr = sessionEncoder.encode(v, System.currentTimeMillis(), sessionConfig)
    createCookieWithString(dataStr)
  }

  private def createCookieWithString(dataStr: String) = {
    val age = if (dataStr.length == 0) Some(0l) else sessionConfig.maxAge
    HttpCookie(
      name = sessionConfig.cookieName,
      value = dataStr,
      expires = None,
      maxAge = None,
      domain = sessionConfig.domain,
      path = sessionConfig.path,
      secure = sessionConfig.secure,
      httpOnly = sessionConfig.httpOnly
    )
  }


  private def decode(data: String): Either[String, Map[String, String]] = {
    sessionEncoder.decode(data, sessionConfig).map { dr =>
      val expired =
        sessionConfig.maxAge.fold(false)(_ => System.currentTimeMillis() > dr.expires.getOrElse(Long.MaxValue))
      if (expired) {
        Left("Cookie expired")
      } else if (!dr.signatureMatches) {
        Left("Corrupt signature")
      } else {
        Right(dr.t)
      }
    }.recover { case t: Exception =>
      t.printStackTrace()
      Left(s"session format error: ${t.getClass.toString}: ${t.getMessage}")
    }.get
  }

}


object SessionSupport {


  case class SessionConfig(
    serverSecret: String,
    cookieName: String,
    sessionEncryptData: Boolean,
    maxAge: Option[Long],
    domain: Option[String],
    path: Option[String],
    secure: Boolean,
    httpOnly: Boolean
  )


  object MapSessionSerializer {
    private def urlEncode(s: String): String = URLEncoder.encode(s, "UTF-8")

    private def urlDecode(s: String): String = URLDecoder.decode(s, "UTF-8")

    def serialize(t: Map[String, String]): String =
      t.map { case (k, v) => urlEncode(k) + "=" + urlEncode(v) }.mkString("&")

    def deserialize(s: String): Try[Map[String, String]] = {
      Try {
        if (s == "") Map.empty[String, String]
        else {
          s.split("&")
            .map(_.split("=", 2))
            .map(p => urlDecode(p(0)) -> urlDecode(p(1)))
            .toMap
        }
      }
    }
  }


  case class DecodeResult(t: Map[String, String], expires: Option[Long], signatureMatches: Boolean)

  trait SessionEncoder {
    def encode(t: Map[String, String], nowMillis: Long, config: SessionConfig): String

    def decode(s: String, config: SessionConfig): Try[DecodeResult]
  }

  /**
    * User: zhaorui
    * Date: 2016/10/19
    * Time: 10:31
    *
    * fix play session, by zhaorui, 2016/12/21
    *
    */
  object PlaySessionEncoder extends SessionEncoder {

    val serializer = MapSessionSerializer

    override def encode(t: Map[String, String], nowMillis: Long, config: SessionConfig): String = {
      val serialized = serializer.serialize(t)
      //val withExpiry = serialized
      val encrypted =
        if (config.sessionEncryptData) {
          Crypto.encrypt_AES(serialized, config.serverSecret)
        } else {
          serialized
        }
      s"${Crypto.sign_HmacSHA1_hex(serialized, config.serverSecret).toLowerCase}-$encrypted"
    }

    override def decode(s: String, config: SessionConfig): Try[DecodeResult] = {
      def extractExpiry(data: String): (Option[Long], String) = {
        config.maxAge.fold((Option.empty[Long], data)) { maxAge =>
          val splitted = data.split("-", 2)
          (Some(splitted(0).toLong), splitted(1))
        }
      }

      Try {
        if(s.equalsIgnoreCase("deleted")){
          Success(
            DecodeResult(Map.empty[String, String], Some(-1l), signatureMatches = false)
          )
        } else {
          val splitted = s.split("-", 2)
          val decrypted = if (config.sessionEncryptData) Crypto.decrypt_AES(splitted(1), config.serverSecret) else splitted(1)
          val serialized = decrypted
          val signatureMatches = SessionUtil.constantTimeEquals(
            splitted(0).toUpperCase,
            Crypto.sign_HmacSHA1_hex(serialized, config.serverSecret)
          )
          serializer.deserialize(serialized).map {
            DecodeResult(_, None, signatureMatches)
          }
        }

      }.flatten
    }
  }


  private object Crypto {

    import SessionUtil._

    def sign_HmacSHA1_hex(message: String, secret: String): String = {
      val key = secret.getBytes("UTF-8")
      val mac = Mac.getInstance("HmacSHA1")
      mac.init(new SecretKeySpec(key, "HmacSHA1"))
      toHexString(mac.doFinal(message.getBytes("utf-8")))
    }

    def sign_HmacSHA256_base64(message: String, secret: String): String = {
      val key = secret.getBytes("UTF-8")
      val mac = Mac.getInstance("HmacSHA256")
      mac.init(new SecretKeySpec(key, "HmacSHA256"))
      DatatypeConverter.printBase64Binary(mac.doFinal(message.getBytes("utf-8")))
    }

    def encrypt_AES(value: String, secret: String): String = {
      val raw = util.Arrays.copyOf(secret.getBytes("utf-8"), 16)
      val skeySpec = new SecretKeySpec(raw, "AES")
      val cipher = Cipher.getInstance("AES")
      cipher.init(Cipher.ENCRYPT_MODE, skeySpec)
      toHexString(cipher.doFinal(value.getBytes("utf-8")))
    }

    def decrypt_AES(value: String, secret: String): String = {
      val raw = util.Arrays.copyOf(secret.getBytes("utf-8"), 16)
      val skeySpec = new SecretKeySpec(raw, "AES")
      val cipher = Cipher.getInstance("AES")
      cipher.init(Cipher.DECRYPT_MODE, skeySpec)
      new String(cipher.doFinal(hexStringToByte(value)))
    }

    def hash_SHA256(value: String): String = {
      val digest = MessageDigest.getInstance("SHA-256")
      toHexString(digest.digest(value.getBytes("UTF-8")))
    }
  }


  private object SessionUtil {
    def randomString(length: Int) = {
      // http://stackoverflow.com/questions/41107/how-to-generate-a-random-alpha-numeric-string
      val random = ThreadLocalRandom.current()
      new BigInteger(length * 5, random).toString(32) // because 2^5 = 32
    }

    /**
      * Utility method for generating a good server secret.
      */
    def randomServerSecret() = randomString(128)

    // Do not change this unless you understand the security issues behind timing attacks.
    // This method intentionally runs in constant time if the two strings have the same length.
    // If it didn't, it would be vulnerable to a timing attack.
    def constantTimeEquals(a: String, b: String) = {
      if (a.length != b.length) {
        false
      }
      else {
        var equal = 0
        for (i <- Array.range(0, a.length)) {
          equal |= a(i) ^ b(i)
        }
        equal == 0
      }
    }

    def toHexString(array: Array[Byte]): String = {
      DatatypeConverter.printHexBinary(array)
    }

    def hexStringToByte(hexString: String): Array[Byte] = {
      DatatypeConverter.parseHexBinary(hexString)
    }
  }


}

