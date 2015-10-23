import sbt._
import Keys._

object Build extends Build {
  lazy val edgar = Project(id = "edgar", base = file("edgar")).settings(
      name := "edgar",
      version := "1.0",
      scalaVersion := "2.11.6",
      libraryDependencies ++= Seq(
            "com.github.scopt" %% "scopt" % "3.3.0",
            "com.typesafe.akka" % "akka-actor_2.11" % "2.3.9",
            "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
            "ch.qos.logback" % "logback-classic" % "1.1.2",
            "io.spray" % "spray-json_2.11" % "1.3.2",
            "io.spray" % "spray-can_2.11" % "1.3.3",
            "io.spray" % "spray-httpx_2.11" % "1.3.3",
            "io.spray" % "spray-client_2.11" % "1.3.3",
            "io.spray" % "spray-util_2.11" % "1.3.3"
      ),
      resolvers += Resolver.sonatypeRepo("public"),
      fork in run := true
  )
}
