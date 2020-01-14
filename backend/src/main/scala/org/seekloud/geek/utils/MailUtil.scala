package org.seekloud.geek.utils

import java.util.{Date, Properties}

import com.sun.mail.util.MailSSLSocketFactory
import javax.mail.Message.RecipientType
import javax.mail.internet.{InternetAddress, MimeBodyPart, MimeMessage, MimeMultipart}
import javax.mail.{Address, Authenticator, PasswordAuthentication, Session, Transport}
import org.seekloud.geek.common.AppSettings
/**
  * Author: Jason
  * Date: 2020/1/5
  * Time: 12:43
  */
object MailUtil {
  def send(toAddress: String, str: String, title: String): Unit = {
    val session = getEbuptSession
    val message = new MimeMessage(session)
    message.setFrom(new InternetAddress(AppSettings.addresserEmail))

    var index = 0
    val toAddressList = new Array[Address](toAddress.split("\\;").length) //多个邮箱都发邮件
    toAddress.split("\\;").foreach { a =>
      toAddressList(index) = new InternetAddress(a)
      index = index + 1
    }
    message.setRecipients(RecipientType.TO, toAddressList)

    message.setSubject(title)
    message.setSentDate(new Date)
    val mainPart = new MimeMultipart
    val html = new MimeBodyPart
    html.setContent(str, "text/html; charset=utf-8")
    mainPart.addBodyPart(html)
    message.setContent(mainPart)
    Transport.send(message)
  }

  def getProperties: Properties = {
    val p = new Properties
    val sf = new MailSSLSocketFactory();
    sf.setTrustAllHosts(true);
    p.put("mail.smtp.ssl.enable","true")
    p.put("mail.smtp.ssl.socketFactory",sf)
    p.put("mail.smtp.host", AppSettings.emailHost)
    p.put("mail.smtp.port", AppSettings.emailPort)
    p.put("mail.transport.protocol", "smtp")
    p.put("mail.smtp.auth", "true")
    p.put("mail.smtp.starttls.enable", "true")
    p
  }

  case class MyAuthenticator(userName: String, password: String) extends Authenticator {

    override def getPasswordAuthentication: PasswordAuthentication = {
      new PasswordAuthentication(userName, password)
    }
  }

  def getEbuptSession: Session = {
    Session.getInstance(getProperties, new MyAuthenticator(AppSettings.addresserEmail, AppSettings.addresserPwd))
  }

  val toAddress = "1978354221@qq.com"

  def main(args: Array[String]): Unit = {
//    1978354221@qq.com
  }
}
