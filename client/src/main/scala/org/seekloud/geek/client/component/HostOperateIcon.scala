package org.seekloud.geek.client.component

import akka.actor.typed.ActorRef
import com.jfoenix.controls.JFXRippler
import javafx.scene.layout.Pane
import org.seekloud.geek.client.common.Constants.HostOperateIconType
import org.seekloud.geek.client.core.RmManager
import org.seekloud.geek.client.core.RmManager.Shield
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
            RmManager.getUserInfo(userInfo.userId).get.isAllow = Some(!userInfo.isAllow.get)
            //todo ws消息

          case HostOperateIconType.HOST =>
            val origHost = RmManager.roomInfo.get.userList.find(_.isHost.get == true)
            if (origHost nonEmpty){
              origHost.get.isHost = Some(false)
              userInfo.isHost = Some(true)
            }else{
              log.info("当前数据有误，成员列表中没有房主")
            }
            rmManager ! RmManager.ChangePossession(ChangePossessionReq(RmManager.roomInfo.get.roomId,userInfo.userId))

        }

        //当前自己的用户
        if (updateMyUIIfNeedI && userInfo.userId == RmManager.userInfo.get.userId){
          updateMyUI()
        }else{
          updateMyUI()
        }


        //修改list界面
        updateUI()

      })
    }
    icon

  }

}
