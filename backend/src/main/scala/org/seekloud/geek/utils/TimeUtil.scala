package org.seekloud.geek.utils

import java.text.SimpleDateFormat

import org.slf4j.LoggerFactory

/**
  * Created by hongruying on 2017/10/25.
  */
object TimeUtil {
  private val log = LoggerFactory.getLogger(this.getClass)
  import com.github.nscala_time.time.Imports._

  val fmt_yyyyMMdd = DateTimeFormat.forPattern("yyyyMMdd")
  val fmt_yyyyMMddHHmmss = DateTimeFormat.forPattern("yyyyMMddHHmmss")
  val fmt_yyyysMMsdd = DateTimeFormat.forPattern("yyyy/MM/dd")
  val fmt_yyyysMM = DateTimeFormat.forPattern("yyyy/MM")
  val fmt_yyyyMM = DateTimeFormat.forPattern("yyyyMM")
  val fmt_yyyyWW = DateTimeFormat.forPattern("yyyyww")

  def date2TimeStamp(date: (String,String)): Either[String,(Long,Long)] = {
    try {
      Right((new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(date._1).getTime,new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(date._2).getTime))
    }catch{
      case e: Exception =>
        Left(e.getMessage)
    }
  }

  def timeStamp2yyyyMMdd(timestamp: Long) = {
    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(timestamp)
  }

  def timestamp2yyyyMMdd(timestamp: Long) = {
    new SimpleDateFormat("yyyy-MM-dd").format(timestamp)
  }

  def getDate(millis:Long) = {
    val d = new DateTime(millis)
    d.toString(fmt_yyyyMMdd)

  }

  /**
    * 根据时间转换为当天的时间戳 HHmmss
    * */
  def parse_HHmmss(timeStr:String) = {
    try{
      Some(fmt_yyyyMMddHHmmss.parseDateTime(getDate(System.currentTimeMillis()) + timeStr).getMillis)
    }catch {
      case e:Exception =>
        log.warn(s"parse time=${timeStr} exception,error:${e.getMessage}")
        None
    }
  }

  private def nextDayDelay() = {
    import com.github.nscala_time.time.Imports._
    val curTime = System.currentTimeMillis()
    val nextDay = new DateTime(curTime).plusDays(1).withTimeAtStartOfDay().getMillis
    println(curTime)
    println(nextDay)
    nextDay - curTime + 1000
  }

  def getMinusDayTimestamp(minusDay:Int):Long = {
    DateTime.now().minusDays(minusDay).withTimeAtStartOfDay().getMillis
  }

  def main(args: Array[String]): Unit = {
    val date1 = "2018-01-22 00:00:00"
    val date2 = "2018-01-23 00:00:00"
    println(date2TimeStamp((date1,date2)))
  }

}
