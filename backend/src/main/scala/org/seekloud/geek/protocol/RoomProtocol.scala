package org.seekloud.geek.protocol

import akka.actor.typed.ActorRef
import org.seekloud.geek.core.{RoomDealer, RoomManager, UserActor}
import org.seekloud.geek.shared.ptcl.CommonProtocol.RoomInfo
import org.seekloud.geek.shared.ptcl.WsProtocol.WsMsgClient

/**
 * Author: Jason
 * Date: 2020/2/8
 * Time: 17:23
 */
object RoomProtocol {

  trait RoomCommand extends RoomManager.Command with RoomDealer.Command

  case class WebSocketMsgWithActor(userId:Long,roomId:Long,msg:WsMsgClient) extends RoomCommand

  case class UpdateSubscriber(join:Int,roomId:Long,userId:Long,userActorOpt:Option[ActorRef[UserActor.Command]]) extends RoomCommand

  case class StartRoom4Anchor(userId:Long,roomId:Long,actor:ActorRef[UserActor.Command]) extends RoomCommand

  case class UserLeftRoom(userId:Long,roomId:Long) extends RoomCommand

  final case class StartLiveAgain(roomId:Long) extends RoomCommand

  case class HostCloseRoom(roomId:Long) extends RoomCommand// 主播关闭房间

  case class AddUserActor4Test(userId:Long,roomId:Long,userActor: ActorRef[UserActor.Command])extends RoomCommand


  case class BanOnAnchor(roomId:Long) extends RoomCommand

}
