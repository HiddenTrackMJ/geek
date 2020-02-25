package org.seekloud.geek.utils

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

//    val mysqlDS = new MysqlDataSource()
//    mysqlDS.setURL(slickUrl)
//    mysqlDS.setUser(slickUser)
//    mysqlDS.setPassword(slickPassword)
//    val hikariDS = new HikariDataSource()
//    hikariDS.setDataSource(mysqlDS)
//    hikariDS.setMaximumPoolSize(slickMaximumPoolSize)
//    hikariDS.setConnectionTimeout(slickConnectTimeout)
//    hikariDS.setIdleTimeout(slickIdleTimeout)
//    hikariDS.setMaxLifetime(slickMaxLifetime)
//    hikariDS.setAutoCommit(true)
//    hikariDS

    val dataSource = new org.h2.jdbcx.JdbcDataSource
    dataSource.setURL(slickUrl)
    dataSource.setUser(slickUser)
    dataSource.setPassword(slickPassword)
    val hikariDS = new HikariDataSource()
    hikariDS.setDataSource(dataSource)
    hikariDS.setMaximumPoolSize(slickMaximumPoolSize)
    hikariDS.setConnectionTimeout(slickConnectTimeout)
    hikariDS.setIdleTimeout(slickIdleTimeout)
    hikariDS.setMaxLifetime(slickMaxLifetime)
    hikariDS.setAutoCommit(true)
    hikariDS
  }



  val driver = slick.jdbc.MySQLProfile

  val asyncExecutor : AsyncExecutor = AsyncExecutor.apply("AsyncExecutor.default",20,-1)
  val db = Database.forDataSource(dataSource, Some(slickMaximumPoolSize),asyncExecutor)




}