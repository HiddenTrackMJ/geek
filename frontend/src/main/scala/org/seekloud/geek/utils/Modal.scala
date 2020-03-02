package org.seekloud.geek.utils

/**
  * User: Jason
  * Date: 2019/5/29
  * Time: 14:14
  */
import org.scalajs.dom

import scala.xml.Elem

class Modal(header:Elem, child:Elem, successFunc:() => Unit, id:String, withButton: Boolean = true) extends Component {


  val closeBtn: Elem = <button data-dismiss="modal" class="btn btn-default" id="closeBtn">关闭</button>
  val confirmBtn: Elem = <button data-dismissAll="modal" class="btn btn-primary" id="confirmBtn" onclick={() => successFunc()}>确认</button>

  def noButton(): Unit ={
    dom.document.getElementById("closeBtn").setAttribute("style","display:none")
    dom.document.getElementById("confirmBtn").setAttribute("style","display:none")
  }

  val modal: Elem =
    <div class="modal fade" id={id}  data-backdrop="static" tabindex="-1" role="dialog" aria-labelledby="myModalLabel">
      <div class="modal-dialog">
        <div class="modal-content">
          <div class="modal-header">
            {header}
          </div>
          <div class="modal-body">
            {child}
          </div>
          <div class="modal-footer">
            {confirmBtn}
            {closeBtn}
          </div>
        </div><!-- /.modal-content -->
      </div><!-- /.modal-dialog -->
    </div>

  val msgModal: Elem =
    <div class="modal fade" id={id}  data-backdrop="static" tabindex="-1" role="dialog" aria-labelledby="myModalLabel">
      <div class="modal-dialog">
        <div class="modal-content">
          <div class="modal-header">
            <button data-dismiss="modal" class="close" type="button"><span aria-hidden="true">×</span><span class="sr-only">Close</span></button>
            {header}
          </div>
          <div class="modal-body">
            {child}
          </div>
        </div><!-- /.modal-content -->
      </div><!-- /.modal-dialog -->
    </div>

  override def render: Elem = {
    <div>
      {
      if(withButton) modal
      else msgModal
      }
    </div>
  }
}
