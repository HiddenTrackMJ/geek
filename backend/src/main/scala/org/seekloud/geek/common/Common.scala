package org.seekloud.geek.common

/**
 * Author: Jason
 * Date: 2020/2/8
 * Time: 17:28
 */
object Common {

  object Role{
    val host = 0
    val audience = 1
  }

  object Subscriber{
    val join = 1
    val left = 0
    val change = 2
  }

}
