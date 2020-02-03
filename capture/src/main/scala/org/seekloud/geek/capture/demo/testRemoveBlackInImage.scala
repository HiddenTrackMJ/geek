package org.seekloud.geek.capture.demo

import javafx.application.Application
import javafx.stage.Stage

/**
  * 去除图片黑边的测试demo
  */
class testRemoveBlackInImage extends Application {

  override def start(primaryStage: Stage): Unit = {

    //hx
    /*
    val ut = new JavaUtils();
    val image = new Image("file:/Users/haoxue/Downloads/blackEdge.png")
    val pr = image.getPixelReader
    val iw = image.getWidth.toInt
    val ih = image.getHeight.toInt
    val writableImage = new WritableImage(pr,iw,ih)
    val result = ut.removeBlackEdge(writableImage)
    val root = new BorderPane()
    val imageView: ImageView = new ImageView();
    imageView.setImage(result)
    root.setCenter(imageView)
    val scene: Scene = new Scene(root, 400, 400);
    primaryStage.setScene(scene);
*/
    println(System.getProperty("os.name"))

    var video = false
    val str = """[AVFoundation input device @ 0x7faa98622780] [0] FaceTime HD Camera
                [AVFoundation input device @ 0x7faa98622780] [1] Capture screen 0
                [AVFoundation input device @ 0x7faa98622780] AVFoundation audio devices:
                [AVFoundation input device @ 0x7faa98622780] [0] Soundflower (2ch)
                [AVFoundation input device @ 0x7faa98622780] [1] MacBook Pro 麦克风
                [AVFoundation input device @ 0x7faa98622780] [2] Soundflower (64ch)"""

    val reg = """(\[\d+]) (.*)""".r
    reg.findAllIn(str).foreach{
      case reg(a, b) =>
        if(a=="[0]"){
          video = !video
        }
        if(video) println(b+" "+"video"+a.drop(1).dropRight(1) )
        else println(b+" "+"audio"+a.tail.head )
    }



//    println(reg.findAllMatchIn(str).size)
  //  println(reg.findFirstMatchIn("[0] FaceTime HD Camera").get.group(2))
//    println("""\[dshow @]""")
    //hw
   /* //源图片Mat
    val imgsrc :Mat = imread("/Users/hewro/Downloads/Jietu20191015-144557@2x.jpg")
    val  startTime = System.currentTimeMillis();
    val result2 = new JavaUtils().removeBlackEdge(imgsrc)
    val bufferedImage = Java2DFrameUtils.toBufferedImage(result2)
    val image2 = SwingFXUtils.toFXImage(bufferedImage, null)
    val endTime2=System.currentTimeMillis();
    System.err.println("运行时间"+(endTime2-startTime))

    val root2 = new BorderPane();
    val imageView2: ImageView = new ImageView();
    imageView2.setImage(image2);
    root2.setCenter(imageView2);
    val scene2: Scene = new Scene(root2, 400, 400);
    primaryStage.setScene(scene2);
*/

    //primaryStage.show()
  }

}