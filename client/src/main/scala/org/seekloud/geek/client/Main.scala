package org.seekloud.geek.client

import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
import java.io.{BufferedReader, File, InputStreamReader, OutputStream}

import org.bytedeco.javacpp.Loader
/**
  * Author: xgy
  * Date: 2020/1/31
  * Time: 20:17
  */

object Main {


  def main(args: Array[String]): Unit = {
    //    getRoomInfo(100,"sss")

    //    val f = Future{5}
    //  f  andThen{
    //    case Failure(e) =>println("sss1"+e)
    //    case Success(e) =>println("sss2"+e)
    //  }

//    def reverseString(s: Array[Char]): Array[Char] = {
//      // var k:Char = ' '
//      for (i <- 0 until  s.length/2) {
//        val k = s(i)
//        s(i) = s(s.length -i-1)
//        s(s.length -i-1) = k
//      }
//
//      s
//
//    }

//    case class rVideo(id: Long, userid: Long, roomid: Long, timestamp: Long, filename: String, length: String, invitation: Long, comment: String)

//    val video = rVideo(0L, 0, 0, 1,  "kk.mp4", "",0,"")
//
//    val videoNew = video.copy(length = "10:00:33:65")
////    val videoNew2
//
//    println(video)
//    println(videoNew)
//
//val file = new File(s"J:\\暂存\\videos\\1076_1581762286272.mp4")
//    if(file.exists()){
//      val d = getVideoDuration(s"J:\\暂存\\videos\\1076_1581762286272.mp4")
//      println(s"duration:$d")
//    }else{
//      println(s"no record for roomId:")
//    }
testMap

  }

//  def millis2HHMMSS(sec: Double): String = {
//    val hours = (sec / 3600000).toInt
//    val h =  if (hours >= 10) hours.toString else "0" + hours
//    val minutes = ((sec % 3600000) / 60000).toInt
//    val m = if (minutes >= 10) minutes.toString else "0" + minutes
//    val seconds = ((sec % 60000) / 1000).toInt
//    val s = if (seconds >= 10) seconds.toString else "0" + seconds
//    val dec = ((sec % 1000) / 10).toInt
//    val d = if (dec >= 10) dec.toString else "0" + dec
//    s"$h:$m:$s.$d"
//  }
  case class A(id:Int)
  def testMap: Unit ={
    val a=Seq(1,2,3,4,5)
    println(a.map(t=>A(t)).toList)

  }
//  def getVideoDuration(filePath: String) ={
//    val ffprobe = Loader.load(classOf[org.bytedeco.ffmpeg.ffprobe])
//    //容器时长（container duration）
//    val pb = new ProcessBuilder(ffprobe,"-v","error","-show_entries","format=duration", "-of","csv=\"p=0\"","-i", s"$filePath")
//    val processor = pb.start()
//    val br = new BufferedReader(new InputStreamReader(processor.getInputStream))
//    val s = br.readLine()
//    var duration = 0
//    if(s!= null){
//      duration = (s.toDouble * 1000).toInt
//    }
//    br.close()
//    //    if(processor != null){
//    //      processor.destroyForcibly()
//    //    }
//    millis2HHMMSS(duration)
//  }


  class Main {


  }


}

//简单的client前端demo
/*class Main extends javafx.application.Application{
  println("jk")
     def start(primaryStage: Stage): Unit = {
      val btn = new Button("d")
      btn.setOnAction(_=>println("sasaf")
      )

      val root = new StackPane()
      root.getChildren().add(btn)
      val scene = new Scene(root, 300, 250)

      primaryStage.setTitle("hello soew")
      primaryStage.setScene(scene)
      primaryStage.show()


      val stopAction: EventHandler[ActionEvent] = (e: ActionEvent) => {
        println("sff")
      }

      println("Hello")
    }

}*/
