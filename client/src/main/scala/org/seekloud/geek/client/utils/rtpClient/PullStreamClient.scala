package org.seekloud.geek.client.utils.rtpClient

import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.{ClosedByInterruptException, DatagramChannel}
import java.util.concurrent.{ScheduledFuture, ScheduledThreadPoolExecutor, TimeUnit}

import akka.actor.typed.ActorRef
import org.seekloud.geek.client.utils.HttpUtil
import org.seekloud.geek.shared.rtp.Protocol
import org.seekloud.geek.shared.rtp.Protocol.{CloseSuccess, Data, Header, PullStreamData, PullStreamPacketLoss, PullStreamReady, StreamStop}
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import org.seekloud.geek.client.Boot.executor


/**
  * Created by haoshuhan on 2019/7/23.
  * param rtpServerDst:
  * http://10.1.29.246:30390 //开发-内网
  * https://media.seekloud.com:50443 //开发-公网
  * http://10.1.29.244:30390 //生产-内网
  * https://media.seekloud.org:50443 //生产-公网
  */
class PullStreamClient(
  local_host: String,
  local_port: Int,
  pullStreamDst: InetSocketAddress,
  actor: ActorRef[Protocol.Command],
  rtpServerDst:String
) extends HttpUtil {

  import io.circe.generic.auto._
  import io.circe.parser.decode
  import io.circe.syntax._

  private val log = LoggerFactory.getLogger(this.getClass)

  val bufferLength = 10


  private var clientId = -1

  var differ = -1L

  val pullStreamReq = 111
  val pullStreamRsp = 112
  val pullStreamRefuseResponse = 113

  val tsStreamType = 33

  val getClientIdReq = 114
  val getClientIdRsp = 115

  val pullStreamUserHeartbeat = 116

  val stopPullingReq = 117
  val stopPullingRsp = 118

  val streamStopped = 121

  //todo 订阅者收到pullStreamRsp后的回复消息
  val subscriberRcvSSRC = 99
  val pullLiveIdMap: mutable.Map[Int, String] = mutable.Map.empty[Int, String] // (ssrcInt -> liveId)

  case class SeqInfo(seq: Int, data: Array[Byte])

//  val streamBuffer = mutable.Map.empty[String, mutable.Map[Int, Array[Byte]]] // (liveId, (seq, data))
  val streamBuffer = mutable.Map.empty[String, ArrayBuffer[SeqInfo]] // (liveId, (seq, data))

  case class PackageInfo (totalNum: Int, lostNum: Int)

//  val packageTotalLossInfo = mutable.Map.empty[String, PackageInfo]

  case class LastSeqInfo(lastSendSeq: Int, lastRecvSeq: Int)

  val lastSendSeqMap = mutable.Map.empty[String, LastSeqInfo]

  val pullStreamBuf = ByteBuffer.allocate(1024 * 64)

  val pullChannel: DatagramChannel = DatagramChannel.open()

  var timer: ScheduledFuture[_] = _

  var timer4PackageLoss: ScheduledFuture[_] = _

  var timerCount60s: ScheduledFuture[_] = _

  var timerCount10s: ScheduledFuture[_] = _

  var timerCount2s: ScheduledFuture[_] = _

  val count60sExecutor = new ScheduledThreadPoolExecutor(1)

  val count10sExecutor = new ScheduledThreadPoolExecutor(1)

  val count2sExecutor = new ScheduledThreadPoolExecutor(1)

  val audioExecutor = new ScheduledThreadPoolExecutor(1)

  val packageLossExecutor = new ScheduledThreadPoolExecutor(1)

  private val dataQueue = new java.util.concurrent.LinkedBlockingQueue[Array[Byte]]()

  sealed trait Command

  case class ThrowPack(id: String) extends Command

  case class LossPack(id: String, num: Int) extends Command

  case class SendPack(id: String, length: Int) extends Command

  case class Calc(time: Int) extends Command

  case object SendPackageLoss extends Command

  private val commandQueue = new java.util.concurrent.LinkedBlockingDeque[Command]()

  case class DataInfo(sendPack: Int, lossPack: Int, throwPack: Int, band: Int)

  var allInfoMap: Map[String, Map[Int, DataInfo]] = Map.empty


  case class PackageLossInfo(lossScale60: Double,  throwScale60: Double, lossScale10: Double,
    throwScale10: Double, lossScale2: Double, throwScale2: Double, lossTotal: Double)

  case class BandWidthInfo(bandWidth60s: Double, bandWidth10s: Double, bandWidth2s: Double)

  case class DataScaleInfo(lossScale: Double, throwScale: Double)

  val packageLossInfoMap: mutable.Map[String,Map[Int, DataScaleInfo]] = mutable.Map.empty

  var bandWidthInfoMap: Map[String, Map[Int, Double]] = Map.empty

  case class TimeSeq(timestamp: Long, seq: Int)

  case class PackageLossReq(
                             clientId:Int,
                             packageLossMap:mutable.Map[String, PackageLossInfo]
                           )

  case class PackageLossRsp(
                             errCode: Int,
                             msg: String
                           )

  def getPackageLoss(): mutable.Map[String, PackageLossInfo] = {
    packageLossInfoMap.map{info =>
      val totalInfo =  allInfoMap(info._1)(-1)
      info._1 -> PackageLossInfo(info._2(60).lossScale, info._2(60).throwScale, info._2(10).lossScale, info._2(10).throwScale,
        info._2(2).lossScale, info._2(2).throwScale, totalInfo.lossPack.toDouble/(totalInfo.lossPack+totalInfo.sendPack).toDouble)
    }
  }

  def getBandWidth(): Map[String, BandWidthInfo] ={
    bandWidthInfoMap.map{info =>
      info._1 -> BandWidthInfo(info._2(60), info._2(10), info._2(2))
    }
  }

  def sendPackageLoss2Sever(clientId:Int) = {
    val methodName = "sendPackageLoss"
    val url = s"$rtpServerDst/theia/rtpServer/api/packageLoss"
    val json = PackageLossReq(clientId, getPackageLoss()).asJson.noSpaces
    postJsonRequestSend(methodName, url, Nil, json).map{
      case Left(e) =>
        log.error("send packageLoss error")
        Left(e)

      case Right(v) =>
        decode[PackageLossRsp](v) match {
          case Left(error) =>
            log.error("decode error")
            Left(error)

          case Right(value) =>
            Right(value)
        }
    }
  }

  def getServerTimestamp() = System.currentTimeMillis() + differ

  def pullStreamData(liveIds: List[String]): Unit = {
    val data = liveIds.mkString("#").getBytes("UTF-8")
    sendData(Header(pullStreamReq, 0, 0, clientId, System.currentTimeMillis()),
      data, pullStreamDst, pullChannel, 750, true)
  }

  //将收到的 pullStreamRsp再转发回给rtpServer，以令RtpServer得知客户端已收到pullStreamRsp
  def subscriberRcvSSRC(liveIdsByte: Array[Byte]): Unit ={
    sendData(Header(subscriberRcvSSRC, 0, 0, clientId, System.currentTimeMillis()),
      liveIdsByte, pullStreamDst, pullChannel, 750)
  }


  def getClientId(): Unit = {
    sendData(Header(getClientIdReq, 0, 0, 0, System.currentTimeMillis()),
      Array.empty[Byte], pullStreamDst, pullChannel)
  }

  private def sendPackageRunnable(): Runnable = new Runnable {
    override def run(): Unit = {
      if(clientId != -1) {
        sendPackageLoss2Sever(clientId).map {
          case Left(e) =>
            log.error(s"send packageLoss error $e")

          case Right(value) =>
//            log.info(s"send packageLoss successfully")
        }
      }
    }
  }

  private def heartbeatRunnable(): Runnable = new Runnable {
    override def run(): Unit = {
      if (clientId != -1) {
        sendData(Header(pullStreamUserHeartbeat, 0, 0, clientId, System.currentTimeMillis()),
          Array.empty[Byte], pullStreamDst, pullChannel)
      }
    }
  }

  val recvThread = new Thread(() => {
    try {
      while(true) {
        pullStreamBuf.clear()
        pullChannel.receive(pullStreamBuf)
        pullStreamBuf.flip()
        val byteArray = new Array[Byte](pullStreamBuf.remaining())
        pullStreamBuf.get(byteArray)
        dataQueue.put(byteArray)
      }


    } catch {
      case e: ClosedByInterruptException =>
        log.info(s"pull stream client closed")
      case e: Exception =>
        log.error(s"recvThread catch exception: $e")

    }

  })

  val commandThread = new Thread( () =>{
    try {
      while(true) {
        val command = commandQueue.take()
        command match {
          case SendPack(id, length) =>
            val allInfoOption = allInfoMap.get(id)
            if(allInfoOption.isDefined){
              val allInfo = allInfoOption.get
              allInfoMap += (id -> allInfo.map{info =>
                info._1 -> info._2.copy(sendPack = info._2.sendPack + 1, band = info._2.band + length)
              })
            }else{
              allInfoMap += (id -> Map(60 -> DataInfo(0, 0, 0, 0), 10 -> DataInfo(0, 0, 0, 0),
                                        2 -> DataInfo(0, 0, 0, 0), -1 -> DataInfo(0, 0, 0, 0)))
              packageLossInfoMap.update(id, Map(60 -> DataScaleInfo(0.0, 0.0), 10 -> DataScaleInfo(0.0, 0.0), 2 -> DataScaleInfo(0.0, 0.0)))
              bandWidthInfoMap += (id -> Map(60 -> 0.0, 10 -> 0.0, 2 -> 0.0))
            }

          case LossPack(id, num) =>
            val allInfo = allInfoMap(id)
            allInfoMap += (id -> allInfo.map{info =>
              info._1 -> info._2.copy(lossPack = info._2.lossPack + num)
            })

          case ThrowPack(id) =>
            val allInfo = allInfoMap(id)
            allInfoMap += (id -> allInfo.map{ info =>
              info._1 -> info._2.copy(throwPack = info._2.throwPack + 1)
            })

          case Calc(time) =>
            allInfoMap =
              allInfoMap.map{allInfo =>
                val dataInfo = allInfo._2(time)
                val total = dataInfo.sendPack + dataInfo.lossPack
                val lossScale = dataInfo.lossPack.toDouble / total.toDouble
                val throwScale = dataInfo.throwPack.toDouble / total.toDouble
                val packageLoss = packageLossInfoMap(allInfo._1)
                val newScale = packageLoss + (time -> DataScaleInfo(lossScale, throwScale))
                packageLossInfoMap.update(allInfo._1, newScale)
                val bandInfo = bandWidthInfoMap(allInfo._1)
                val newBandInfo = bandInfo + (time -> allInfo._2(time).band*8.toDouble/time.toDouble)
                bandWidthInfoMap += (allInfo._1 -> newBandInfo)
                val newInfo = allInfo._2 + (time -> DataInfo(0, 0, 0, 0))
                allInfo._1 -> newInfo
              }

          case SendPackageLoss =>
            if(clientId != -1) {
              sendPackageLoss2Sever(clientId).map {
                case Left(e) =>
                  log.error(s"send packageLoss error $e")

                case Right(value) =>
//                  log.info(s"send packageLoss successfully")
              }
            }
        }
      }


    } catch {
      case e: ClosedByInterruptException =>
        log.info(s"get command client closed")
      case e: Exception =>
        log.error(s"commandThread catch exception: $e")

    }
  }
  )

  val pullStreamThread = new Thread(() => {
    try {
      while (true) {
        val byteArray = dataQueue.take()
//        if (dataQueue.size() >= 5)println(s"data queue length: ${dataQueue.size()}")
        val data: Data = parseData(byteArray)
        val payloadType = data.header.payloadType

        payloadType match {
          case t if t == pullStreamRsp =>
            val liveIdSsrc = new String(data.body, "UTF-8").split(";")
            val liveInfo = liveIdSsrc.map {ls =>
              val liveId = ls.split("#").head.trim
              val ssrcString = ls.split("#")(1).trim
              (liveId, ssrcString)
            }
            val noStreamList = liveInfo.filter(_._2 == "null").map(_._1).toList
            val streamList = liveInfo.filterNot(l => noStreamList.contains(l._1)).map(i => (i._2.toInt, i._1)).toMap
            if (noStreamList.nonEmpty) actor ! Protocol.NoStream(noStreamList)
            pullLiveIdMap ++= streamList
            if (streamList.nonEmpty) actor ! Protocol.PullStreamReqSuccess(streamList.values.toList)
            //todo 回复rtpServer：订阅者已经收到 pullStreamRsp
            val rspList = liveInfo.filter(_._2 != "null").toList
            if(rspList.nonEmpty) {
              val liveIdsByte = rspList.map {info =>
                s"${info._1}"
              }.reduce(_ + ";" + _).getBytes("UTF-8")
              subscriberRcvSSRC(liveIdsByte)
            }



          case t if t == tsStreamType =>
            val ssrc = data.header.ssrc
            val seq = data.header.seq
            val liveId = pullLiveIdMap.get(ssrc)
            liveId match {
              case Some(id) =>
                streamBuffer.get(id) match {
                  case Some(bufferedData) =>
                    val seqInfo = SeqInfo(seq, data.body)
                    lastSendSeqMap.get(id) match {
                        //ex: lastSendSeq: 5 seq: 2
                      case Some(lastSeqInfo) if lastSeqInfo.lastSendSeq > seq && lastSeqInfo.lastSendSeq - seq < 20000 =>
                        log.warn(
                          s"超过乱序最大范围，丢弃该数据包, seq:$seq, " +
                            s"lastSendSeq: ${lastSeqInfo.lastSendSeq}, " +
                            s"lastRecvSeq: ${lastSeqInfo.lastRecvSeq}")
                        commandQueue.add(ThrowPack(id))
                      case Some(lastSeqInfo) if lastSeqInfo.lastSendSeq > seq=>  //seq重置0情况
                        bufferedData += seqInfo

                      case Some(lastSeqInfo) =>
                        if (seq - lastSeqInfo.lastRecvSeq == 1) bufferedData += seqInfo
                        else insertToBufferBySeq(bufferedData, seqInfo)


                      case None => //从未转发过数据包，直接按序放入buffer

                        insertToBufferBySeq(bufferedData, seqInfo)
                    }

                    if (bufferedData.length > bufferLength) { //buffer长度 > 10, 转发第一个数据包,
                      val sendSeq = bufferedData.head.seq
                      actor ! PullStreamData(id, sendSeq, bufferedData.head.data)

                      if(lastSendSeqMap.get(id).isDefined) {
                        val lastSeq = lastSendSeqMap(id).lastSendSeq
                        if (!(sendSeq - lastSeq == 1 || lastSeq - sendSeq == 65535)) {
                          if (lastSeq - sendSeq > 20000) commandQueue.add(LossPack(id, 65535 - lastSeq + sendSeq))
                          else commandQueue.add(LossPack(id, sendSeq - lastSeq - 1))
                        }
                      }
                      bufferedData.remove(0)
                      lastSendSeqMap += id -> LastSeqInfo(sendSeq, seq)
                      commandQueue.add(SendPack(id, bufferedData.head.data.length))
                    }


                  case None => //第一次收到流信息
                    val arrayBuffer = new ArrayBuffer[SeqInfo]()
                    arrayBuffer.insert(0, SeqInfo(seq, data.body))
                    streamBuffer += (id -> arrayBuffer) //加入到buffer中
                }

              case None =>
                log.warn(s"unkonwn data with ssrc $ssrc")

            }

          case t if t == pullStreamRefuseResponse =>
            actor ! PullStreamPacketLoss

          case t if t == getClientIdRsp =>
            clientId = data.header.ssrc
            actor ! PullStreamReady
            timer = audioExecutor.scheduleAtFixedRate(
              heartbeatRunnable(),
              1,
              1,
              TimeUnit.SECONDS)
            timer4PackageLoss = packageLossExecutor.scheduleAtFixedRate(
              //sendPackageRunnable(),
              new Runnable {
                override def run(): Unit = {
                  commandQueue.add(SendPackageLoss)
                }
              },
              1,
              5,
              TimeUnit.SECONDS)

            timerCount60s = count60sExecutor.scheduleAtFixedRate(
              new Runnable {
                override def run(): Unit = {
                  commandQueue.add(Calc(60))
                }
              },
              60,
              60,
              TimeUnit.SECONDS
            )

            timerCount10s = count10sExecutor.scheduleAtFixedRate(
              new Runnable {
                override def run(): Unit = {
                  commandQueue.add(Calc(10))
                }
              },
              10,
              10,
              TimeUnit.SECONDS
            )

            timerCount2s = count2sExecutor.scheduleAtFixedRate(
              new Runnable {
                override def run(): Unit = {
                  commandQueue.add(Calc(2))
                }
              },
              2,
              2,
              TimeUnit.SECONDS
            )

          case t if t == streamStopped =>
            val liveId = new String(data.body, "UTF-8")
            actor ! StreamStop(liveId)
            streamBuffer -= liveId
            allInfoMap -= liveId
            packageLossInfoMap.remove(liveId)
            bandWidthInfoMap -= liveId

          case t if t == stopPullingRsp =>
            interrupt()


          case _ =>


        }
      }
    } catch {
      case e: ClosedByInterruptException =>
        log.info(s"pull stream client closed")
      case e: Exception =>
        log.error(s"error: $e")

    }

  }
  )



  def sendData(header: Header, data: Array[Byte], dst: InetSocketAddress,
               channel: DatagramChannel, maxLength: Int = 1500, isLiveIds: Boolean = false) = {
    val seq = header.seq
    val realTimestamp = header.timestamp + differ
    val seq_byte_list = toByte(seq, 2)
    val ts_byte_list = toByte(realTimestamp, 4)
    (0 until (data.length / (maxLength - 12)) + 1).map { i =>
      val dropData = data.drop(i * (maxLength - 12))
      val length = scala.math.min(dropData.length, maxLength - 12)
      val rtp_buf: ByteBuffer = ByteBuffer.allocate(12 + length)
      val payload = dropData.take(length)
      rtp_buf.clear()
      rtp_buf.put(0x80.toByte)
      if (i != data.length / (maxLength - 12) || !isLiveIds) rtp_buf.put(header.payloadType.toByte)
      else rtp_buf.put((header.payloadType | 0x80).toByte)
      if (!isLiveIds) seq_byte_list.foreach(rtp_buf.put)
      else toByte(i, 2).foreach(rtp_buf.put)
      ts_byte_list.foreach(rtp_buf.put)
      rtp_buf.putInt(header.ssrc)
      rtp_buf.put(payload)
      rtp_buf.flip()
      rtp_buf.array().take(rtp_buf.remaining())
      channel.send(rtp_buf, dst)
    }
  }

  def parseData(byteBuffer: ByteBuffer) = {
    byteBuffer.flip()
    val first_byte = byteBuffer.get()
    val payloadType = byteBuffer.get()
    val seq_h = byteBuffer.get()
    val seq_l = byteBuffer.get()
    val ts = byteBuffer.getInt()
    if (differ == -1l) {
//      val differ1 = ts - (System.currentTimeMillis() & 0xFFFFFFFFL)
//      val differ2 = ts - (System.currentTimeMillis() & 0x1FFFFFFFFL)
//      if (Math.abs(differ1) < Math.abs(differ2)) differ = differ1 else differ = differ2
      differ = ts - System.currentTimeMillis().toInt
    }
    val ssrc = byteBuffer.getInt()
    val seq = toInt(Array(seq_h, seq_l))
    val data = new Array[Byte](byteBuffer.remaining())
    byteBuffer.get(data)
    Data(Header(payloadType, 0, seq, ssrc, ts), data)
  }

  def parseData(bytes: Array[Byte]) = {
    val first_byte = bytes(0)
    val payloadType = bytes(1)
    val seq = toInt(bytes.slice(2, 4))
    val ts = toInt(bytes.slice(4, 8))
    if (differ == -1l) {
//      val differ1 = ts - (System.currentTimeMillis() & 0xFFFFFFFFL)
//      val differ2 = ts - (System.currentTimeMillis() & 0x1FFFFFFFFL)
//      if (Math.abs(differ1) < Math.abs(differ2)) differ = differ1 else differ = differ2
      differ = ts - System.currentTimeMillis().toInt
    }
    val ssrc = toInt(bytes.slice(8, 12))
    val data = bytes.drop(12)
    Data(Header(payloadType, 0, seq, ssrc, ts), data)
  }


  def toByte(num: Long, byte_num: Int) = {
    (0 until byte_num).map { index =>
      (num >> ((byte_num - index - 1) * 8) & 0xFF).toByte
    }.toList
  }

  def toInt(numArr: Array[Byte]) = {
    numArr.zipWithIndex.map { rst =>
      (rst._1 & 0xFF) << (8 * (numArr.length - rst._2 - 1))
    }.sum
  }

  def insertToBufferBySeq(buffer: ArrayBuffer[SeqInfo], seqInfo: SeqInfo): Unit = {
    var ifGetResult = false
    for (i <- buffer.zipWithIndex if !ifGetResult) { //排序放入buffer
      if (i._1.seq > seqInfo.seq) {
        ifGetResult = true
        buffer.insert(i._2, seqInfo)
      }
    }
    if (!ifGetResult) buffer += seqInfo
  }

  def pullStreamStart(): Unit = {
    pullChannel.socket().setReuseAddress(true)
//    try {
      pullChannel.socket().bind(new InetSocketAddress(local_host, local_port))
//    } catch {
//      case e: Exception =>
//        log.debug(s"pull channel bind $local_host error: $e")
//    }
    recvThread.start()
    pullStreamThread.start()
    commandThread.start()
    getClientId()
  }

  def close(): Unit = {
    timer.cancel(false)
    timer4PackageLoss.cancel(false)
    timerCount60s.cancel(false)
    timerCount10s.cancel(false)
    timerCount2s.cancel(false)
    sendData(Header(stopPullingReq, 0, 0, clientId, System.currentTimeMillis()),
      Array.empty[Byte], pullStreamDst, pullChannel)
  }

  private def interrupt(): Unit = {
    recvThread.interrupt()
    pullStreamThread.interrupt()
    commandThread.interrupt()
    actor ! CloseSuccess
  }


}

