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

  val searchRoom: String = userUrl + "/searchRoom"
  val temporaryUser: String = userUrl + "/temporaryUser"


  val recordUrl: String = baseUrl + "/record"
  //  val getRecordList: String = recordUrl + "/getRecordList"
  val searchRecord: String = recordUrl + "/searchRecord"

  val roomUrl: String = baseUrl + "/room"
  val getRoomInfo: String = roomUrl + "/getRoomInfo"
  val createRoom: String = roomUrl + "/createRoom"
  val startLive: String = roomUrl + "/startLive"
  val startLive4Client: String = roomUrl + "/startLive4Client"
  val stopLive: String = roomUrl + "/stopLive"
  val stopLive4Client: String = roomUrl + "/stopLive4Client"
  val joinRoom: String = roomUrl + "/joinRoom"
  val getRoomList: String = roomUrl + "/getRoomList"
}
