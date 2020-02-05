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
  lazy val schema: profile.SchemaDescription = tRoom.schema ++ tUser.schema
  @deprecated("Use .schema instead of .ddl", "3.0")
  def ddl = schema

  /** Entity class storing rows of table tRoom
   *  @param id Database column ID SqlType(BIGINT), PrimaryKey
   *  @param title Database column TITLE SqlType(VARCHAR), Length(200,true)
   *  @param desc Database column DESC SqlType(VARCHAR), Length(200,true), Default(None)
   *  @param livecode Database column LIVECODE SqlType(VARCHAR), Length(1000,true)
   *  @param hostcode Database column HOSTCODE SqlType(VARCHAR), Length(200,true)
   *  @param serverurl Database column SERVERURL SqlType(VARCHAR), Length(200,true)
   *  @param hostid Database column HOSTID SqlType(BIGINT) */
  case class rRoom(id: Long, title: String, desc: Option[String] = None, livecode: String, hostcode: String, serverurl: String, hostid: Long)
  /** GetResult implicit for fetching rRoom objects using plain SQL queries */
  implicit def GetResultrRoom(implicit e0: GR[Long], e1: GR[String], e2: GR[Option[String]]): GR[rRoom] = GR{
    prs => import prs._
      rRoom.tupled((<<[Long], <<[String], <<?[String], <<[String], <<[String], <<[String], <<[Long]))
  }
  /** Table description of table ROOM. Objects of this class serve as prototypes for rows in queries. */
  class tRoom(_tableTag: Tag) extends profile.api.Table[rRoom](_tableTag, Some("GEEK"), "ROOM") {
    def * = (id, title, desc, livecode, hostcode, serverurl, hostid) <> (rRoom.tupled, rRoom.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(title), desc, Rep.Some(livecode), Rep.Some(hostcode), Rep.Some(serverurl), Rep.Some(hostid))).shaped.<>({r=>import r._; _1.map(_=> rRoom.tupled((_1.get, _2.get, _3, _4.get, _5.get, _6.get, _7.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column ID SqlType(BIGINT), PrimaryKey */
    val id: Rep[Long] = column[Long]("ID", O.PrimaryKey)
    /** Database column TITLE SqlType(VARCHAR), Length(200,true) */
    val title: Rep[String] = column[String]("TITLE", O.Length(200,varying=true))
    /** Database column DESC SqlType(VARCHAR), Length(200,true), Default(None) */
    val desc: Rep[Option[String]] = column[Option[String]]("DESC", O.Length(200,varying=true), O.Default(None))
    /** Database column LIVECODE SqlType(VARCHAR), Length(1000,true) */
    val livecode: Rep[String] = column[String]("LIVECODE", O.Length(1000,varying=true))
    /** Database column HOSTCODE SqlType(VARCHAR), Length(200,true) */
    val hostcode: Rep[String] = column[String]("HOSTCODE", O.Length(200,varying=true))
    /** Database column SERVERURL SqlType(VARCHAR), Length(200,true) */
    val serverurl: Rep[String] = column[String]("SERVERURL", O.Length(200,varying=true))
    /** Database column HOSTID SqlType(BIGINT) */
    val hostid: Rep[Long] = column[Long]("HOSTID")
  }
  /** Collection-like TableQuery object for table tRoom */
  lazy val tRoom = new TableQuery(tag => new tRoom(tag))

  /** Entity class storing rows of table tUser
   *  @param id Database column ID SqlType(BIGINT), PrimaryKey
   *  @param name Database column NAME SqlType(VARCHAR), Length(100,true)
   *  @param password Database column PASSWORD SqlType(VARCHAR), Length(300,true)
   *  @param avatar Database column AVATAR SqlType(VARCHAR), Length(300,true), Default(None) */
  case class rUser(id: Long, name: String, password: String, avatar: Option[String] = None)
  /** GetResult implicit for fetching rUser objects using plain SQL queries */
  implicit def GetResultrUser(implicit e0: GR[Long], e1: GR[String], e2: GR[Option[String]]): GR[rUser] = GR{
    prs => import prs._
      rUser.tupled((<<[Long], <<[String], <<[String], <<?[String]))
  }
  /** Table description of table USER. Objects of this class serve as prototypes for rows in queries. */
  class tUser(_tableTag: Tag) extends profile.api.Table[rUser](_tableTag, Some("GEEK"), "USER") {
    def * = (id, name, password, avatar) <> (rUser.tupled, rUser.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(name), Rep.Some(password), avatar)).shaped.<>({r=>import r._; _1.map(_=> rUser.tupled((_1.get, _2.get, _3.get, _4)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column ID SqlType(BIGINT), PrimaryKey */
    val id: Rep[Long] = column[Long]("ID", O.PrimaryKey)
    /** Database column NAME SqlType(VARCHAR), Length(100,true) */
    val name: Rep[String] = column[String]("NAME", O.Length(100,varying=true))
    /** Database column PASSWORD SqlType(VARCHAR), Length(300,true) */
    val password: Rep[String] = column[String]("PASSWORD", O.Length(300,varying=true))
    /** Database column AVATAR SqlType(VARCHAR), Length(300,true), Default(None) */
    val avatar: Rep[Option[String]] = column[Option[String]]("AVATAR", O.Length(300,varying=true), O.Default(None))
  }
  /** Collection-like TableQuery object for table tUser */
  lazy val tUser = new TableQuery(tag => new tUser(tag))
}
