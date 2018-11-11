name := "agar.core"

version := "1.0"

scalaVersion := "2.12.6"

lazy val akkaVersion = "2.5.16"
lazy val akkaStreamVersion = "2.5.18"
lazy val akkaHttpVersion = "10.1.5"


libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaStreamVersion,
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaStreamVersion % Test,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
  "org.scalatest" %% "scalatest" % "3.0.5" % "test"
)
