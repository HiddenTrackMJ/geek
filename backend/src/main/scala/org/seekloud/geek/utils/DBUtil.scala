package org.seekloud.geek.utils

import java.net.NetworkInterface

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource
import com.zaxxer.hikari.HikariDataSource
import org.slf4j.LoggerFactory
import slick.jdbc.MySQLProfile.api._
import slick.util.AsyncExecutor

/**
 * User: Taoz
 * Date: 2/9/2015
 * Time: 4:33 PM
 */
object DBUtil {
  val log = LoggerFactory.getLogger(this.getClass)

  import org.seekloud.geek.common.AppSettings._

  private val dataSource = createDataSource()

  private def createDataSource() = {

    //hikariDS.setDataSource(DB.getDataSource())

    val mysqlDS = new MysqlDataSource()
    mysqlDS.setURL(slickUrl)
    mysqlDS.setUser(slickUser)
    mysqlDS.setPassword(slickPassword)
    val hikariDS = new HikariDataSource()
    hikariDS.setDataSource(mysqlDS)
    hikariDS.setMaximumPoolSize(slickMaximumPoolSize)
    hikariDS.setConnectionTimeout(slickConnectTimeout)
    hikariDS.setIdleTimeout(slickIdleTimeout)
    hikariDS.setMaxLifetime(slickMaxLifetime)
    hikariDS.setAutoCommit(true)
    hikariDS
  }


//  private[this] def getUrl = {
//    import collection.JavaConversions._
//    //val ip = InetAddress.getLocalHost.getHostAddress
//    val nif = NetworkInterface.getNetworkInterfaces
//    val ips = collection.mutable.Set[String]()
//    while (nif.hasMoreElements) {
//      ips ++= nif.nextElement().getInterfaceAddresses.map( _.getAddress.getHostAddress )
//    }
//    log.debug("local ips:" + ips)
//    //    if(ips.contains("182.92.149.40")) {
//    //      slickLocalUrl
//    //    } else
//    slickUrl
//  }


  val asyncExecutor : AsyncExecutor = AsyncExecutor.apply("AsyncExecutor.default",20,-1)
  val db = Database.forDataSource(dataSource, Some(slickMaximumPoolSize),asyncExecutor)




}