package org.seekloud.geek.models
// AUTO-GENERATED Slick data model
/** Stand-alone Slick data model for immediate use */
object SlickTables extends {
  val profile = slick.jdbc.H2Profile
} with SlickTables

/** Slick data model trait for extension, choice of backend or usage in the cake pattern. (Make sure to initialize this late.) */
trait SlickTables {
  val profile: slick.jdbc.JdbcProfile
  import profile.api._
  import slick.model.ForeignKeyAction
  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.
  import slick.jdbc.{GetResult => GR}

  /** DDL for all tables. Call .create to execute. */
  lazy val schema: profile.SchemaDescription = tUser.schema
  @deprecated("Use .schema instead of .ddl", "3.0")
  def ddl = schema

  /** Entity class storing rows of table tUser
   *  @param id Database column ID SqlType(BIGINT), AutoInc, PrimaryKey
   *  @param name Database column NAME SqlType(VARCHAR), Length(100,true)
   *  @param password Database column PASSWORD SqlType(VARCHAR), Length(300,true)
   *  @param avatar Database column AVATAR SqlType(VARCHAR), Length(300,true), Default(None)
   *  @param roomid Database column ROOMID SqlType(BIGINT), Default(None) */
  case class rUser(id: Long, name: String, password: String, avatar: Option[String] = None, roomid: Option[Long] = None)
  /** GetResult implicit for fetching rUser objects using plain SQL queries */
  implicit def GetResultrUser(implicit e0: GR[Long], e1: GR[String], e2: GR[Option[String]], e3: GR[Option[Long]]): GR[rUser] = GR{
    prs => import prs._
      rUser.tupled((<<[Long], <<[String], <<[String], <<?[String], <<?[Long]))
  }
  /** Table description of table USER. Objects of this class serve as prototypes for rows in queries. */
  class tUser(_tableTag: Tag) extends profile.api.Table[rUser](_tableTag, Some("GEEK"), "USER") {
    def * = (id, name, password, avatar, roomid) <> (rUser.tupled, rUser.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(name), Rep.Some(password), avatar, roomid)).shaped.<>({r=>import r._; _1.map(_=> rUser.tupled((_1.get, _2.get, _3.get, _4, _5)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column ID SqlType(BIGINT), AutoInc, PrimaryKey */
    val id: Rep[Long] = column[Long]("ID", O.AutoInc, O.PrimaryKey)
    /** Database column NAME SqlType(VARCHAR), Length(100,true) */
    val name: Rep[String] = column[String]("NAME", O.Length(100,varying=true))
    /** Database column PASSWORD SqlType(VARCHAR), Length(300,true) */
    val password: Rep[String] = column[String]("PASSWORD", O.Length(300,varying=true))
    /** Database column AVATAR SqlType(VARCHAR), Length(300,true), Default(None) */
    val avatar: Rep[Option[String]] = column[Option[String]]("AVATAR", O.Length(300,varying=true), O.Default(None))
    /** Database column ROOMID SqlType(BIGINT), Default(None) */
    val roomid: Rep[Option[Long]] = column[Option[Long]]("ROOMID", O.Default(None))
  }
  /** Collection-like TableQuery object for table tUser */
  lazy val tUser = new TableQuery(tag => new tUser(tag))
}
