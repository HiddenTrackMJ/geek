package org.seekloud.geek.client.utils.rtpClient

import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.{ClosedByInterruptException, DatagramChannel}
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{ScheduledFuture, ScheduledThreadPoolExecutor, TimeUnit}

import akka.actor.typed.ActorRef
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import org.seekloud.geek.client.utils.HttpUtil
import org.seekloud.geek.shared.rtp.Protocol
import org.seekloud.geek.shared.rtp.Protocol.{Data, Header}
import org.slf4j.LoggerFactory
import org.seekloud.geek.client.Boot.executor

import scala.collection.mutable

/**
  * Created by haoshuhan on 2019/7/23.
  */
class PushStreamClient(local_host: String, local_port: Int, pushStreamDst: InetSocketAddress,
                       actor: ActorRef[Protocol.Command], rtpServerDst:String) extends HttpUtil {

  case class DelayReq(
    liveId:String,
    delay: Double
  )

  case class DelayRsp(
    errCode: Int,
    msg: String
  )

  private val log = LoggerFactory.getLogger(this.getClass)

  def getIp: String = local_host

 /* private*/ val pushChannel = DatagramChannel.open()

  var differ = -1l

  var seq = new AtomicInteger(0)


  val seqMap: mutable.Map[String, AtomicInteger] = mutable.Map.empty[String, AtomicInteger]

  val authType = 101

  val authRspType = 102

  val authRefuseResponse = 103

  val stopStreamReq = 122

  val stopStreamRsp = 123

  val calcDelayReq = 11

  val calcDelayRsp = 12

  val tsStreamType = 33

  var calcDelay: ScheduledFuture[_] = _

  val calcDelayExecutor = new ScheduledThreadPoolExecutor(1)

  val pushStreamDst1 = pushStreamDst

  val liveIdMap: mutable.Map[String, Int]  = mutable.Map.empty[String, Int]

  val liveIdDelayMap: mutable.Map[String, List[Double]] = mutable.Map.empty

  val pushBuf = ByteBuffer.allocate(1024 * 64) //buffer should not be too small (e.g. smaller than 1024).

  def getServerTimestamp() = System.currentTimeMillis() + differ

  //健全
  def auth(liveId: String, liveCode: String) = {
    val payload = s"$liveId#$liveCode".getBytes("UTF-8")
    sendData(Header(authType, 0, 0, 0, 0), payload, pushStreamDst, pushChannel)
    liveIdMap += (liveId -> -1)
  }


  def pushStreamData(liveId: String, data: Array[Byte]) = {
    liveIdMap.get(liveId) match {
      case Some(-1) =>
        actor ! Protocol.PushStreamError(liveId, 10001, "该liveId暂未收到鉴权成功消息！")
        println("该liveId暂未收到鉴权成功消息！")
      case Some(-2) =>
        actor ! Protocol.PushStreamError(liveId, 10002, "该liveId鉴权失败！")
        println("该liveId鉴权失败！")
      case None =>
        actor ! Protocol.PushStreamError(liveId, 10003, "该liveId未鉴权！")
        println("该liveId未鉴权！")
      case Some(ssrc) =>
        sendData(Header(tsStreamType, 0, seqMap.getOrElseUpdate(liveId, new AtomicInteger(0)).getAndIncrement(), ssrc, System.currentTimeMillis()),
          data, pushStreamDst, pushChannel)
    }

  }

  def sendDelay(liveId: String): Unit ={
    val methodName = "sendDelay"
    val url = s"$rtpServerDst/theia/rtpServer/api/delay"
    val json = DelayReq(liveId, liveIdDelayMap(liveId).sum/10.0).asJson.noSpaces
    postJsonRequestSend(methodName, url, Nil, json).map{
      case Left(e) =>
        log.error("send packageLoss error")
        Left(e)

      case Right(v) =>
        decode[DelayRsp](v) match {
          case Left(error) =>
            log.error("decode error")
            Left(error)

          case Right(value) =>
            Right(value)
        }
    }
  }


  private val pushThread = new Thread(() => {
    try {
      while (true) {
        pushBuf.clear()
        pushChannel.receive(pushBuf)
        pushBuf.flip()
        val byteArray = new Array[Byte](pushBuf.remaining())
        pushBuf.get(byteArray)
        val data = parseData(byteArray)
        val liveId = new String(data.body, "UTF-8")
        if (data.header.payloadType == authRspType) {
          val ssrc = data.header.ssrc
          liveIdMap += (liveId -> ssrc)
          liveIdDelayMap.update(liveId, List.empty[Double])
          actor ! Protocol.AuthRsp(liveId, true)

        } else if (data.header.payloadType == authRefuseResponse) {
          liveIdMap += (liveId -> -2)
          actor ! Protocol.AuthRsp(liveId, false)
        }else if(data.header.payloadType == calcDelayRsp){
          val liveId = new String(data.body, "UTF-8")
          val time = System.currentTimeMillis()
          val now = toInt(toByte(time, 4).toArray)
          val delay = Math.abs(now - data.header.timestamp.toInt).toDouble / 2.0
          liveIdDelayMap.update(liveId, liveIdDelayMap(liveId) :+ delay)
          if(liveIdDelayMap(liveId).length == 10){
            sendDelay(liveId)
            liveIdDelayMap.update(liveId, List.empty[Double])
          }
        } else if (data.header.payloadType == stopStreamRsp) {
          interrupt()
        }
      }
    } catch {
      case e: ClosedByInterruptException =>
        log.info(s"push stream client closed")
      case e: Exception =>
        log.error(s"push thread error: $e")

    }
  })

  private val calcThread = new Thread( () =>{
    try{
      calcDelay = calcDelayExecutor.scheduleAtFixedRate(
        new Runnable {
          override def run(): Unit = {
            liveIdDelayMap.keys.foreach{liveId =>
              val time = System.currentTimeMillis()
              sendData(Header(calcDelayReq, 0, 0, 0, time), liveId.getBytes("UTF-8"), pushStreamDst, pushChannel, calcDelay = true)
            }
          }
        },
        1,
        1,
        TimeUnit.SECONDS
      )
    } catch {
      case e: ClosedByInterruptException =>
        log.info(s"calc stream client closed")
      case e: Exception =>
        log.error(s"calc thread error: $e")
    }
  }
  )



  def sendData(header: Header, data: Array[Byte], dst: InetSocketAddress,
    channel: DatagramChannel, maxLength: Int = 1500, isLiveIds: Boolean = false, calcDelay: Boolean = false) = {
    val seq = header.seq
    //test
    if(calcDelay && liveIdDelayMap.get(s"liveIdTest-${header.ssrc}").isEmpty){
      liveIdDelayMap.update(s"liveIdTest-${header.ssrc}", List.empty[Double])
    }
    val realTimestamp = if(!calcDelay) header.timestamp + differ else header.timestamp
    val seq_byte_list = toByte(seq, 2)
    val ts_byte_list = toByte(realTimestamp, 4)
    (0 until (data.length / (maxLength - 12)) + 1).map { i =>
      val dropData = data.drop(i * (maxLength - 12))
      val length = scala.math.min(dropData.length, maxLength - 12)
      val rtp_buf: ByteBuffer = ByteBuffer.allocate(12 + length)
      val payload = dropData.take(length)
      rtp_buf.clear()
      rtp_buf.put(0x80.toByte)
      if(i != (data.length / (maxLength - 12)) || !isLiveIds) rtp_buf.put(header.payloadType.toByte)
      else rtp_buf.put((header.payloadType | 0x80).toByte)
      if(!isLiveIds) seq_byte_list.foreach(rtp_buf.put(_))
      else toByte(i, 2).foreach(rtp_buf.put(_))
      ts_byte_list.foreach(rtp_buf.put(_))
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
    if (differ == -1l && payloadType != calcDelayRsp) {
      differ = ts - System.currentTimeMillis().toInt
//      val differ1 = ts - (System.currentTimeMillis() & 0xFFFFFFFFL)
//      val differ2 = ts - (System.currentTimeMillis() & 0x1FFFFFFFFL)
//      if (Math.abs(differ1) < Math.abs(differ2)) differ = differ1 else differ = differ2
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
      differ = ts - System.currentTimeMillis().toInt
//      val differ1 = ts - (System.currentTimeMillis() & 0xFFFFFFFFL)
//      val differ2 = ts - (System.currentTimeMillis() & 0x1FFFFFFFFL)
//      if (Math.abs(differ1) < Math.abs(differ2)) differ = differ1 else differ = differ2
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

  def authStart(): Unit = {
    pushChannel.socket().setReuseAddress(true)

//    try {
      pushChannel.socket().bind(new InetSocketAddress(local_host, local_port))
//    } catch {
//      case e: Exception =>
//        log.debug(s"push channel bind $local_host error: $e")
//
//    }
    pushThread.start()
    calcStart()

  }


  def close(): Unit = {
    val liveIdList = liveIdMap.filter(i => i._2 != -1 && i._2 != -2).keys.toList
    val payload = liveIdList.mkString("#").getBytes("UTF-8")
    sendData(Header(stopStreamReq, 0, 0, 0, 0), payload, pushStreamDst, pushChannel)
  }

  private def calcStart() : Unit ={
    calcThread.start()
  }

  private def interrupt(): Unit = {
    calcDelay.cancel(false)
    pushThread.interrupt()
    calcThread.interrupt()

    actor ! Protocol.CloseSuccess
  }


}

