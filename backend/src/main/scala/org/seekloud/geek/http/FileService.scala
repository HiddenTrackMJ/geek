package org.seekloud.geek.http

/**
  * User: easego
  * Date: 2018/10/23
  * Time: 14:59
  *
  */
import java.io._

import akka.actor.Scheduler
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive1, Route}
import akka.stream.scaladsl.{FileIO, Source}
import akka.util.{ByteString, Timeout}
import io.circe.generic.auto._
import org.slf4j.LoggerFactory

import org.seekloud.geek.common.AppSettings
import org.seekloud.geek.common.Constant._
import org.seekloud.geek.shared.ptcl.{ErrorRsp, SuccessRsp}
import org.seekloud.geek.utils.{FileUtil, HestiaClient, ZipUtil}
import org.seekloud.geek.shared.ptcl.FileProtocol._
import org.seekloud.geek.shared.ptcl.CommonErrorCode._

import scala.util.{Failure, Success}

trait FileService extends SessionBase with BaseService {

  //  implicit val executor: ExecutionContextExecutor
  implicit val timeout: Timeout
  implicit val scheduler: Scheduler

  private val log = LoggerFactory.getLogger(this.getClass)

  private def storeFile(source: Source[ByteString, Any]): Directive1[java.io.File] = {
    val dest = java.io.File.createTempFile("leaf", ".tmp")
    val file = source.runWith(FileIO.toPath(dest.toPath)).map(_ => dest)
    onComplete[java.io.File](file).flatMap {
      case Success(f) =>
        provide(f)
      case Failure(e) =>
        dest.deleteOnExit()
        failWith(e)
    }
  }

  private def uploadFile = (path("uploadFile") & post) {
    //    refererCheck(geekIdentity.localManager){ _ =>
      parameters('targetDir.as[String], 'fileType.as[Int]) {
        (targetDir, fileType) =>
          if (fileTypeMap.keys.toList.contains(fileType)) {
            if (fileType == 3) {
              //tour文件
              val basicDir = fileTypeMap(fileType)
              //释放大小限制
              withoutSizeLimit {
                fileUpload("fileUpload") {
                  case (fileInfo, file) =>
                    val fileName = fileInfo.fileName.split("[.]").reverse.tail.reverse.mkString(".")
                    if (FileUtil.checkFile(fileName, basicDir)) {
                      log.warn(s"file with same name $fileName exist")
                      complete(fileWithSameNameExistError)
                    } else {
                      storeFile(file) { f =>
                        FileUtil.storeFile(fileInfo.fileName, f, targetDir, basicDir)
                        f.deleteOnExit()
                        log.debug(s"${fileInfo.fileName} + ${fileInfo.fieldName} + ${fileInfo.contentType} upload success ")
                        if (FileUtil.checkFile(fileInfo.fileName, basicDir)) {
                          ZipUtil.upzipFile(basicDir + "/" + fileInfo.fileName, basicDir) match {
                            case Right(rsp) =>
                              FileUtil.deleteFile(basicDir + "/" + fileInfo.fileName)
                              complete(SuccessRsp())

                            case Left(e) =>
                              FileUtil.deleteFile(basicDir + "/" + fileInfo.fileName)
                              complete(uploadTourFileError(s"upload tour file error: $e"))
                          }
                        } else {
                          complete(uploadTourFileError(s"upload tour file error"))
                        }
                      }
                    }
                }
              }
            } else if (fileType == 2) {
              fileUpload("fileUpload") {
                case (fileInfo, file) =>
                  storeFile(file) { f =>
                    dealFutureResult {
                      HestiaClient.upload(f, fileInfo.fileName).map {
                        case Right(url) =>
                          f.deleteOnExit()
                          log.debug(s"${fileInfo.fileName} + ${fileInfo.fieldName} + ${fileInfo.contentType} upload success ")
                          complete(uploadSuccessRsp(url, fileInfo.fileName))

                        case Left(error) =>
                          f.deleteOnExit()
                          log.debug(s"upload error111")
                          complete(ErrorRsp(100100, "upload error!!"))
                      }
                    }
                  }
              }
            }
            else {//filetype为1
              val basicDir = fileTypeMap(fileType)
              fileUpload("fileUpload") {
                case (fileInfo, file) =>
                  storeFile(file) { f =>
                    FileUtil.storeFile(fileInfo.fileName, f, targetDir, basicDir) //targetDir是前端传来的path(跟在basicpath后)，basicDir是APPSETTING里的
                    f.deleteOnExit()
                    log.debug(s"${fileInfo.fileName} + ${fileInfo.fieldName} + ${fileInfo.contentType} upload success ")
                    complete(SuccessRsp())
                  }
              }
            }
          } else {
            complete(fileTypeNotSupportError)
          }
      }
  }

  private def uploadSliceFile = (path("uploadSliceFile") & post) {
    parameter('targetDir.as[String], 'fileType.as[Int], 'fileName.as[String], 'size.as[Long], 'sliceSize.as[Long], 'end.as[Double], 'pointer.as[Int]) {
      (targetDir, fileType, fileName, size, sliceSize, end, pointer) => //0正常，1取消
        //          val time = size / 100
        //          implicit val timeout = Timeout(time.seconds)
        fileUpload("fileUpload") {
          case (_, file) =>
            storeFile(file) { f =>
              val tmpFileDir = AppSettings.videoFilePath + "/tmp"
              val filePath = tmpFileDir + "/" + fileName
              val tmpFile = new File(filePath)
              if (pointer != -1) { //非最后一个分片情况
                if (tmpFile.exists()) {
                  //保证文件完整
                  FileUtil.storeTmpFile(tmpFileDir, tmpFile, f)
                  f.deleteOnExit()
                  complete(SuccessRsp(1)) //后续非最后一个分片上传成功
                } else {
                  if (pointer != 1) {
                    //已有文件丢失，所传文件不是第一段，重新上传
                    f.deleteOnExit()
                    complete(ErrorRsp(10000, "部分文件丢失，请重新上传"))
                  } else {
                    FileUtil.storeTmpFile(tmpFileDir, tmpFile, f)
                    f.deleteOnExit()
                    complete(SuccessRsp(2)) //第一个分片上传成功
                  }
                }
              } else { //最后一个分片
                if (tmpFile.exists()) {
                  FileUtil.storeTmpFile(tmpFileDir, tmpFile, f)
                  f.deleteOnExit()
//                  println(s"!!!filename${fileName}")
                  FileUtil.moveFile(fileName, fileName, tmpFileDir, fileTypeMap(fileType) + "/" + targetDir)
                  log.info(s"upload the last slice $pointer")
                  complete(SuccessRsp(3, "上传成功")) //文件上传完毕
                } else {
                  if (size * 1024 <= sliceSize) {
                    //文件仅为一片，且小于分片大小
                    FileUtil.storeTmpFile(tmpFileDir, tmpFile, f)
                    f.deleteOnExit()
                    FileUtil.moveFile(fileName, fileName, tmpFileDir, fileTypeMap(fileType) + "/" + targetDir)
                    complete(SuccessRsp(3, "上传成功")) //文件上传完毕
                  }
                  else {
                    complete(ErrorRsp(100000, "error"))
                  }
                }
              }
            }
        }
    }
  }

  private def deleteFile = (path("deleteFile") & get & pathEndOrSingleSlash) {
    //    refererCheck(geekIdentity.localManager){ _ =>
    authManager { _ =>
      parameters('targetName.as[String], 'fileType.as[Int]) {
        (targetName, fileType) =>
          if (fileTypeMap.keys.toList.contains(fileType)) {
            val basicDir = fileTypeMap(fileType)
            val completeName = basicDir + "/" + targetName
            //            println(s"comp:$completeName")
            val file = new File(completeName)
            if (file.isDirectory()) {
              FileUtil.deleteDirectory(completeName) match {
                case true =>
                  complete(SuccessRsp())

                case false =>
                  complete(deleteFileError("delete directory error"))
              }
            } else {
              FileUtil.deleteFile(completeName) match {
                case true =>
                  complete(SuccessRsp())

                case false =>
                  complete(deleteFileError("delete file error"))
              }
            }
          } else {
            complete(fileTypeNotSupportError)
          }
      }
    }

  }

  private def deleteTmpFile = (path("deleteTmpFile") & get & pathEndOrSingleSlash) {
    authManager { _ =>
      parameters('targetDir.as[String], 'fileType.as[Int], 'fileName.as[String]) {
        (targetDir, fileType, fileName) =>
          val basicDir = fileTypeMap(fileType) + "/tmp"
          val completeName = basicDir + "/" + fileName
          println(s"$completeName")
          FileUtil.deleteFile(completeName)
          complete(ErrorRsp(12345, "传输中断"))
      }
    }

  }


  private def download = (path("down" / Segments(2)) & get & pathEndOrSingleSlash) {
    case dir :: name :: Nil =>
      log.debug(s"dir: $dir --- name: $name")
      val f = FileUtil.getPdfFile(dir, name)
      log.debug("fileName: " + f.getName)
      if (f.exists()) {
        val responseEntity = HttpEntity(
          ContentTypes.`application/octet-stream`,
          f.length,
          FileIO.fromPath(f.toPath, chunkSize = 262144))
        complete(responseEntity)
      } else {
        complete(fileNotExistError)
      }

    case other =>
      log.debug(s"view segment failed $other")
      reject
  }

  //
  private def checkPdf = (path("checkPdf") & get & pathEndOrSingleSlash) {
    authManager { _ =>
      parameters('name.as[String]) {
        case name =>
          val basicDir = fileTypeMap(1)
          FileUtil.checkFile(name, basicDir) match {
            case true =>
              complete(SuccessRsp())

            case false =>
              complete(fileNotExistError)
          }
      }
    }
  }


  val fileRoute: Route = pathPrefix("file") {
    log.debug("in the stage of file")
    download ~ uploadFile ~ deleteFile ~ checkPdf ~ uploadSliceFile ~ deleteTmpFile
  }

}