package org.seekloud.geek.shared.ptcl

/**
  * User: Jason
  * Date: 2019/5/23
  * Time: 11:59
  */
object Protocol {

  case class RoomData(
    playerId: String,
    nickname: String,
    url: String,
    roomId: Long
  )

  case class OutTarget(
    dir: String,
    file: String
  )

}
