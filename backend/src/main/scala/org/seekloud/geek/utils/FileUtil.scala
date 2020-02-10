package org.seekloud.geek.utils

import java.io._
import java.net.URLEncoder
import java.util.zip.{ZipEntry, ZipOutputStream}

import akka.util.ByteString
import org.seekloud.geek.common.AppSettings
import org.slf4j.LoggerFactory

/**
  * User: easego
  * Date: 2018/5/11
  * Time: 10:20
  */

object FileUtil {

  private val log = LoggerFactory.getLogger(this.getClass)

  def getFilePath(id:Long,name:String) = {
    AppSettings.pdfFilePath + "/" + getFileName(id,name)
  }

  def getFileName(id:Long,name:String) = {
    val index = name.lastIndexOf(".")
    val p = if(index < 0) "" else name.substring(index)
    id + p
  }


  def getConvertJobId(id:Long,name:String) = {
    id+"_"+URLEncoder.encode(name,"UTF-8")
  }

  def storeFile(fileInfo: String, file: File, subDir: String, basicDir: String) = {
    val fileDir = if(subDir.trim == "") {
      basicDir
    }else{
      basicDir + "/" + subDir
    }
    val filePath = fileDir  + "/" + fileInfo
    log.debug(s"fileDir: $fileDir filePath: $filePath")
    val dir = new File(fileDir)
    if(!dir.exists()) dir.mkdirs()
    val dest = new File(filePath)
    if(dest.exists()) dest.delete()

    copyFile(dest,file)
    if(file.exists()) file.delete()
  }

  def storeFile(dest: String,input:String) = {
    val dir = new File(AppSettings.convertPdfFilePath)
    if(!dir.exists()) dir.mkdirs()
    val destF = new File(dest)
    if(destF.exists()) destF.delete()
    val inputF = new File(input)
    if(inputF.exists()) copyFile(destF,inputF)
  }

  def storeFile(dest:String,input:List[ByteString]) = {
    val dir = new File(AppSettings.pdfFilePath)
    if(!dir.exists()) dir.mkdirs()
    val destF = new File(dest)
    if(destF.exists()) destF.delete()
    copyFile(destF,input)
  }



  def storeTmpFile(tmpFileDir:String,tmpFile:File,file:File)={
//    val fileDir = AppSettings.storeFilePath + "/tmp"
    println("tmptmptmptmptmp")
    val dir = new File(tmpFileDir)
    //创建目录
    //    log.debug(s"store ${file.getName} ...")
    if(!dir.exists()) dir.mkdirs()
    //    copyFile(tmpFile,file)
    var in:InputStream = null
    var out:OutputStream = null
    try{
      in = new FileInputStream(file)
      out = new FileOutputStream(tmpFile,true)
      val buffer = new Array[Byte](1024)
      var byte = in.read(buffer)
      while(byte >= 0){
        out.write(buffer,0,byte)
        byte = in.read(buffer)
      }
    }catch{
      case e:Exception =>
        log.error(s"write file ${tmpFile.getName} error",e)
    }finally {
      if(in!=null) in.close()
      if(out!=null) out.close()
    }
    if(file.exists()) file.delete()
  }

  def copyFile(dest:File,source:List[ByteString]) = {
    var out:FileOutputStream = null
    try{
      out = new FileOutputStream(dest)
      source.foreach(s => out.write(s.toArray))
    }catch{
      case e:Exception =>
        log.error(s"copy to file ${dest.getName} error",e)
    }finally {
      if(out!=null) out.close()
    }
  }

  def copyFile(dest:File,source:File) = {
    var in:InputStream = null
    var out:OutputStream = null
    try{
      in = new FileInputStream(source)
      out = new FileOutputStream(dest)
      val buffer = new Array[Byte](1024)
      var byte = in.read(buffer)
      while(byte >= 0){
        out.write(buffer,0,byte)
        byte = in.read(buffer)
      }
    }catch{

      case e:Exception =>
        log.error(s"copy file ${source.getName} error",e)
    }finally {
      if(in!=null) in.close()
      if(out!=null) out.close()
    }
  }

  def moveFile(oldName:String,newName:String,start:String,dest:String)={
    val dir = new File(dest)
    if(!dir.exists()) dir.mkdirs()
    val destFile =new File(dest+"/"+newName)
    val oldFile = new File(start+"/"+ oldName)
    if(destFile.exists()) destFile.delete()
    try{
      oldFile.renameTo(destFile)
      log.debug(s" move ${oldFile.getName} to $destFile")
    }catch {
      case e:Exception =>
        log.error(s"move to file ${destFile.getName} error",e)
    }
  }

  def deleteFile(fileName: String): Boolean = {
    val file = new File(fileName)
    if(file.exists()) {
      file.delete()
    }else{
      false
    }
  }

  def getPdfFile(name:String) = {
    println(AppSettings.pdfFilePath + "/" + name)
    val file = new File(AppSettings.pdfFilePath + "/" + name)
    file
  }

  def getPdfFile(dir: String,name: String) = {
    val file = new File(AppSettings.pdfFilePath + "/" + dir + "/" + name )
    file
  }

  def checkFile(name: String, basicDir: String) = {
    val file = new File(basicDir + "/" + name)
//    println(file.getAbsolutePath)
    if(file.exists()) {
      true
    }else{
      false
    }
  }

  def packZip(zipFile: File, fileList: List[String]): Boolean = {
    var zipStream: ZipOutputStream = null
    var zipSource: FileInputStream = null
    var bufferStream: BufferedInputStream = null
    try{
      zipStream = new ZipOutputStream(new FileOutputStream(zipFile))
      fileList.foreach { f =>
        log.info("uppic zipFile: " +f);

        val file = new File(f)
        zipSource = new FileInputStream(file)
        val bufferArea = new Array[Byte](1024 * 10)// 读写缓冲区
//        println("interupt")
//        println(s"name:${file.getName}")
        val zipEntry = new ZipEntry(file.getName())
//        println(s"aa:${file.getName()}")
        zipStream.putNextEntry(zipEntry)
        bufferStream = new BufferedInputStream(zipSource, 1024 * 10)
        var read = 0
        read = bufferStream.read(bufferArea, 0, 1024 * 10)
        while (read > 0) { try{println(s"zipStream.write");zipStream.write(bufferArea, 0, read); read = bufferStream.read(bufferArea, 0, 1024 * 10)} catch {case e : Exception =>println(s"read error:$e")}}

        log.info(s"zipStream:$zipStream")
      }
      true
    } catch {
      case e: Exception =>
        println(s"error1: $e")
        false
    } finally {
      try {
        if (null != bufferStream) bufferStream.close()
        if (null != zipStream) zipStream.close()
        if (null != zipSource) zipSource.close()
      } catch {
        case e: IOException =>
          log.error(s"打包文件报错：$e")
      }
    }


  }

//  /**
//    * 删除目录（文件夹）以及目录下的文件
//    *
//    * @param   path 被删除目录的文件路径
//    * @return 目录删除成功返回true，否则返回false
//    */
  def deleteDirectory(path: String): Boolean = {
  //如果sPath不以文件分隔符结尾，自动添加文件分隔符
    var sPath = path
    var flag = false
    var resultFlag = false
    if (!sPath.endsWith(File.separator)) {
      sPath = sPath + File.separator
    }
    val dirFile = new File(sPath)
    //如果dir对应的文件不存在，或者不是一个目录，则退出
    if (!dirFile.exists || !dirFile.isDirectory) {
      false
    } else {
      flag = true
      resultFlag = true
      //删除文件夹下的所有文件(包括子目录)
      val files = dirFile.listFiles
      var i = 0
      while (i < files.length) { //删除子文件
        if (files(i).isFile) {
          //是个文件直接删除
          flag = deleteFile(files(i).getAbsolutePath)
          if(! flag) {
            resultFlag = false
          }
        } else {
          //是个目录则进行迭代
          flag = deleteDirectory(files(i).getAbsolutePath)
          if(flag) {
            //子目录正常删完了,删除主目录
            deleteFile(files(i).getAbsolutePath) match {
              case false =>
                resultFlag = false

              case _ =>
            }
          }else{
            //子目录删除过程中出错了,做删除主目录的尝试
            deleteFile(files(i).getAbsolutePath)
            resultFlag = false
          }
        }
        i += 1
      }
      val file = new File(path)
      file.delete()
      resultFlag
    }
  }

  def main(args: Array[String]): Unit = {
    println(deleteDirectory("test"))
  }


}