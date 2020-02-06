package org.seekloud.geek.models.dao

import org.seekloud.geek.models.SlickTables._
import org.seekloud.geek.utils.DBUtil.db
import org.seekloud.geek.utils.DBUtil.driver.api._

import scala.concurrent.Future

/**
 * Author: Jason
 * Date: 2020/2/6
 * Time: 13:00
 */
object VideoDao {

  def getAllVideo: Future[Seq[tVideo#TableElementType]] = {
    db.run{
      tVideo.result
    }
  }

  def getSecVideo(userId: Long): Future[Seq[tVideo#TableElementType]] = {
    db.run{
      tVideo.filter(_.userid === userId).result
    }
  }

  def addVideo(video: rVideo): Future[Long] = {
    db.run(
      tVideo.returning(tVideo.map(_.id)) += video
    )
  }

  def deleteVideo(id: Long): Future[Int] =
    db.run{
      tVideo.filter(_.id === id).delete
    }

  //邀请相关
  def getInviter(id: Long) =
    db.run{
      tVideo.filter(_.invitation === id).result
    }

  def getInvitee(id: Long) =
    db.run{
      tVideo.filter(_.userid === id).result
    }

//  def getInvitee2(id: Long) =
//    db.run{
//      tVideo.filter(_.invitation === id).result
//    }

  def delInvitee(inviterId: Long,inviteeId :Long) =
    db.run{
      tVideo.filter(_.userid === inviterId).filter(_.invitation === inviteeId).delete
    }

}
