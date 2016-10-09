name := "withlog"

organization := "io.github.tomykaira"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.10.0"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "1.9.1" % "test",
  "com.typesafe.akka" %% "akka-actor" % "2.2.0",
  "org.scala-lang" % "scala-swing" % "2.10.2",
  "io.github.tomykaira" %% "constraintscala" % "0.1.0",
  "com.typesafe.slick" %% "slick" % "1.0.1",
  "com.h2database" % "h2" % "1.3.166",
  "ch.qos.logback" % "logback-classic" % "1.0.6" % "runtime",
  "net.databinder.dispatch" %% "dispatch-core" % "0.11.0"
)

initialCommands := "import io.github.tomykaira.withlog._"

fork in run := true
