package org.seekloud.geek.models.dao

import scala.concurrent.Future
import org.seekloud.geek.utils.DBUtil.driver.api._
import org.seekloud.geek.utils.DBUtil.db
import org.seekloud.geek.models.SlickTables._
/**
 * Author: Jason
 * Date: 2020/2/5
 * Time: 16:33
 */
object RoomDao {

  def getAllRoom: Future[Seq[tRoom#TableElementType]] = {
    db.run{
      tRoom.result
    }
  }

  def addRoom(room: rRoom): Future[Long] = {
    db.run(
      tRoom.returning(tRoom.map(_.id)) += room
    )
  }

  def deleteRoom(id: Long): Future[Int] =
    db.run{
      tRoom.filter(_.id === id).delete
    }

  def updateRoom(id: Long, roomName: String, roomDes: String): Future[Int] = {
    val action = tRoom.filter(_.id === id).map(i => (i.title, i.desc)).update((roomName, Some(roomDes)))
    db.run(action)
  }

  def modifyRoom(room: rRoom): Future[Int] = {
    val action = tRoom.filter(_.id === room.id).update(room)
    db.run(action)
  }

  def updateUserCodeMap(id: Long, userCodeMap: String): Future[Int] = {
    val action = tRoom.filter(_.id === id).map(i => (i.livecode)).update((userCodeMap))
    db.run(action)
  }

}
