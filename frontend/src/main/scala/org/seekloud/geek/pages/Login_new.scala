package org.seekloud.geek.pages

import org.seekloud.geek.utils.Page

import scala.xml.Elem

object Login_new extends Page {

  override def render: Elem = {
    <div>
      <div class="tyg-div">
        <ul>
          <li>让</li>
          <li><div style="margin-left:20px;">数</div></li>
          <li><div style="margin-left:40px;">据</div></li>
          <li><div style="margin-left:60px;">改</div></li>
          <li><div style="margin-left:80px;">变</div></li>
          <li><div style="margin-left:100px;">生</div></li>
          <li><div style="margin-left:120px;">活</div></li>
        </ul>
      </div>
      <div id="contPar" class="contPar">
        <div id="page1"  style="z-index:1;">
          <div class="title0">行业智慧能力平台</div>
          <div class="title1">旅游、交通、气象、公共安全、大数据</div>
          <div class="imgGroug">
            <ul>
              <img alt="1" class="img0 png" src="./img/page1_0.png"/>
                <img alt="2" class="img1 png" src="./img/page1_1.png"/>
                  <img alt="3" class="img2 png" src="./img/page1_2.png"/>
                  </ul>
                </div>
                <img alt="4" class="img3 png" src="./img/page1_3.jpg" />
                </div>
              </div>
              <div class="tyg-div-denglv">
                <div class="tyg-div-form">
                  <form action="5">
                    <h2>登录</h2><p class="tyg-p">欢迎访问  智慧能力</p>
                    <div style="margin:5px 0px;">
                      <input type="text" placeholder="请输入账号..."/>
                    </div>
                    <div style="margin:5px 0px;">
                      <input type="text" placeholder="请输入密码..."/>
                    </div>
                    <div style="margin:5px 0px;">
                      <input type="text" style="width:150px;" placeholder="请输入验证码..."/>
                      <img src="./img/1.png" style="vertical-align:bottom;" alt="验证码"/>
                    </div>
                    <button type="submit" >登<span style="width:20px;"></span>录</button>
                  </form>
                </div>
              </div>

    </div>
  }
}
