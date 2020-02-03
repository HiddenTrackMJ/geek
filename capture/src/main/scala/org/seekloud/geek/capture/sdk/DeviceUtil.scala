package org.seekloud.geek.capture.sdk

import java.io.{BufferedReader, InputStreamReader}
import java.nio.charset.Charset
import java.util

import org.bytedeco.javacpp.Loader
import org.seekloud.geek.capture.sdk.MediaCapture.executor
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.concurrent.Future


/**
  * User: TangYaruo
  * Date: 2019/8/28
  * Time: 13:42
  *
  * Edited by: Jason
  * Time: 2019/9/6
  */
object DeviceUtil {

  private  val operationSystem = System.getProperty("os.name")

  private val log = LoggerFactory.getLogger(this.getClass)

  val ffmpeg = Loader.load(classOf[org.bytedeco.ffmpeg.ffmpeg])

  private val cmd4inputDevices = operationSystem match {

    case "Mac OS X"  => s"""$ffmpeg -f avfoundation -list_devices true -i """""
    case _ =>  s"$ffmpeg -list_devices true -f dshow -i dummy"
  }

  private val deviceOptionMap: mutable.HashMap[Device, List[DeviceOption]] = mutable.HashMap.empty

  sealed trait DeviceOption

  case class VideoOption(`type`: String, typeName: String, s_min: String, fps_min: Int, s_max: String, fps_max: Int) extends DeviceOption

  case class AudioOption(ch_min: Int, bits_min: Int, rate_min: Int, ch_max: Int, bits_max: Int, rate_max: Int) extends DeviceOption

  case class Device(dType: String, dName: String)


  private def exeCmd(commandStr: String) : Future[Either[scala.Exception, String]]= {
    println("执行的命令" + commandStr)
    var br: BufferedReader = null
    var br4Error: BufferedReader = null
    val sb: StringBuilder = new StringBuilder()
    try {
      //分割command String 并执行命令
//            val p: Process = Runtime.getRuntime.exec(commandStr)
      val cmd: util.ArrayList[String] = new util.ArrayList[String]()
      commandStr.split(" ").foreach(cmd.add)
      val pb: ProcessBuilder = new ProcessBuilder(cmd)
      //      pb.inheritIO().start().waitFor()
      val p = pb.start()
      //获得执行命令后的返回值
      br = new BufferedReader(new InputStreamReader(p.getInputStream, Charset.forName("UTF-8")))
      br4Error = new BufferedReader(new InputStreamReader(p.getErrorStream, Charset.forName("UTF-8")))
      var line: String = null
      val duFuture = Future(br.read()).map {
        rst =>
          //如果bufferedReader没有读到末尾 就读一行 并将读到的一行数据写到StringBuilder中 用换行隔开
          if (rst != -1) {
            line = br.readLine()
            while (line != null) {
              sb.append(line + "\n")
              line = br.readLine()
            }
            br.close()
            Future(Right(sb.toString()))
          } else {
            log.warn(s"normal stream get null, try error stream.")
            br.close()
            Future(br4Error.read()).map {
              rst =>
                if (rst != -1) {
                  line = br4Error.readLine()
                  while (line != null) {
                    sb.append(line + "\n")
                    line = br4Error.readLine()
                  }
                } else {
                  log.warn(s"error info get null.")
                }
                br4Error.close()
                Right(sb.toString())

            }.recover {
              case ex: Exception =>
                log.warn(s"errorStream read error: $ex")
                br4Error.close()
                Left(ex)
            }

          }
      }.recover {
        case ex: Exception =>
          log.warn(s"inputStream read error: $ex, try errorStream")
          br.close()
          Future(br4Error.read()).map {
            rst =>
              if (rst != -1) {
                line = br4Error.readLine()
                while (line != null) {
                  sb.append(line + "\n")
                  line = br4Error.readLine()
                }
              } else {
                log.warn(s"error info get null.")
              }
              br4Error.close()
              Right(sb.toString())

          }.recover {
            case ex: Exception =>
              log.warn(s"errorStream read error: $ex")
              br4Error.close()
              Left(ex)
          }
      }
      duFuture.flatMap(f => f)
    } catch {
      case e: Exception =>
        e.printStackTrace()
        Future(Left(e))
    }
    finally {
      if (br != null) {
        try {
          br.close()
        } catch {
          case e: Exception =>
            e.printStackTrace()
        }
      }
    }
  }

  //  @scala.annotation.tailrec
  /**
    * 返回所有设备 (设备别名，设备类型+序号)
    * 如 (我的AirPods, audio0)
    * @return
    */
  //  @scala.annotation.tailrec
  private def getAllDevices = {
    exeCmd(cmd4inputDevices).map {
      case Right(output) =>

        val reg = operationSystem match {
          case "Mac OS X" => """(\[\d+]) (.*)""".r
          case _ => """\[dshow @(.*?)](.*?)\n""".r
        }
        var video = false
        val rst = reg.findAllIn(output)
        var deviceName = ""
        var deviceMap: Map[String, String] = Map.empty
        rst.foreach {
          case reg(index,deviceName) if operationSystem == "Mac OS X" =>
            if(index=="[0]"){
              video = !video
            }
            val indexNum = index.drop(1).dropRight(1)
            if(video) deviceMap += (deviceName -> ("video"+indexNum))
            else deviceMap += (deviceName -> ("audio"+indexNum))

          case reg(_, b) =>
            //              println(b)
            var a = b
            while (a.startsWith(" ")) a = a.drop(1)
            if (a.startsWith("Alternative")) a = a.drop("Alternative name ".length)
            if (a.contains("devices")) {
              deviceName = a
            }
            else {
              deviceMap += (a -> deviceName)
            }
          case x@_ =>
            log.info(s"match error, $x")
        }
        //        println(deviceMap)
        deviceMap

      case Left(e) =>
        log.info(s"get all devices failed! error: ${e.getMessage}")
        Map.empty
    }
  }

  /**
    *
    * @param dName 设备名称
    * @param dType 设备类型+序号 如audio1 video1
    * @return
    */
  private def getDeviceOption(dName: String, dType: String)= {
    val `type` = if (dType.contains("video")) "video" else "audio"

    val useADevice = operationSystem match {
      case "Mac OS X" =>
        val index = dType.substring(5)
        `type` match {
        case "video" =>
          s"""$ffmpeg -f avfoundation -video_device_index $index -i """""
        case "audio" =>
          s"""$ffmpeg -f avfoundation -audio_device_index $index -i """""
      }
      case _ => s"""$ffmpeg -f dshow -list_options true -i ${`type`}=$dName"""
    }

    exeCmd(useADevice).map {
      case Right(output) =>
        //根据命令的输出结果进行获取设备的选项信息
        val device = Device(`type`, dName)
        operationSystem match {
          case "Mac OS X" =>
            val optionList = getDeviceOptionInMac(output,device,`type`)
            log.info("设备名"+device.dName+optionList.toString())
            deviceOptionMap.put(device, optionList)
            optionList
          case _ =>
            val optionList = getDeviceOptionInWindows(output,device)
            deviceOptionMap.put(device, optionList)
            optionList
        }
      case Left(e) =>
        log.info(s"get device option failed! error: ${e.getMessage}")
        List.empty
    }
  }


  def getDeviceOptionInWindows(output:String,device:Device) ={
//    println("work in windows")

    val reg = """\[dshow @(.*?)](.*?)\n""".r

    val pixelReg = """pixel_format=(.*?) {2}min s=(.*?) fps=([.0-9]+) max s=(.*?) fps=([.0-9]+)""".r
    val vcodecReg = """vcodec=(.*?) {2}min s=(.*?) fps=([.0-9]+) max s=(.*?) fps=([.0-9]+)""".r
    val audioReg = """min ch=([.0-9]+) bits=([.0-9]+) rate= ([.0-9]+) max ch=([.0-9]+) bits=([.0-9]+) rate= ([.0-9]+)""".r
    val rst = reg.findAllIn(output)//匹配的每一行列表
    var optionList: List[DeviceOption] = List.empty
    rst.foreach {
      case reg(_, b) =>
        var a = b
        //去掉前面所有空格
        while (a.startsWith(" ")) a = a.drop(1)
        a match {
          case pixelReg(format, s_min, fps_min, s_max, fps_max) =>
            optionList = optionList :+ VideoOption("pixel_format", format, s_min, fps_min.toInt, s_max, fps_max.toInt)

          case vcodecReg(vcodec, s_min, fps_min, s_max, fps_max) =>
            optionList = optionList :+ VideoOption("vcodec", vcodec, s_min, fps_min.toInt, s_max, fps_max.toInt)

          case audioReg(ch_min, bits_min, rate_min, ch_max, bits_max, rate_max) =>
            optionList = optionList :+ AudioOption(ch_min.toInt, bits_min.toInt, rate_min.toInt, ch_max.toInt, bits_max.toInt, rate_max.toInt)

          case _ =>
          //                log.info(s"useless info or format error, $e")
        }

      case x@_ =>
        log.info(s"match error, $x")
    }
    optionList
  }


  def getDeviceOptionInMac(output:String,device:Device,inputType:String) ={
//    println("work in mac")
//    println(output)
    var optionList: List[DeviceOption] = List.empty
    if (inputType == "audio"){
      //TODO:对音频设备需要单独处理
      val reg = """(.*?bitrate: )([0-9]+)[.|\n]*?\n""".r
    }else{
      val reg = """\[avfoundation @(.*?)](.*?)\n""".r
      //pixelFormat这个是主要的，其他的没用到，而且mac获取的信息也很有限
      val pixelReg = """(.*?)@\[(.*?) (.*?)]fps""".r
      val audioReg = """[0-9]*?""".r
      //音频直接对output进行解析
      val rst = reg.findAllIn(output)//匹配的每一行列表
      rst.foreach {
        case reg(_, b) =>
          var a = b
          //去掉前面所有空格
          while (a.startsWith(" ")) a = a.drop(1)
          a match {
            case audioReg(value) =>
              optionList = optionList :+ AudioOption(1,8,value.toInt,1,8,value.toInt)
            case pixelReg(s, fps_min, fps_max) =>
              optionList = optionList :+ VideoOption("pixel_format", "uyvy422", s, fps_min.toFloat.toInt, s, fps_max.toFloat.toInt)
            case _ =>
            //            println(s"useless info or format error, $a")
          }
        case x@_ =>
          println(s"match error, $x")

        case _ =>

      }
    }

    optionList
  }

  def init: Future[Unit] = {
    //    log.debug(s"${availableDevices.size}")
    log.info(s"Try to get pc device info...")
    val availableDevices = getAllDevices
    availableDevices.map { ad =>
      ad.foreach { i =>
        getDeviceOption(i._1, i._2)
      }
    }
  }


  /**
    * 获取所有设备的选项
    */
  def getAllDeviceOptions ={
    val availableDevices = getAllDevices
    val te = availableDevices.map { ads =>
      ads.map { ad =>
        val optionList = getDeviceOption(ad._1, ad._2)
        var deviceOptionMap: Map[Device, List[DeviceOption]] = Map.empty
        optionList.map{
          list =>
            val `type` = if (ad._2.contains("video")) "video" else "audio"
            deviceOptionMap += (Device(`type`,ad._1)->list)
            deviceOptionMap
        }
      }
    }
    //处理future
    te.flatMap(f => scala.concurrent.Future.sequence(f))
  }

  /**
    * 需要先init之后才能返回正确的数据，但是init方法不提供返回值，所以无法知道是否init成功
    * @return
    */
  @deprecated
  def getDeviceOptions: mutable.HashMap[Device, List[DeviceOption]] = {
    deviceOptionMap
  }

  def parseImgResolution(re: String): (Int, Int) = {
    val reg = """([.0-9]+)x([.0-9]+)""".r
    re match {
      case reg(w, h) =>
        (w.toInt, h.toInt)
      case _ =>
        log.info("parse resolution error, use default resolution")
        (640, 360)
    }
  }


  def main(args: Array[String]): Unit = {

    //输出所有设备选项的用例（外部可以直接调用这个方法，也可以调用init初始化选项信息）
/*    val r = getAllDeviceOptions
    r.map{
      l => l.map{
        m=> m.map{
          n=>
            println("设备" + n._1 + "选项列表为" + n._2.toString())
        }
      }
    }*/

    //使用init初始化
/*    init
    Thread.sleep(2000)
    deviceOptionMap.map{
      n=>
        println("设备" + n._1 + "选项列表为" + n._2.toString())
    }*/
    //输出视频设备的信息用例
/*    getDeviceOption("facetime","video0").map{
      list =>
        println(list)
    }*/
    //输出音频设备的信息用例
/*    getDeviceOption("airpods","audio1").map{
      list =>
        println(list)
        System.exit(0)
    }*/

    //    def useADevice(dName: String) = s"ffmpeg -f dshow -list_options true -i video=\"${dName}\"";
    //    val useDevices = "ffmpeg -f dshow -i video=\"Integrated Camera\":audio=\"Microphone name here\" out.mp4"
    //    val ip = "ipconfig"
/*    val availableDevices = getAllDevices
    availableDevices.foreach(i => i.foreach(j => getDeviceOption(j._1, j._2)))
        println(deviceOptionMap)
        println(parseImgResolution(AppSettings.imgResolution))*/

  }


}
