package org.seekloud.geek.utils

import com.zaxxer.hikari.HikariDataSource
import org.slf4j.LoggerFactory
import slick.jdbc.H2Profile

/**
 * User: Taoz
 * Date: 2/9/2015
 * Time: 4:33 PM
 */
object DBUtil {

  val log = LoggerFactory.getLogger(this.getClass)
  private val dataSource = createDataSource()

  import org.seekloud.geek.common.AppSettings._

  private def createDataSource() = {

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

  val driver = H2Profile

  import driver.api.Database

  val db: Database = Database.forDataSource(dataSource, Some(slickMaximumPoolSize))

}