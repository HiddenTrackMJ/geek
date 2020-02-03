import sbt._


/**
  * User: TangYaruo
  * Date: 2019/8/27
  * Time: 10:50
  */
object Dependencies4Capture {

  val akkaV = "2.5.23"
  val akkaHttpV = "10.1.8"
  val circeVersion = "0.9.3"


  val akkaSeq = Seq(
    //    "com.typesafe.akka" %% "akka-actor" % akkaV withSources(),
    "com.typesafe.akka" %% "akka-actor-typed" % akkaV withSources(),
    "com.typesafe.akka" %% "akka-slf4j" % akkaV,
    //    "com.typesafe.akka" %% "akka-stream" % akkaV,
    "com.typesafe.akka" %% "akka-stream-typed" % akkaV
  )

  val akkaHttpSeq = Seq(
    "com.typesafe.akka" %% "akka-http" % akkaHttpV,
    //    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpV
  )

  val circeSeq = Seq(
    "io.circe" %% "circe-core" % circeVersion,
    "io.circe" %% "circe-generic" % circeVersion,
    "io.circe" %% "circe-parser" % circeVersion
  )

  val logback = "ch.qos.logback" % "logback-classic" % "1.2.3"
  val asynchttpclient = "org.asynchttpclient" % "async-http-client" % "2.0.32"
//  val byteobject = "org.seekloud" %% "byteobject" % "0.1.1"

  val captureDependencies: Seq[ModuleID] =
    akkaSeq ++ akkaHttpSeq ++ circeSeq ++
    Seq(
      logback,
      asynchttpclient
//      byteobject
    )

}
