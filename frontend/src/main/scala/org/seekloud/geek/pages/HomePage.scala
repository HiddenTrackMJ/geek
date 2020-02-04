package org.seekloud.geek.pages

import mhtml._
import org.scalajs.dom
import org.seekloud.geek.Main
import org.seekloud.geek.utils.Page

import scala.xml.Elem

object HomePage extends Page {
  override val locationHashString: String = "#/home"
  val modalDiv = Var(emptyHTML)

  val homeWrapper: Elem =
    <header id="home">

      <div class="home-wrapper">
        <div class="container">
          <div class="row" style="margin:0 auto">

            <div class="col-md-10 col-md-offset-1">
              <div class="home-content">
                <h1 class="white-text">We Create Miracles</h1>
                <p class="white-text">La Vita Nuova.
                </p>
                <button class="white-btn" style="width:145px" onclick={() => gotoPage("inviterManage")}>Invite</button>
                <button class="main-btn" style="width:145px" onclick={() =>
                  Main.getRoomList()
                  dom.window.setTimeout(() => Header.gotoLive(), 500)
                  ()
                }>Watch</button>
              </div>
            </div>

          </div>
        </div>
      </div>

    </header>



  def gotoPage(path: String): Unit = {
    dom.window.location.hash = s"#/$path"
  }

  def init(): Unit = {
    val userId = dom.window.localStorage.getItem("userId")
    val userName = dom.window.localStorage.getItem("username")
    if (userId != null && userName != null) {
      Header.userId := userId
      Header.username := userName
    } else {
      gotoPage("login")
    }

    dom.document.body.style = "background-image: url('/geek/static/img/bg1.png');" +
      "background-attachment: fixed;" +
      "background-size: cover;" +
      "background-position: center;" +
      "background-repeat: no-repeat;"
  }

  override def render: Elem = {
    println("Render")
    init()
    //    dom.document.documentElement.appendChild({renderWebm()})

    <div style="margin:0 0;">
      {homeWrapper}
    </div>

  }

}

//<ul class="main-nav nav navbar-nav navbar-right">
//<li class="active"><a href="#home">Home</a></li>
//<li><a href="#about">About</a></li>
//<li><a href="#portfolio">Portfolio</a></li>
//<li><a href="#service">Services</a></li>
//<li><a href="#pricing">Prices</a></li>
//<li><a href="#team">Team</a></li>
//<li class="has-dropdown"><a href="#blog">Blog</a>
//<ul class="dropdown">
//<li><a href="blog-single.html">blog post</a></li>
//</ul>
//</li>
//<li><a href="#contact">Contact</a></li>
//</ul>