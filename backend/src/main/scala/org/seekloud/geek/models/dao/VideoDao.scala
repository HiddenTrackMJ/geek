package org.seekloud.geek.models.dao

import org.seekloud.geek.models.SlickTables._
import org.seekloud.geek.utils.DBUtil.db
import org.seekloud.geek.utils.DBUtil.driver.api._
import org.seekloud.geek.Boot.executor
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

  def getSecVideo2(inviteeId: Long): Future[Seq[tVideo#TableElementType]] = {
    db.run{
      tVideo.filter(_.invitation === inviteeId).result
    }
  }

  def getSecVideo(inviteeId: Long) = {
    val q = tUser join tVideo.filter(_.invitation === inviteeId) on{
      (t1,t2)=>
        List(t1.id === t2.userid).reduceLeft(_ || _)
    }
//    val innerJoin = for {
//      (inviterName, inviterId) <- q
//    } yield (inviterName,inviterId)
    db.run(q.distinct.sortBy(_._1.id).sortBy(_._2.roomid).result)
  }

  def getInviteeVideo(inviteeId: Long,filename: String): Future[Seq[tVideo#TableElementType]] = {
    db.run{
      tVideo.filter(_.invitation === inviteeId).filter(_.filename === filename).result
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
//  def getInviter(id: Long) ={
//      val action = for{
//      inviteeName <- tVideo.filter(_.invitation === id).map(_.userid).result
//      inviteeId <- tUser.filter(_.invitation === id).map(_.userid).result
//      }
//        yield (inviteeName,inviteeId)
//    db.run{action.transactionally}
//  }

  def getInviter(id:Long) = {
    val q = tUser join tVideo.filter(_.invitation === id) on{
      (t1,t2)=>
        List(t1.id === t2.userid).reduceLeft(_ || _)
    }
    val innerJoin = for {
      (inviterName, inviterId) <- q
    } yield (inviterName,inviterId)
    db.run(innerJoin.distinct.sortBy(_._1.id).result)
  }

  def getInvitee(id:Long) = {
    val q = tUser join tVideo.filter(_.userid === id) on{
      (t1,t2)=>
        List(t1.id === t2.invitation).reduceLeft(_ || _)
    }
    val innerJoin = for {
      (inviteeName, inviteeId) <- q
    } yield (inviteeName,inviteeId)
    db.run(innerJoin.distinct.sortBy(_._1.id).result)
  }


//  def getInvitee(id: Long) =
//    db.run{
//      tVideo.filter(_.userid === id).result
//    }

//  def getInvitee2(id: Long) =
//    db.run{
//      tVideo.filter(_.invitation === id).result
//    }

  def delInvitee(inviterId: Long,inviteeId :Long) =
    db.run{
      tVideo.filter(_.userid === inviterId).filter(_.invitation === inviteeId).delete
    }

}
