logLevel := Level.Warn
//val sbtRevolverV = "0.8.0"
val sbtRevolverV = "0.9.1"
// val sbtAssemblyV = "0.13.0"
//val sbtAssemblyV = "0.14.6"
//val sbtPackV = "0.9.2"
val sbtPackV = "0.12"
val sbtScalaJsV = "0.6.31"
//val coursierV = "1.0.0-M15"
//val coursierV = "1.0.0-RC3"
//val coursierV = "1.0.0"
//val buildinfoV = "0.6.1"

addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "0.6.1")


addSbtPlugin("org.scala-js" % "sbt-scalajs" % sbtScalaJsV)

//addSbtPlugin("io.get-coursier" % "sbt-coursier" % coursierV)

addSbtPlugin("io.spray" % "sbt-revolver" % sbtRevolverV)

//addSbtPlugin("com.eed3si9n" % "sbt-assembly" % sbtAssemblyV)

addSbtPlugin("org.xerial.sbt" % "sbt-pack" % sbtPackV)

//addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % buildinfoV)