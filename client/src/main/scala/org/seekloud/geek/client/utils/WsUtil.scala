package org.seekloud.geek.client.utils

import akka.Done
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.{ActorContext, TimerScheduler}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage, WebSocketRequest}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.typed.scaladsl.ActorSource
import akka.util.{ByteString, ByteStringBuilder}
import org.seekloud.byteobject.ByteObject.bytesDecode
import org.seekloud.byteobject.MiddleBufferInJvm
import org.seekloud.geek.client.controller.GeekHostController
import org.seekloud.geek.client.core.RmManager.{GetSender, RmCommand}
import org.slf4j.LoggerFactory
import org.seekloud.geek.client.Boot.{executor, materializer, scheduler, system, timeout}
import org.seekloud.byteobject.ByteObject._
import org.seekloud.geek.shared.ptcl.WsProtocol.{CompleteMsgClient, DecodeError, FailMsgClient, TextMsg, WsMsgClient, WsMsgFront, WsMsgRm}

import scala.concurrent.Future

/**
 * User: hewro
 * Date: 2020/2/5
 * Time: 22:18
 * Description: websocket 管理的一些工具方法
 * client  hostController 与 后端建立ws链接
 */
object WsUtil {

  private val log = LoggerFactory.getLogger(this.getClass)


  def buildWebSocket(
    ctx: ActorContext[RmCommand],
    url: String,
    controller: GeekHostController,
    successFunc: => Unit,
    failureFunc: => Unit)(
    implicit timer: TimerScheduler[RmCommand]
  ): Unit = {
    log.debug(s"build ws with roomManager: $url")
    val webSocketFlow = Http().webSocketClientFlow(WebSocketRequest(url))
    //c -> b，获取后端的对象
    val source = getSource(ctx.self)

    //b->c backend给client发消息
    val sink = getRMSink(controller)

    val (stream, response) =
      source
        .viaMat(webSocketFlow)(Keep.both)
        .toMat(sink)(Keep.left)
        .run()
    val connected = response.flatMap { upgrade =>
      if (upgrade.response.status == StatusCodes.SwitchingProtocols) {
        ctx.self ! GetSender(stream) //获取后端的对象
        successFunc
        Future.successful(s"link room manager success.")
      } else {
        failureFunc
        throw new RuntimeException(s"link room manager failed: ${upgrade.response.status}")
      }
    } //链接建立时
    connected.onComplete(i => log.info(s"ws connect error:${i.toString}"))
  }


  def getSource(rmManager: ActorRef[RmCommand]): Source[BinaryMessage.Strict, ActorRef[WsMsgFront]] =
    ActorSource.actorRef[WsMsgFront](
      completionMatcher = {
        case CompleteMsgClient =>
          log.info("disconnected from room manager.")
      },
      failureMatcher = {
        case FailMsgClient(ex) ⇒
          log.error(s"ws failed: $ex")
          ex
      },
      bufferSize = 8,
      overflowStrategy = OverflowStrategy.fail
    ).collect {
      case message: WsMsgClient =>
        //println(message)
        val sendBuffer = new MiddleBufferInJvm(409600)
        BinaryMessage.Strict(ByteString(
          message.fillMiddleBuffer(sendBuffer).result()
        ))
    }

  def getRMSink(
    hController: GeekHostController,
  )(
    implicit timer: TimerScheduler[RmCommand]
  ): Sink[Message, Future[Done]] = {
    Sink.foreach[Message] {
      case TextMessage.Strict(msg) =>
        //处理后端发过来的消息
        hController.wsMessageHandle(TextMsg(msg))

      case BinaryMessage.Strict(bMsg) =>
        val buffer = new MiddleBufferInJvm(bMsg.asByteBuffer)
        val message = bytesDecode[WsMsgRm](buffer) match {
          case Right(rst) => rst
          case Left(_) => DecodeError
        }
        hController.wsMessageHandle(message)


      case msg: BinaryMessage.Streamed =>
        val futureMsg = msg.dataStream.runFold(new ByteStringBuilder().result()) {
          case (s, str) => s.++(str)
        }
        futureMsg.map { bMsg =>
          val buffer = new MiddleBufferInJvm(bMsg.asByteBuffer)
          val message = bytesDecode[WsMsgRm](buffer) match {
            case Right(rst) => rst
            case Left(_) => DecodeError
          }
          hController.wsMessageHandle(message)
        }


      case _ => //do nothing

    }
  }
}
