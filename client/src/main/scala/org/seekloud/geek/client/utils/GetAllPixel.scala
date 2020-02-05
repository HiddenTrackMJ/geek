package org.seekloud.geek.client.utils

import org.seekloud.geek.capture.sdk.DeviceUtil

import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

object GetAllPixel {

  def getAllDevicePixel()(implicit ec:ExecutionContext)={
    val availableDevices = DeviceUtil.getAllDeviceOptions
    val devicePixelOptionSet:mutable.SortedSet[String] = mutable.SortedSet.empty
    val avd = availableDevices.map{ am =>
      am.foreach{ a2m =>
        a2m.values.foreach{ a3m =>
          for(i<-a3m.indices){
            if(!a3m(i).toString.contains("AudioOption")){
              val pixelOpt = a3m(i).asInstanceOf[DeviceUtil.VideoOption]
              if(pixelOpt.`type`.equals("pixel_format")){
                devicePixelOptionSet.add(pixelOpt.s_max)
              }
            }
          }
        }
      }
    }
    try {
      Await.result(avd, Duration.Inf)
    }catch {
      case t:Throwable => println(s"get devices pixels error, error info: $t")
    }
    devicePixelOptionSet
  }

  def main(args: Array[String]): Unit = {
    implicit val ec = ExecutionContext.global
    println(getAllDevicePixel())
  }
}
