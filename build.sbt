name := "edgar"

version := "1.0"

scalaVersion := "2.11.7"

libraryDependencies += "com.github.scopt" %% "scopt" % "3.3.0"
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.2"

resolvers += Resolver.sonatypeRepo("public")

fork in run := true
    
