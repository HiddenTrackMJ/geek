package org.seekloud.geek.utils

object RegEx {

  def checkAge(str: String): Boolean = {
    val agePattern = """^([1-9]|[1-9][0-9]|[1][1-9][1-9])$""".r
    val r = agePattern.findAllMatchIn(str).toList
    //    println(r)
    if(r.nonEmpty)
      false
    else
      true
  }

}
