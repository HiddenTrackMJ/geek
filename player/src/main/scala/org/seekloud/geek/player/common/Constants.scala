package org.seekloud.geek.player.common

import java.io.File

/**
  * Author: xgy
  * Date: 2020/1/17
  * Time: 1:13
  */
object Constants {

  val cachePath: String = s"${System.getProperty("user.home")}/.geek/pcClient"
  val cacheFile = new File(cachePath)

  if (!cacheFile.exists()) cacheFile.mkdirs()

  val mp4Path: String = System.getProperty("user.home") + "\\.geek\\pcClient\\mp4"

  val mp4 = new File(mp4Path)
  if (!mp4.exists()) mp4.mkdirs()







}
