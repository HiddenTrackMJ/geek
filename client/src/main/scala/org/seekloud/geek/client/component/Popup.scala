package org.seekloud.geek.client.component

import com.jfoenix.controls.{JFXListView, JFXPopup}
import javafx.fxml.FXMLLoader
import javafx.scene.control.Label
import javafx.scene.layout.Pane
import org.seekloud.geek.shared.ptcl.CommonProtocol.UserInfo
import org.slf4j.LoggerFactory

/**
  * User: hewro
  * Date: 2020/3/1
  * Time: 23:27
  * Description: 弹出菜单
  */
case class Popup(
  root:Pane,
  userInfo: UserInfo
){

  var pop:JFXPopup = _
  def apply(): Unit = {
    pop = new JFXPopup(FXMLLoader.load(getClass.getResource("/scene/popup.fxml")))
  }

  def show() = {
    //静音/关闭视频/踢出房间
    val jList = new JFXListView[Label]
    if (userInfo.isVideo.get){
      new Label("关闭视频")
    }


    pop = new JFXPopup(jList)

    pop.show(root)
  }
}

import javafx.fxml.FXML

final class InputController {

  private val log = LoggerFactory.getLogger(this.getClass)


  @FXML private var toolbarPopupList:JFXListView[Label] = _

  // close application
  @FXML private def submit(): Unit = {
    if (toolbarPopupList.getSelectionModel.getSelectedIndex == 1){
      log.info("click !!")
    }
  }
}

