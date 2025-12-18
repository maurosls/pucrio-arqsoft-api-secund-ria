name := "movies-provider-service"
version := "1.0"
scalaVersion := "2.13.12"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % "10.5.3",
  "com.typesafe.akka" %% "akka-stream" % "2.8.5",
  "de.heikoseeberger" %% "akka-http-circe" % "1.39.2",
  "io.circe" %% "circe-core" % "0.14.6",
  "io.circe" %% "circe-generic" % "0.14.6",
  "io.circe" %% "circe-parser" % "0.14.6",
  "com.softwaremill.sttp.client3" %% "core" % "3.9.1",
  "com.h2database" % "h2" % "2.2.224",
  "org.scalikejdbc" %% "scalikejdbc" % "4.0.0",
  "org.sangria-graphql" %% "sangria" % "4.0.2",
  "org.sangria-graphql" %% "sangria-circe" % "1.3.2",
  "org.slf4j" % "slf4j-simple" % "1.7.36"
)
