name := "scala-fsm"

scalaVersion := "2.12.3"
sbtVersion := "0.13.16"

lazy val `reactive-lab` = project.in(file("."))
  .settings(resolvers ++= Dependencies.additionalResolvers)
  .settings(libraryDependencies ++= Dependencies.projectDependencies)
        