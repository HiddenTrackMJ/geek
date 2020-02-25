package org.seekloud.geek.client.component

import com.jfoenix.controls.{JFXAlert, JFXButton, JFXDialogLayout, JFXTextField}
import javafx.event.ActionEvent
import javafx.scene.control.Label
import javafx.scene.layout.VBox
import javafx.stage.{Modality, Stage}
import org.slf4j.LoggerFactory

/**
  * User: hewro
  * Date: 2020/2/16
  * Time: 20:10
  * Description: 输入弹框
  */

// todo: 抽象成组件
case class InputDialog(
  title:String = "",
  desc:String = "",
  text:String = "",//输入框的默认值
  data:List[InputField] =Nil,
  stage:Stage
){
  private val log = LoggerFactory.getLogger(this.getClass)
  private var alert:JFXAlert[String] = _

  def build()= {
    try {
      val usernameTextField = new JFXTextField
      //设置默认值
      usernameTextField.setText(text)
      val alert = new JFXAlert[String](stage)
      alert.initModality(Modality.APPLICATION_MODAL)
      //点击外部不会取消
      alert.setOverlayClose(false)

      val layout = new JFXDialogLayout
      layout.setHeading(new Label(title))
      layout.setBody(new VBox(new Label(desc), usernameTextField))

      val addButton = new JFXButton("确定")
      addButton.setDefaultButton(true)
      addButton.setOnAction((addEvent: ActionEvent) => {
        def foo(addEvent: ActionEvent) = {
          alert.setResult(usernameTextField.getText)
          alert.hideWithAnimation()
        }
        foo(addEvent)
      })

      val cancelButton = new JFXButton("取消")
      cancelButton.setCancelButton(true)
      cancelButton.setOnAction((_: ActionEvent) => alert.hideWithAnimation())

      layout.setActions(addButton, cancelButton)
      layout.setPrefWidth(stage.getWidth * 0.8)
      alert.setContent(layout)

      val result = alert.showAndWait
      var info:Option[String] = None
      result.ifPresent{
        a=>
          info = Some(a)
      }
      info
    }catch {
      case e: Throwable =>
        log.info(s"$e")
        None
    }
  }
}


case class InputField(
  name:String,//label
  desc:String //说明文字
)