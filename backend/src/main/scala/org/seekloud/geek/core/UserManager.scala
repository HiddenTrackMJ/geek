package org.seekloud.geek.core

import akka.actor.Scheduler
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.stream.scaladsl.Flow
import akka.stream.{ActorAttributes, Supervision}
import akka.util.{ByteString, Timeout}
import org.seekloud.geek.Boot
import org.seekloud.geek.Boot.{executor, scheduler, timeout}
import org.seekloud.geek.models.dao.UserDao
import org.seekloud.geek.shared.ptcl.CommonProtocol._
import org.seekloud.geek.shared.ptcl.WsProtocol.{Wrap, WsMsgClient}
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.language.postfixOps
import scala.util.{Failure, Success}

/**
 * User: hewro
 * Date: 2020/2/2
 * Time: 15:30
 * Description: 处理[[org.seekloud.geek.http.UserService]]的消息
 */
object UserManager {


  trait Command
  private val log = LoggerFactory.getLogger(this.getClass)

  final case class MSignIn(user:SignIn,replyTo:ActorRef[SignInRsp]) extends Command//登录
  final case class MSignIn4Client(user:SignIn,replyTo:ActorRef[SignInRsp]) extends Command//登录

  final case class MSignUp(user:SignUp, replyTo:ActorRef[SignUpRsp]) extends Command//注册

  private final case object BehaviorChangeKey

  case class TimeOut(msg:String) extends Command

  final case class WebSocketFlowSetup(userId:Long, roomId:Long, replyTo:ActorRef[Option[Flow[Message,Message,Any]]]) extends Command

  final case class Register(code:String, userName:String, password:String, replyTo:ActorRef[SignUpRsp]) extends Command

  final case class SetupWs(uidOpt:Long, roomId:Long,replyTo: ActorRef[Option[Flow[Message, Message, Any]]]) extends Command

  private[this] def switchBehavior(ctx: ActorContext[Command], behaviorName: String,
    behavior:Behavior[Command], durationOpt: Option[FiniteDuration] = None,
    timeOut:TimeOut = TimeOut("busy time error"))
    (implicit stashBuffer: StashBuffer[Command],
      timer:TimerScheduler[Command]) ={
    println(s"${ctx.self.path} becomes $behaviorName behavior.")
    timer.cancel(BehaviorChangeKey)
    durationOpt.foreach(timer.startSingleTimer(BehaviorChangeKey, timeOut, _))
    stashBuffer.unstashAll(ctx, behavior)
  }



  def create()(implicit timeout: Timeout, scheduler: Scheduler): Behavior[Command] = {
    log.info("UserManager started.")
    Behaviors.setup[Command] {
      _ =>
        implicit val stashBuffer: StashBuffer[Command] = StashBuffer[Command](Int.MaxValue)
        Behaviors.withTimers[Command] { implicit timer =>
          idle()
        }
    }
  }


  private def idle()
    (implicit stashBuffer: StashBuffer[Command],timer:TimerScheduler[Command]):Behavior[Command] =
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

          case MSignIn4Client(user, replyTo) =>
            UserDao.sigIn4Client(user.userName,user.password).map{
              rsp=>
                if (rsp._1 nonEmpty){//直接登录成功没有注册
                  replyTo ! SignInRsp(Some(UserInfo(rsp._1.get.id,rsp._1.get.name,"")),None,0,"恭喜你登录成功")
                }else{//注册成功的
                  replyTo ! SignInRsp(Some(UserInfo(rsp._2,user.userName,"")),None,0,"恭喜你注册成功")
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

          case SetupWs(uid, roomId,replyTo) =>
            UserDao.searchById(uid).onComplete {
              case Success(f) =>
                if (f.isDefined) {
                  log.debug(s"${ctx.self.path} ws start")
                  val flowFuture: Future[Option[Flow[Message, Message, Any]]] = ctx.self ? (WebSocketFlowSetup(uid,roomId, _))
                  flowFuture.map(replyTo ! _)
                } else {
                  log.debug(s"${ctx.self.path}setup websocket error: the user doesn't exist")
                  replyTo ! None
                }
              case Failure(e) =>
                log.error(s"getBindWx future error: $e")
                replyTo ! None
            }
            Behaviors.same

          case WebSocketFlowSetup(userId, roomId, replyTo) =>
            val existRoom: Future[Boolean] = Boot.roomManager ? (RoomManager.ExistRoom(roomId, _))
            existRoom.map{exist =>
              if(exist){
                log.info(s"${ctx.self.path} websocket will setup for user:$userId")
                getUserActorOpt(userId, ctx) match{
                  case Some(actor) =>
                    log.debug(s"${ctx.self.path} setup websocket error:该账户已经登录userId=$userId")
                    //TODO 重复登录相关处理
                    //                    actor ! UserActor.UserLogin(roomId,userId)
                    //                    replyTo ! Some(setupWebSocketFlow(actor))
                    replyTo ! None

                  case None =>
                    val userActor = getUserActor(userId, ctx)
                    userActor ! UserActor.UserLogin(roomId,userId)
                    replyTo ! Some(setupWebSocketFlow(userActor))
                }


              }else{
                log.debug(s"${ctx.self.path} setup websocket error:the room doesn't exist")
                replyTo ! None
              }
            }
            Behaviors.same

          case UserActor.ChildDead(userId,actor) =>
            log.debug(s"${ctx.self.path} the child = ${userId}")
            Behaviors.same

          case _=>
            log.info("recv unknown msg when create")
            Behaviors.unhandled
        }
    }

  private def getUserActor(userId:Long, ctx: ActorContext[Command]) = {
    val childrenName = s"userActor-$userId"
    ctx.child(childrenName).getOrElse {
      val actor = ctx.spawn(UserActor.create(userId), childrenName)
      ctx.watchWith(actor, UserActor.ChildDead(userId, actor))
      actor
    }.unsafeUpcast[UserActor.Command]
  }

  private def getUserActorOpt(userId:Long, ctx:ActorContext[Command]) = {
    val childrenName = s"userActor-$userId"
    ctx.child(childrenName).map(_.unsafeUpcast[UserActor.Command])
  }

  private def setupWebSocketFlow(userActor:ActorRef[UserActor.Command]):Flow[Message,Message,Any]  = {
    import org.seekloud.byteobject.ByteObject._
    import org.seekloud.byteobject.MiddleBufferInJvm

    import scala.language.implicitConversions

    implicit def parseJsonString2WsMsgClient(s: String): Option[WsMsgClient] = {
      import io.circe.generic.auto._
      import io.circe.parser._

      try {
        val wsMsg = decode[WsMsgClient](s).right.get
        Some(wsMsg)
      } catch {
        case e: Exception =>
          log.warn(s"parse front msg failed when json parse,s=$s,e=$e")
          None
      }
    }
    Flow[Message]
      .collect {
        case TextMessage.Strict(m) =>
          log.debug(s"接收到ws消息，类型TextMessage.Strict，msg-$m")
          UserActor.WebSocketMsg(m)

        case BinaryMessage.Strict(m) =>
          //          log.debug(s"接收到ws消息，类型Binary")
          val buffer = new MiddleBufferInJvm(m.asByteBuffer)
          bytesDecode[WsMsgClient](buffer) match {
            case Right(req) =>
              UserActor.WebSocketMsg(Some(req))

            case Left(e) =>
              log.debug(s"websocket decode error:$e")
              UserActor.WebSocketMsg(None)
          }

        case x =>
          log.debug(s"$userActor recv a unsupported msg from websocket:$x")
          UserActor.WebSocketMsg(None)

      }
      .via(UserActor.flow(userActor))
      .map{
        case t: Wrap =>
          //          val buffer = new MiddleBufferInJvm(16384)
          //          val message = bytesDecode[WsMsgRm](buffer) match {
          //            case Right(rst) => rst
          //            case Left(e) => DecodeError
          //          }
          //
          //          message match {
          //            case HeatBeat(ts) =>
          //              log.debug(s"heartbeat: $ts")
          //
          //            case x =>
          //              log.debug(s"unknown msg:$x")
          //
          //          }
          BinaryMessage.Strict(ByteString(t.ws))

        case x =>
          log.debug(s"websocket send an unknown msg:$x")
          TextMessage.apply("")

      }

      .withAttributes(ActorAttributes.supervisionStrategy(decider = decider))
  }


  private val decider:Supervision.Decider = {
    e:Throwable =>
      e.printStackTrace()
      Supervision.Resume
  }


}
