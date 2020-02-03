package org.seekloud.geek.core

import akka.actor.Scheduler
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.server.Directives.complete
import akka.util.Timeout
import org.seekloud.geek.models.dao.UserDao
import org.seekloud.geek.Boot.executor
import org.seekloud.geek.shared.ptcl.CommonProtocol.{SignIn, SignInRsp, SignUp, SignUpRsp, UserInfo}
import org.slf4j.LoggerFactory

import scala.language.postfixOps

/**
 * User: hewro
 * Date: 2020/2/2
 * Time: 15:30
 * Description: 处理[[org.seekloud.geek.http.UserService]]的消息
 */
object UserManager {


  sealed trait Command
  private val log = LoggerFactory.getLogger(this.getClass)


  case class MSignIn(user:SignIn,replyTo:ActorRef[SignInRsp]) extends Command//登录
  case class MSignUp(user:SignUp,replyTo:ActorRef[SignUpRsp]) extends Command//注册

  def create()(implicit timeout: Timeout, scheduler: Scheduler) = Behaviors.setup[Command] {
    _ =>
      log.info("UserManager started.")
      Behaviors.receive[Command] {
        (ctx, msg) =>
          msg match {
            case MSignIn(user, replyTo)=>
              UserDao.sigIn(user.userName,user.password).map{
                rsp=>
                  println("登录:" + rsp)
                  if (rsp isEmpty){//登录失败
                    replyTo ! SignInRsp(None,None,-1,"再想想你的用户名或者密码")
                  }else{
                    replyTo ! SignInRsp(Some(UserInfo(rsp.get.id,rsp.get.name,"")),None,0,"恭喜你登录成功")
                  }
              }
              Behaviors.same
            case MSignUp(user, replyTo)=>
              UserDao.signUp(user.userName,user.password).map{
                rsp=>
                  var msg = ""
                  if (rsp == -1){
                    msg = "注册失败，您的名字已被抢注"
                  }else{
                    msg = "恭喜你，注册成功！"
                  }
                  replyTo ! SignUpRsp(rsp)
              }
              Behaviors.same
            case _=>
              log.info("收到未知消息create")
              Behaviors.unhandled
          }
      }
  }


}
