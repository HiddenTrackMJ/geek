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
import org.seekloud.geek.shared.ptcl.RoomProtocol.{GetRoomIdListRsp, GetRoomSectionListReq, GetRoomSectionListRsp, RoomId, RoomInfoSection}
import org.seekloud.geek.shared.ptcl.SuccessRsp
import org.slf4j.LoggerFactory
import org.seekloud.geek.utils.TimeUtil.timeStamp2yyyyMMdd

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

  final case class GetRoomIdList(req: GetRoomSectionListReq,replyTo: ActorRef[GetRoomIdListRsp]) extends Command

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
          case GetRoomSectionList(room, replyTo)=>
            //问题是有录像邀请你，有录像邀请别人，获取全部录像时你可能看到多个实际已邀请自己但邀请别人未邀请自己的录像
            //如果邀请人和被邀请人一致，保持获取；邀请人和被邀请人不一致，保持获取；则会造成冗余，同时邀请人有邀请人和被邀请人录像
            //如果邀请人和被邀请人一致，不获取；邀请人和被邀请人不一致，保持获取；则会造成缺失，邀请人不邀请别人时看不到自己的录像
            //解决方法：全部录像只获取一遍，再判断是否被邀请(不判断)
            VideoDao.getSecVideo(room.inviteeId).map{
              rsp=>
                if (rsp isEmpty){
                  replyTo ! GetRoomSectionListRsp(List(),-1,"查询为空")
                }else{

                  replyTo ! GetRoomSectionListRsp(rsp.map{e=> RoomInfoSection(e._2.roomid,e._2.userid,e._1.name,e._2.filename,timeStamp2yyyyMMdd(e._2.timestamp),true)
                  }.toSet.toList,0,"有该user的录像")
                }
            }
            Behaviors.same

          case GetRoomIdList(room, replyTo)=>
            VideoDao.getSecVideo2(room.inviteeId).map{
              rsp=>
                if (rsp isEmpty){
                  replyTo ! GetRoomIdListRsp(List(),-1,"查询为空")
                }else{
                  replyTo ! GetRoomIdListRsp(rsp.map(e=>RoomId(e.roomid)).toSet.toList,0,"有该user的录像")
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
