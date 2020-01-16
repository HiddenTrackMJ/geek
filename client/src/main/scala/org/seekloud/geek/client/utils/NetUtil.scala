package org.seekloud.geek.client.utils

import java.net.{Inet4Address, InetAddress, NetworkInterface, ServerSocket, SocketException}

/**
  * User: Arrow
  * Date: 2019/8/15
  * Time: 15:39
  */
object NetUtil {

  @throws[SocketException]
  def getLocalIpv4Address: List[String] = {
    val ifaces = NetworkInterface.getNetworkInterfaces
    var siteLocalAddress: List[String] = List.empty
    var rst: List[String] = List.empty
    while (ifaces.hasMoreElements) {
      val iface = ifaces.nextElement
      val addresses = iface.getInetAddresses
      while (addresses.hasMoreElements) {
        val addr = addresses.nextElement
        val hostAddress = addr.getHostAddress
        if (addr.isInstanceOf[Inet4Address] &&
            !addr.isLoopbackAddress
        //               && (hostAddress.startsWith("192.") || hostAddress.startsWith("10.") ) &&
        //                !hostAddress.startsWith("192.168") &&
        //                !hostAddress.startsWith("172.") &&
        //                !hostAddress.startsWith("169.")
        ) {
          if (addr.isSiteLocalAddress) {
            siteLocalAddress = siteLocalAddress :+ hostAddress
          } else {
            rst = rst :+ hostAddress
          }
        }
      }
    }
    if (rst.isEmpty) {
      if (siteLocalAddress.isEmpty) List.empty
      else siteLocalAddress
    } else rst
  }

  def getIpAddress : String = {
    var addr: InetAddress = null
    try {
      addr = InetAddress.getLocalHost
    } catch  {
      case e: Exception =>
        e.printStackTrace()
    }

    val ipAddr = addr.getAddress
    var ipAddrStr: String = ""
    for (i <- ipAddr.indices) {
      if (i > 0) {
        ipAddrStr += "."
      }
      ipAddrStr += ipAddr(i) & 0xFF
    }

   ipAddrStr
  }




  def getIp: String = {
    var ip = ""
    var host = ""
    try {
      val ia = InetAddress.getLocalHost
      host = ia.getHostName //获取计算机名字
      ip = ia.getHostAddress //获取IP
    } catch {
      case e: Exception =>
        e.printStackTrace()
    }
//    System.out.println(host)
//    System.out.println(ip)
   ip
  }

  def getFreePort: Int = {
    val serverSocket =  new ServerSocket(0) //读取空闲的可用端口
    val port = serverSocket.getLocalPort
    serverSocket.close()
    port
  }

  def main(args: Array[String]): Unit = {
    println(getLocalIpv4Address)
    println("\n" + getIpAddress)
    println(getIp)
  }

}
