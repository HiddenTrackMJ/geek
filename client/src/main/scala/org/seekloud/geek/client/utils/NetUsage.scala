package org.seekloud.geek.client.utils

import oshi.SystemInfo
import oshi.software.os.OperatingSystem.ProcessSort
import oshi.util.FormatUtil

/**
  * User: Jason
  * Date: 2019/9/27
  * Time: 15:22
  */
object NetUsage {

  case class CPUMemInfo(memPer: Double, memByte: String, proName: String)
  private val si = new SystemInfo
  def getCPUMemInfo : Option[CPUMemInfo]= {
    val hal = si.getHardware
    val memory = hal.getMemory
    val os = si.getOperatingSystem
    val process = os.getProcesses(10, ProcessSort.MEMORY).toList
    process.find(_.getProcessID == os.getProcessId).map(i => CPUMemInfo(100d * i.getResidentSetSize / memory.getTotal, FormatUtil.formatBytes(i.getResidentSetSize), i.getName))
  }
}
