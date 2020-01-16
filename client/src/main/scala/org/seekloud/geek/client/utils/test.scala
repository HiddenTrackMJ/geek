package org.seekloud.geek.client.utils

import java.io.{BufferedReader, File, FileReader}

/**
  * User: Jason
  * Date: 2019/9/6
  * Time: 14:16
  */
object test {
  val file = new File(System.getProperty("user.home"))
  val os = new File(System.getProperty("os.name"))
  val name = new File(System.getProperty("user.name"))

  def main(args: Array[String]): Unit = {
    println(file)
    println(os)
    println(name)
    val fileName = "C:\\Users\\Shinobi\\theia2317605565576041426userLogin"
    val path = new File(s"$fileName")
    if(path.canRead && path.exists()){
      val bufferedReader = new BufferedReader(new FileReader(path))
      println(bufferedReader.readLine(), bufferedReader.readLine(), bufferedReader.readLine())
    }
  }
}
