package org.seekloud.geek.client.common

import org.seekloud.geek.client.common.AppSettings._
/**
 * User: hewro
 * Date: 2020/2/1
 * Time: 21:40
 * Description:
 */
object Routes {


  /*roomManager*/
  val baseUrl: String = rmProtocol + "://" + rmDomain + "/" + rmUrl
  //  val baseUrl = rmProtocol + "://" + rmHostName + ":" +  rmPort + "/" + rmUrl


  val userUrl: String = baseUrl + "/user"
  val signUp: String = userUrl + "/signUp"
  val signIn: String = userUrl + "/signIn"



}
