package org.seekloud.geek.models
// AUTO-GENERATED Slick data model
/** Stand-alone Slick data model for immediate use */
object SlickTables extends {
  val profile = slick.jdbc.MySQLProfile
} with SlickTables

/** Slick data model trait for extension, choice of backend or usage in the cake pattern. (Make sure to initialize this late.) */
trait SlickTables {
  val profile: slick.jdbc.JdbcProfile
  import profile.api._
  import slick.model.ForeignKeyAction
  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.
  import slick.jdbc.{GetResult => GR}

  /** DDL for all tables. Call .create to execute. */
  lazy val schema: profile.SchemaDescription = tRoom.schema ++ tUser.schema ++ tVideo.schema ++ tVideoTest.schema
  @deprecated("Use .schema instead of .ddl", "3.0")
  def ddl = schema

  /** Entity class storing rows of table tRoom
    *  @param id Database column ID SqlType(BIGINT), AutoInc, PrimaryKey
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
  /** Table description of table room. Objects of this class serve as prototypes for rows in queries. */
  class tRoom(_tableTag: Tag) extends profile.api.Table[rRoom](_tableTag, Some("geek"), "room") {
    def * = (id, title, desc, livecode, hostcode, serverurl, hostid) <> (rRoom.tupled, rRoom.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(title), desc, Rep.Some(livecode), Rep.Some(hostcode), Rep.Some(serverurl), Rep.Some(hostid))).shaped.<>({r=>import r._; _1.map(_=> rRoom.tupled((_1.get, _2.get, _3, _4.get, _5.get, _6.get, _7.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column ID SqlType(BIGINT), AutoInc, PrimaryKey */
    val id: Rep[Long] = column[Long]("ID", O.AutoInc, O.PrimaryKey)
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
    *  @param id Database column ID SqlType(BIGINT), AutoInc, PrimaryKey
    *  @param name Database column NAME SqlType(VARCHAR), Length(100,true)
    *  @param password Database column PASSWORD SqlType(VARCHAR), Length(300,true)
    *  @param avatar Database column AVATAR SqlType(VARCHAR), Length(300,true), Default(None)
    *  @param gender Database column GENDER SqlType(INT), Default(None)
    *  @param age Database column AGE SqlType(INT), Default(None)
    *  @param address Database column ADDRESS SqlType(VARCHAR), Length(100,true), Default(None) */
  case class rUser(id: Long, name: String, password: String, avatar: Option[String] = None, gender: Option[Int] = None, age: Option[Int] = None, address: Option[String] = None)
  /** GetResult implicit for fetching rUser objects using plain SQL queries */
  implicit def GetResultrUser(implicit e0: GR[Long], e1: GR[String], e2: GR[Option[String]], e3: GR[Option[Int]]): GR[rUser] = GR{
    prs => import prs._
      rUser.tupled((<<[Long], <<[String], <<[String], <<?[String], <<?[Int], <<?[Int], <<?[String]))
  }
  /** Table description of table user. Objects of this class serve as prototypes for rows in queries. */
  class tUser(_tableTag: Tag) extends profile.api.Table[rUser](_tableTag, Some("geek"), "user") {
    def * = (id, name, password, avatar, gender, age, address) <> (rUser.tupled, rUser.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(name), Rep.Some(password), avatar, gender, age, address)).shaped.<>({r=>import r._; _1.map(_=> rUser.tupled((_1.get, _2.get, _3.get, _4, _5, _6, _7)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column ID SqlType(BIGINT), AutoInc, PrimaryKey */
    val id: Rep[Long] = column[Long]("ID", O.AutoInc, O.PrimaryKey)
    /** Database column NAME SqlType(VARCHAR), Length(100,true) */
    val name: Rep[String] = column[String]("NAME", O.Length(100,varying=true))
    /** Database column PASSWORD SqlType(VARCHAR), Length(300,true) */
    val password: Rep[String] = column[String]("PASSWORD", O.Length(300,varying=true))
    /** Database column AVATAR SqlType(VARCHAR), Length(300,true), Default(None) */
    val avatar: Rep[Option[String]] = column[Option[String]]("AVATAR", O.Length(300,varying=true), O.Default(None))
    /** Database column GENDER SqlType(INT), Default(None) */
    val gender: Rep[Option[Int]] = column[Option[Int]]("GENDER", O.Default(None))
    /** Database column AGE SqlType(INT), Default(None) */
    val age: Rep[Option[Int]] = column[Option[Int]]("AGE", O.Default(None))
    /** Database column ADDRESS SqlType(VARCHAR), Length(100,true), Default(None) */
    val address: Rep[Option[String]] = column[Option[String]]("ADDRESS", O.Length(100,varying=true), O.Default(None))
  }
  /** Collection-like TableQuery object for table tUser */
  lazy val tUser = new TableQuery(tag => new tUser(tag))

  /** Entity class storing rows of table tVideo
    *  @param id Database column ID SqlType(BIGINT), AutoInc, PrimaryKey
    *  @param userid Database column USERID SqlType(BIGINT)
    *  @param roomid Database column ROOMID SqlType(BIGINT)
    *  @param timestamp Database column TIMESTAMP SqlType(BIGINT)
    *  @param filename Database column FILENAME SqlType(VARCHAR), Length(300,true)
    *  @param length Database column LENGTH SqlType(VARCHAR), Length(100,true)
    *  @param invitation Database column INVITATION SqlType(BIGINT)
    *  @param comment Database column COMMENT SqlType(VARCHAR), Length(500,true) */
  case class rVideo(id: Long, userid: Long, roomid: Long, timestamp: Long, filename: String, length: String, invitation: Long, comment: String)
  /** GetResult implicit for fetching rVideo objects using plain SQL queries */
  implicit def GetResultrVideo(implicit e0: GR[Long], e1: GR[String]): GR[rVideo] = GR{
    prs => import prs._
      rVideo.tupled((<<[Long], <<[Long], <<[Long], <<[Long], <<[String], <<[String], <<[Long], <<[String]))
  }
  /** Table description of table video. Objects of this class serve as prototypes for rows in queries. */
  class tVideo(_tableTag: Tag) extends profile.api.Table[rVideo](_tableTag, Some("geek"), "video") {
    def * = (id, userid, roomid, timestamp, filename, length, invitation, comment) <> (rVideo.tupled, rVideo.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(userid), Rep.Some(roomid), Rep.Some(timestamp), Rep.Some(filename), Rep.Some(length), Rep.Some(invitation), Rep.Some(comment))).shaped.<>({r=>import r._; _1.map(_=> rVideo.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get, _7.get, _8.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column ID SqlType(BIGINT), AutoInc, PrimaryKey */
    val id: Rep[Long] = column[Long]("ID", O.AutoInc, O.PrimaryKey)
    /** Database column USERID SqlType(BIGINT) */
    val userid: Rep[Long] = column[Long]("USERID")
    /** Database column ROOMID SqlType(BIGINT) */
    val roomid: Rep[Long] = column[Long]("ROOMID")
    /** Database column TIMESTAMP SqlType(BIGINT) */
    val timestamp: Rep[Long] = column[Long]("TIMESTAMP")
    /** Database column FILENAME SqlType(VARCHAR), Length(300,true) */
    val filename: Rep[String] = column[String]("FILENAME", O.Length(300,varying=true))
    /** Database column LENGTH SqlType(VARCHAR), Length(100,true) */
    val length: Rep[String] = column[String]("LENGTH", O.Length(100,varying=true))
    /** Database column INVITATION SqlType(BIGINT) */
    val invitation: Rep[Long] = column[Long]("INVITATION")
    /** Database column COMMENT SqlType(VARCHAR), Length(500,true) */
    val comment: Rep[String] = column[String]("COMMENT", O.Length(500,varying=true))
  }
  /** Collection-like TableQuery object for table tVideo */
  lazy val tVideo = new TableQuery(tag => new tVideo(tag))

  /** Entity class storing rows of table tVideoTest
    *  @param id Database column ID SqlType(BIGINT), AutoInc, PrimaryKey
    *  @param userid Database column USERID SqlType(BIGINT)
    *  @param roomid Database column ROOMID SqlType(BIGINT)
    *  @param timestamp Database column TIMESTAMP SqlType(BIGINT)
    *  @param filename Database column FILENAME SqlType(VARCHAR), Length(300,true)
    *  @param invitation Database column INVITATION SqlType(BIGINT), Default(None)
    *  @param length Database column LENGTH SqlType(VARCHAR), Length(100,true)
    *  @param comment Database column COMMENT SqlType(VARCHAR), Length(500,true), Default(None) */
  case class rVideoTest(id: Long, userid: Long, roomid: Long, timestamp: Long, filename: String, invitation: Option[Long] = None, length: String, comment: Option[String] = None)
  /** GetResult implicit for fetching rVideoTest objects using plain SQL queries */
  implicit def GetResultrVideoTest(implicit e0: GR[Long], e1: GR[String], e2: GR[Option[Long]], e3: GR[Option[String]]): GR[rVideoTest] = GR{
    prs => import prs._
      rVideoTest.tupled((<<[Long], <<[Long], <<[Long], <<[Long], <<[String], <<?[Long], <<[String], <<?[String]))
  }
  /** Table description of table video_test. Objects of this class serve as prototypes for rows in queries. */
  class tVideoTest(_tableTag: Tag) extends profile.api.Table[rVideoTest](_tableTag, Some("geek"), "video_test") {
    def * = (id, userid, roomid, timestamp, filename, invitation, length, comment) <> (rVideoTest.tupled, rVideoTest.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(userid), Rep.Some(roomid), Rep.Some(timestamp), Rep.Some(filename), invitation, Rep.Some(length), comment)).shaped.<>({r=>import r._; _1.map(_=> rVideoTest.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6, _7.get, _8)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column ID SqlType(BIGINT), AutoInc, PrimaryKey */
    val id: Rep[Long] = column[Long]("ID", O.AutoInc, O.PrimaryKey)
    /** Database column USERID SqlType(BIGINT) */
    val userid: Rep[Long] = column[Long]("USERID")
    /** Database column ROOMID SqlType(BIGINT) */
    val roomid: Rep[Long] = column[Long]("ROOMID")
    /** Database column TIMESTAMP SqlType(BIGINT) */
    val timestamp: Rep[Long] = column[Long]("TIMESTAMP")
    /** Database column FILENAME SqlType(VARCHAR), Length(300,true) */
    val filename: Rep[String] = column[String]("FILENAME", O.Length(300,varying=true))
    /** Database column INVITATION SqlType(BIGINT), Default(None) */
    val invitation: Rep[Option[Long]] = column[Option[Long]]("INVITATION", O.Default(None))
    /** Database column LENGTH SqlType(VARCHAR), Length(100,true) */
    val length: Rep[String] = column[String]("LENGTH", O.Length(100,varying=true))
    /** Database column COMMENT SqlType(VARCHAR), Length(500,true), Default(None) */
    val comment: Rep[Option[String]] = column[Option[String]]("COMMENT", O.Length(500,varying=true), O.Default(None))
  }
  /** Collection-like TableQuery object for table tVideoTest */
  lazy val tVideoTest = new TableQuery(tag => new tVideoTest(tag))
}
