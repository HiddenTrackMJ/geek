package org.seekloud.geek.utils

/**
  * User: easego
  * Date: 2019/1/9
  * Time: 16:40
  *
  *
  * copy from: https://www.cnblogs.com/DreamDrive/p/5760477.html
  *
  * 压缩或解压zip;
  * 由于直接使用java.util.zip工具包下的类,会出现中文乱码问题,
  * 所以使用ant.jar中的org.apache.tools.zip下的工具类
  */

import org.apache.tools.zip.{ZipEntry, ZipFile, ZipOutputStream}
import java.io.{File, FileInputStream, FileOutputStream, IOException}

object ZipUtil {
  private val _byte = new Array[Byte](1024)

  /**
    * 压缩文件或路径
    *
    * @param zip      压缩的目的地址
    * @param srcFiles 压缩的源文件
    */
  def zipFile(zip: String, srcFiles: List[File]): Unit = {
    try
        if (zip.endsWith(".zip") || zip.endsWith(".ZIP")) {
          val _zipOut = new ZipOutputStream(new FileOutputStream(new File(zip)))
          _zipOut.setEncoding("GBK")
          srcFiles.foreach{ _f =>
            handlerFile(zip,_zipOut,_f,"")
          }
//          for (_f <- srcFiles) {
//            handlerFile(zip, _zipOut, _f, "")
//          }
          _zipOut.close()
        } else {
          println("target file[" + zip + "] is not .zip type file")
        }
    catch {
      case e: Exception =>
        println(s"${e.getMessage}")

    }
  }

  /**
    *
    * @param zip     压缩的目的地址
    * @param zipOut
    * @param srcFile 被压缩的文件信息
    * @param pathT    在zip中的相对路径
    * @throws IOException
    */
  @throws[IOException]
  private def handlerFile(zip: String, zipOut: ZipOutputStream, srcFile: File, pathT: String): Unit = {
    println(" begin to compression file[" + srcFile.getName + "]")
    var path = pathT
    if (!("" == path) && !path.endsWith(File.separator)) {
      path += File.separator
    }
    if (!(srcFile.getPath == zip)) {
      if (srcFile.isDirectory) {
        val _files = srcFile.listFiles
        if (_files.isEmpty) {
          zipOut.putNextEntry(new ZipEntry(path + srcFile.getName + File.separator))
          zipOut.closeEntry()
        }
        else for (_f <- _files) {
          handlerFile(zip, zipOut, _f, path + srcFile.getName)
        }
      } else {
        val _in = new FileInputStream(srcFile)
        zipOut.putNextEntry(new ZipEntry(path + srcFile.getName))
        var len = 0
        len = _in.read(_byte)
        while ( len > 0) {
          zipOut.write(_byte, 0, len)
          len = _in.read(_byte)
        }
        _in.close()
        zipOut.closeEntry()
      }
    }
  }

  /**
    * 解压缩ZIP文件，将ZIP文件里的内容解压到targetDIR目录下
    *
    * @param zipPath  待解压缩的ZIP文件路径
    * @param descDir  目标目录
    */
  def upzipFile(zipPath: String, descDir: String): Either[String,List[File]] = upzipFile(new File(zipPath), descDir)

  /**
    * 对.zip文件进行解压缩
    *
    * @param zipFile 解压缩文件
    * @param descDir 压缩的目标地址，如：D:\\测试 或 /mnt/d/测试
    * @return
    */
  @SuppressWarnings(Array("rawtypes"))
  def upzipFile(zipFile: File, descDir: String): Either[String,List[File]] = {

      try {
        val _list = List.empty[File]
        val _zipFile = new ZipFile(zipFile, "GBK")
        val entries = _zipFile.getEntries
        while ( {
          entries.hasMoreElements
        }) {
          val entry = entries.nextElement.asInstanceOf[ZipEntry]
          val _file = new File(descDir + File.separator + entry.getName)
          if (entry.isDirectory) _file.mkdirs
          else {
            val _parent = _file.getParentFile
            if (!_parent.exists) _parent.mkdirs
            val _in = _zipFile.getInputStream(entry)
            val _out = new FileOutputStream(_file)
            var len = 0
            len = _in.read(_byte)
            while (len > 0) {
              _out.write(_byte, 0, len)
              len = _in.read(_byte)
            }
            _in.close()
            _out.flush()
            _out.close()
            _list :+ _file
          }
        }
        Right(_list)
      } catch {
        case e: Exception =>
          Left(e.getMessage)
      }
  }

  /**
    * 对临时生成的文件夹和文件夹下的文件进行删除
    */
  def deletefile(delpath: String): Unit = {
    try {
      val file = new File(delpath)
      if (!file.isDirectory) file.delete
      else if (file.isDirectory) {
        val filelist = file.list
        var i = 0
        while ( {
          i < filelist.length
        }) {
          val delfile = new File(delpath + File.separator + filelist(i))
          if (!delfile.isDirectory) delfile.delete
          else if (delfile.isDirectory) deletefile(delpath + File.separator + filelist(i))

          {
            i += 1; i - 1
          }
        }
        file.delete
      }
    } catch {
      case e: Exception =>
        e.printStackTrace()
    }
  }

  def main(args: Array[String]): Unit = {

    upzipFile("1.zip","test/d")

  }
}