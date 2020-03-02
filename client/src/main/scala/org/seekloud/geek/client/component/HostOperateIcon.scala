package org.seekloud.geek.client.component

import akka.actor.typed.ActorRef
import com.jfoenix.controls.JFXRippler
import javafx.scene.layout.Pane
import org.seekloud.geek.client.common.Constants.HostOperateIconType
import org.seekloud.geek.client.core.RmManager
import org.seekloud.geek.client.core.RmManager.{Appoint4Host, Shield}
import org.seekloud.geek.shared.ptcl.CommonProtocol.UserInfo
import org.seekloud.geek.shared.ptcl.WsProtocol.{ChangePossessionReq, ShieldReq}
import org.slf4j.LoggerFactory
/**
  * User: hewro
  * Date: 2020/2/25
  * Time: 17:41
  * Description: 主持人的可以对某个用户操作的图标，提取成组件
  */
case class HostOperateIcon(
  yesIcon:String,
  notIcon:String,
  yesText:String="",//执行操作是状态变为yes的显示消息
  notText:String="",
  yseFlag:Boolean,// true
  userInfo: UserInfo,
  rootPane:Pane,
  updateMyUI:() =>Unit, //更新与自己相关的界面，当前仅当点击的userinfo.id和自己的id匹配时候执行
  updateUI:()=>Unit, //更新整个用户列表界面
  sType:Int,
  updateMyUIIfNeedI :Boolean = true, //执行updateMyUI这个函数，是否需要判断点击的用户是当前登录的用户
  rmManager: ActorRef[RmManager.RmCommand]
){

  private val log = LoggerFactory.getLogger(this.getClass)

  def apply(): JFXRippler = {
    val r =  if(yseFlag) RippleIcon(List(yesIcon))()else RippleIcon(List(notIcon))()
    val icon = r._1


    if (RmManager.getCurrentUserInfo().isHost.get){//只有房主可以进行操作
      icon.setOnMouseClicked(_ =>{
//        Popup(icon).show()
        if (RmManager.isStart){
          if (yseFlag){
            SnackBar.show(rootPane,notText + userInfo.userName)
          }else{
            SnackBar.show(rootPane,yesText + userInfo.userName)
          }

          //修改内存中该用户的状态
          sType match {
            case HostOperateIconType.MIC =>
              //            userInfo.isMic = Some(!userInfo.isMic.get)
              log.info("roomid" + RmManager.roomInfo.get.roomId)
              rmManager ! Shield(ShieldReq(isForced = true,RmManager.roomInfo.get.roomId,userInfo.userId,isImage = userInfo.isVideo.get,isAudio = !userInfo.isMic.get))

            case HostOperateIconType.VIDEO =>
              //修改内存中该用户的静音状态
              rmManager ! Shield(ShieldReq(isForced = true,RmManager.roomInfo.get.roomId,userInfo.userId,isImage = !userInfo.isVideo.get,isAudio = userInfo.isMic.get))
            //            userInfo.isVideo = Some(!userInfo.isVideo.get)


            case HostOperateIconType.ALLOW =>
              //            println("当前用户" + userInfo.isAllow.get)

              val originAllow = RmManager.roomInfo.get.userList.find(_.isAllow.get == true)
              if (originAllow nonEmpty){
                if(originAllow.get.userId == userInfo.userId) {//之前的发言人和点击的用户是一个人，即取消当前用户的发言人身份
                  log.info("之前的发言人和点击的用户是一个人，即取消当前用户的发言人身份")
                  userInfo.isAllow = Some(false)
                  originAllow.get.isAllow = Some(false)
                  rmManager ! Appoint4Host(userInfo.userId,status = false)
                }else{//更换发言人的身份的用户
                  log.info("更换发言人的身份的用户")
                  originAllow.get.isAllow = Some(false)//之前的发言人变成false
                  userInfo.isAllow = Some(true)//现在的发言人变成true
                  rmManager ! Appoint4Host(originAllow.get.userId,status = false)
                  rmManager ! Appoint4Host(userInfo.userId,status = true)
                }
              }else{//之前没有发言人，把当前点击的人设为发言人
                log.info("之前没有发言人，把当前点击的人设为发言人")
                userInfo.isAllow = Some(true)
                log.info("RmManager.roomInfo.get.userList:" + RmManager.roomInfo.get.userList)
                rmManager ! Appoint4Host(userInfo.userId,status = true)
              }


            case HostOperateIconType.HOST =>
              val origHost = RmManager.roomInfo.get.userList.find(_.isHost.get == true)
              if (origHost nonEmpty){
                origHost.get.isHost = Some(false)
                userInfo.isHost = Some(true)
              }else{
                log.info("当前数据有误，成员列表中没有房主")
              }
              rmManager ! RmManager.ChangePossession(ChangePossessionReq(RmManager.roomInfo.get.roomId,userInfo.userId))

            case HostOperateIconType.EXIT =>
              rmManager ! RmManager.KickOff(userInfo.userId)
          }

          //当前自己的用户
          if (updateMyUIIfNeedI && userInfo.userId == RmManager.userInfo.get.userId){
            updateMyUI()
          }else{
            updateMyUI()
          }

          //修改list界面
          updateUI()
        }else{
          SnackBar.show(rootPane,"您没有开启会议，无法操作用户信息")
        }
      })

    }
    icon

  }

}
