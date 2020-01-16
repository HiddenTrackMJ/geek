package org.seekloud.geek.client.utils

import java.io.File

import javafx.application.Platform
import javafx.scene.image.WritableImage
import org.bytedeco.javacv.OpenCVFrameConverter.ToMat
import org.bytedeco.javacv.{FFmpegFrameGrabber, FFmpegFrameRecorder, Frame, Java2DFrameConverter}
import org.bytedeco.ffmpeg.global.avcodec._

/**
  * User: Jason
  * Date: 2019/7/19
  * Time: 14:18
  */
object VideoUtil {
  def draw(): Unit = {
    val srcFile = "rtmp://localhost/oflaDemo/123456"
//    val srcFile = "H:\\V6\\773-819op\\carnie.mp4"
    val outDir = new File("H:\\V6\\test")
//    H:\V6\test

    val outFile = "rec0011.flv"
    val outTarget = new File(outDir, outFile)


    val grabber = new FFmpegFrameGrabber(srcFile)
    //    grabber.getPixelFormat
    //    grabber.getImageMode

    //    val pixelFormat0 = grabber.getPixelFormat
    //    println(s"pixelFormat0: $pixelFormat0")
    println(grabber.getFormat)
    grabber.setFormat("")
    grabber.setFrameRate(30)
    grabber.setOption("fflags", "nobuffer")
    grabber.start()

    val pixelFormat = grabber.getPixelFormat
    println(s"pixelFormat: $pixelFormat")

//    import org.bytedeco.ffmpeg.global.avcodec
//    import org.bytedeco.javacv.FFmpegFrameRecorder
//    val recorder2: FFmpegFrameRecorder = new FFmpegFrameRecorder("rtmp://localhost/oflaDemo/123123", grabber.getImageWidth, grabber.getImageHeight, 2)
//    recorder2.setInterleaved(true)
//
//    // decrease "startup" latency in FFMPEG (see:
//    // https://trac.ffmpeg.org/wiki/StreamingGuide)
//    recorder2.setVideoOption("tune", "zerolatency")
//    // tradeoff between quality and encode speed
//    // possible values are ultrafast,superfast, veryfast, faster, fast,
//    // medium, slow, slower, veryslow
//    // ultrafast offers us the least amount of compression (lower encoder
//    // CPU) at the cost of a larger stream size
//    // at the other end, veryslow provides the best compression (high
//    // encoder CPU) while lowering the stream size
//    // (see: https://trac.ffmpeg.org/wiki/Encode/H.264)
//    recorder2.setVideoOption("preset", "ultrafast")
//    // Constant Rate Factor (see: https://trac.ffmpeg.org/wiki/Encode/H.264)
//    recorder2.setVideoOption("crf", "28")
//    // 2000 kb/s, reasonable "sane" area for 720
//    recorder2.setVideoBitrate(2000000)
//    recorder2.setVideoCodec(avcodec.AV_CODEC_ID_H264)
//    recorder2.setFormat("flv")
//    // FPS (frames per second)
//    recorder2.setFrameRate(grabber.getFrameRate)
//    // Key frame interval, in our case every 2 seconds -> 30 (fps) * 2 = 60
//    // (gop length)
////    recorder2.setGopSize(GOP_LENGTH_IN_FRAMES)
//
//    // We don't want variable bitrate audio
//    recorder2.setAudioOption("crf", "0")
//    // Highest quality
//    recorder2.setAudioQuality(0)
//    // 192 Kbps
//    recorder2.setAudioBitrate(192000)
//    recorder2.setSampleRate(44100)
//    recorder2.setAudioChannels(2)
//    recorder2.setAudioCodec(avcodec.AV_CODEC_ID_AAC)
//
//    recorder2.start()

        val recorder =
      new FFmpegFrameRecorder(
//        "http://localhost/hls/mystream.m3u8",
        outTarget,
        grabber.getImageWidth / 2,
        grabber.getImageHeight / 2,
        grabber.getAudioChannels
      )

//    recorder.setFormat("m3u8")


    //recorder.setInterleaved(true)
    //recorder.setPixelFormat(pixelFormat)
    recorder.setFrameRate(grabber.getFrameRate)

    recorder.setVideoOption("crf", "25")
    // 2000 kb/s, 720P视频的合理比特率范围
    recorder.setVideoBitrate(2000000)
    //recorder.setVideoBitrate(grabber.getVideoBitrate)
    //println(s"getVideoBitrate: ${grabber.getVideoBitrate}")
    // h264编/解码器
    //recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264)
    recorder.setVideoCodec(grabber.getVideoCodec)
    println(s"getVideoCodec: ${grabber.getVideoCodec}")


    recorder.start()

    var frame = grabber.grab()



    import javafx.embed.swing.SwingFXUtils
    import java.awt.image.BufferedImage

    var lastTs = frame.timestamp.toDouble / 1000
    var currentTime = System.nanoTime()
    var frameCount = 0

    val beginTime = System.currentTimeMillis()
    val writableImage = new WritableImage(320, 240)

    while (frame != null) {
      frameCount += 1

      if (frame.image != null) {
        val image =
//          frame.image
          processImage(frame).image
//        val bufferedImage: BufferedImage= converter.convert(frame)
//        SwingFXUtils.toFXImage(bufferedImage, writableImage)
//        ClientBoot.homeScene.drawImage(writableImage)
//        println(recorder.getFormat)
//        recorder.recordImage(
//          frame.imageWidth,
//          frame.imageHeight,
//          frame.imageDepth,
//          frame.imageChannels,
//          frame.imageStride,
//          pixelFormat,
//          image: _*)
      }

      if (frame.samples != null) {
//        recorder.recordSamples(frame.sampleRate, frame.audioChannels, frame.samples: _*)
      }
      //      recorder.setTimestamp(frame.timestamp)


      //recorder.record(frame)

      //      if (frame.image != null) {
      //        recorder.recordImage(
      //          frame.imageWidth,
      //          frame.imageHeight,
      //          frame.imageDepth,
      //          frame.imageChannels,
      //          frame.imageStride,
      //          pixelFormat,
      //          frame.image: _*)
      //      }

      //recorder.recordSamples(frame.samples: _*)
      Thread.sleep(16)
      frame = grabber.grab()
      if (frame != null && frame.image != null) {
        val a = System.currentTimeMillis()
        val bufferedImage: BufferedImage= converter.convert(frame)
        SwingFXUtils.toFXImage(bufferedImage, writableImage)
        Platform.runLater(() => {
//          Boot.homeScene.drawImage(writableImage)
        })

        val b = System.currentTimeMillis()
        println(s"delta: ${b-a}")
        val ts = frame.timestamp.toDouble / 1000
        val ct = System.nanoTime()
        val d1 = ts - lastTs
        val d2 = (ct - currentTime) / 1000000
        println(s"w: ${frame.imageWidth}, h: ${frame.imageHeight}")
        println(s"fc=$frameCount, d1= $d1 ms, d2= $d2 ms ")
        lastTs = ts
        currentTime = ct
      }
    }

    grabber.close()
    recorder.close()
//    recorder2.close()


    println(s"DONE, total time: ${System.currentTimeMillis() - beginTime}")
  }

  val converter = new Java2DFrameConverter()
  val matConverter = new ToMat()

  def processImage(frame: Frame): Frame = {
    val image = matConverter.convert(frame)
    //    Drawer.drawEllipse(image, 0)
    //    Drawer.putText(image, "Hello, world!!!")
    val img =
          image
//    Drawer.combineImg(image, image)
    val out = matConverter.convert(img)
    out
  }

  def process(frame: Frame)= {
    val image = matConverter.convert(frame)
    //    Drawer.drawEllipse(image, 0)
    //    Drawer.putText(image, "Hello, world!!!")
    image
  }
}
