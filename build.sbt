name := "edgar"

version := "1.0"

scalaVersion := "2.11.6"

libraryDependencies += "com.github.scopt" %% "scopt" % "3.3.0"

libraryDependencies += "com.typesafe.akka" % "akka-actor_2.11" % "2.3.9"
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.2"

libraryDependencies += "io.spray" % "spray-json_2.11" % "1.3.2"
libraryDependencies += "io.spray" % "spray-can_2.11" % "1.3.3"
libraryDependencies += "io.spray" % "spray-httpx_2.11" % "1.3.3"
libraryDependencies += "io.spray" % "spray-client_2.11" % "1.3.3"
libraryDependencies += "io.spray" % "spray-util_2.11" % "1.3.3"

resolvers += Resolver.sonatypeRepo("public")


fork in run := true
    
