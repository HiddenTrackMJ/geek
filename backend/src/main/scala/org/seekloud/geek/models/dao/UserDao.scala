package org.seekloud.geek.models.dao
import org.seekloud.geek.Boot.executor
import org.seekloud.geek.models.SlickTables
import org.seekloud.geek.models.SlickTables._
import org.seekloud.geek.utils.DBUtil.db
import org.slf4j.LoggerFactory
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.Failure
/**
 * User: hewro
 * Date: 2020/2/2
 * Time: 15:43
 * Description: user表相关,password 使用md5加密存入数据库
 */
object UserDao {
  private val log = LoggerFactory.getLogger(this.getClass)

  def sigIn(name:String,password:String): Future[Option[SlickTables.rUser]] ={
    db.run(tUser.filter(_.name===name).filter(_.password === password).result.headOption).andThen {
      case Failure(ex) =>
        ex.printStackTrace()
        log.error(s"登录查询数据库失败")
    }
  }

  //先查询，查询不存在，直接创建一个
  def sigIn4Client(name:String,password:String) = {

    val q = for{
      i<- tUser.filter(_.name===name).filter(_.password === password).result.headOption
      m <- if(i nonEmpty){//查询到信息直接返回
        DBIO.successful(i.get.id)
      }else{//注册成功
        tUser.returning(tUser.map(_.id)) += rUser(-1L,name,password)
      }
    }yield (i,m)
    db.run(q)

  }

  def test(filename:String,userId:Long,commentContent:String) = {
    val q=for {
      a<-tVideo.filter(_.filename === filename).filter(_.invitation===userId).result
      c <-tVideo ++= a.map(d=>rVideo(-1L,d.userid, d.roomid,d.timestamp,d.filename,d.length,d.invitation,commentContent))
    } yield a
    db.run(q)
  }

  //注册，暂时不存储头像，随机显示一个头像完事
  def signUp(name:String,password:String): Future[Int] = {
    val q = for{
      i<- tUser.filter(_.name===name).length.result
      m <- if(i>0){//注册失败，名称重复了
        DBIO.successful(-1)
      }else{//注册成功
        tUser += rUser(-1L,name,password)
      }
    }yield m
    db.run(q)
  }

  def searchById(userId: Long): Future[Option[SlickTables.rUser]] = {
    db.run(tUser.filter(i => i.id === userId).result.headOption)
  }

  def getUserDetail(userId:Long) = {
    val action = tUser.filter(t=>t.id === userId).result
    db.run(action)
  }

  def updateUserDetail(userId:Long,userName:String,gender:Int,age:Int,address:String) = {

    val q = for{
      i<- tUser.filter(_.name===userName).length.result
      m <- if(i>0){//注册失败，名称重复了
        DBIO.successful(-1)
      }else{//注册成功
        tUser.filter(t=>t.id === userId).map(x=> (x.name,x.gender,x.age,x.address)).update((userName,Some(gender),Some(age),Some(address)))
      }
    }yield m

//    val action = tUser.filter(t=>t.id === userId).map(x=> (x.name,x.gender,x.age,x.address)).update((userName,Some(gender),Some(age),Some(address)))
//    try{
      db.run(q)
//    }
//    catch {
//      case e: Throwable =>
//        log.error(s"updateUserDetail error with error $e")
//        Future.successful(-1)
//    }

  }

  def updateAvatar(userId:Long,avatar:String) = {
    val action = tUser.filter(t=>t.id === userId).map(_.avatar).update(Option(avatar))
    try{
      db.run(action)
    }
    catch {
      case e: Throwable =>
        log.error(s"updateAvatar error with error $e")
        Future.successful(-1)
    }
  }

}
