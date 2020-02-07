package org.seekloud.geek.core

import akka.actor.Scheduler
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.server.Directives.complete
import akka.util.Timeout
import org.seekloud.geek.models.dao.VideoDao
import org.seekloud.geek.models.dao.UserDao
import org.seekloud.geek.Boot.executor
import org.seekloud.geek.core.RoomManager.{Command, idle}
import org.seekloud.geek.shared.ptcl.CommonProtocol._
import org.seekloud.geek.shared.ptcl.RoomProtocol.{GetRoomSectionListReq, GetRoomSectionListRsp, RoomId}
import org.seekloud.geek.shared.ptcl.SuccessRsp
import org.slf4j.LoggerFactory

import scala.language.postfixOps

/**
  * User: xgy
  * Date: 2020/2/6
  * Time: 23:06
  * Description: 处理[[org.seekloud.geek.http.InvitationService]]的消息
  */
object Invitation {


  sealed trait Command
  private val log = LoggerFactory.getLogger(this.getClass)

  final case class GetRoomSectionList(req: GetRoomSectionListReq,replyTo: ActorRef[GetRoomSectionListRsp]) extends Command

  final case class GetInviterList(req: InvitationReq,replyTo: ActorRef[InvitationRsp]) extends Command

  final case class GetInviteeList(req: InvitationReq,replyTo: ActorRef[InvitationRsp]) extends Command

  final case class DelInvitee(req: InviterAndInviteeReq,replyTo: ActorRef[SuccessRsp]) extends Command

  def create()(implicit timeout: Timeout, scheduler: Scheduler) =
    Behaviors.setup[Command] {
      _ =>
        log.info("invitation started.")
        Behaviors.withTimers[Command] { implicit timer =>
          idle()
        }
    }

  private def idle():Behavior[Command] =
    Behaviors.receive[Command] {
      (ctx, msg) =>
        msg match {
          case GetRoomSectionList(user, replyTo)=>
            VideoDao.getSecVideo(user.userId).map{
              rsp=>
                if (rsp isEmpty){
                  replyTo ! GetRoomSectionListRsp(List(),-1,"查询为空")
                }else{
                  replyTo ! GetRoomSectionListRsp(rsp.map(e=>RoomId(e.roomid)).toSet.toList,0,"有该user的录像")
                }
            }
            Behaviors.same

          case GetInviterList(user, replyTo)=>
            VideoDao.getInviter(user.inviterId).map{
              rsp=>
                if (rsp isEmpty){
                  replyTo ! InvitationRsp(None,-1,"查询为空")
                }else{
                  replyTo ! InvitationRsp(Some(rsp.map(e=>Inviter(e._1.name,e._1.id)).toSet.toList),0,"有该user的录像")
                }
            }
            Behaviors.same

          case GetInviteeList(user, replyTo)=>
            VideoDao.getInvitee(user.inviterId).map{
              rsp=>
                if (rsp isEmpty){
                  replyTo ! InvitationRsp(None,-1,"查询为空")
                }else{
                  replyTo ! InvitationRsp(Some(rsp.map(e=>Inviter(e._1.name,e._1.id)).toSet.toList),0,"有该user的录像")
                }
            }
            Behaviors.same
          case DelInvitee(user, replyTo)=>
            VideoDao.delInvitee(user.inviterId,user.inviteeId).map{
              rsp=>
              var msg = ""
              if (rsp == -1){
                msg = "删除失败"
              }else{
                msg = "删除成功！"
              }
              replyTo ! SuccessRsp(rsp)
            }
            Behaviors.same
          case _=>
            log.info("收到未知消息create")
            Behaviors.unhandled
        }
    }


}
