package org.seekloud.geek.utils

import org.scalajs.dom

import scala.collection.immutable
import scala.language.implicitConversions
import scala.xml.Elem

/**
  * User: Taoz
  * Date: 3/29/2018
  * Time: 1:59 PM
  */
trait Component {

  def render: Elem



}

object Component {
  implicit def component2Element(comp: Component): Elem = comp.render

  val loadingDiv: Elem =
    <div>
      <img src="/esheep/static/img/loading.gif" style="width: 100px"></img>
      <h3>加载中</h3>
    </div>

  val noDataDiv: Elem =
    <div>
      <img src="/esheep/static/img/warn.png"></img>
      <h3>无数据</h3>
    </div>

}
