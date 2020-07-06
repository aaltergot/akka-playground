val ScalaTestLib = "org.scalatest" %% "scalatest" % "3.1.2"
val AkkaActorLib = "com.typesafe.akka" %% "akka-actor-typed" % "2.6.6"
val AkkaTestkitLib = "com.typesafe.akka" %% "akka-actor-testkit-typed" % "2.6.6"
val LogbackLib = "ch.qos.logback" % "logback-classic" % "1.2.3"

val sharedSettings = 
  Seq(
    version := "0.1.0-SNAPSHOT",
    organization := "com.github.aaltergot",
    scalaVersion := "2.13.2",
    libraryDependencies ++= Seq(
      ScalaTestLib % "test"
    ),
  )

lazy val projectList = 
  Seq[sbt.ProjectReference](
    app,
    actors
  )

lazy val akkaPlayground = 
  Project(
    id = "akka-playground",
    base = file(".")
  )
  .settings(sharedSettings)
  .settings(
    skip in publish := true
  )
  .aggregate(projectList: _*)

lazy val app =
  Project(
    id = "app",
    base = file("app")
  )
  .settings(sharedSettings)
  .settings(
    name := "app",
    libraryDependencies ++= Seq(
      LogbackLib
    )
  )
  .dependsOn(actors)

lazy val actors =
  Project(
    id = "actors",
    base = file("actors")
  )
  .settings(sharedSettings)
  .settings(
    name := "actors",
    libraryDependencies ++= Seq(
      AkkaActorLib,
      AkkaTestkitLib,
      LogbackLib % "test"
    )
  )
