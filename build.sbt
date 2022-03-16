name := "battlenet_emulator"

version := "0.1"

scalaVersion := "2.13.6"

resolvers += "confluent" at "https://packages.confluent.io/maven/"

lazy val root = (project in file("."))
  .configs(IntegrationTest)
  .enablePlugins(JavaAppPackaging)
  .settings(
    Defaults.itSettings,
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "2.6.1",
      "org.typelevel" %% "cats-effect" % "3.2.7",
      "org.typelevel" %% "munit-cats-effect-3" % "1.0.5" % "it,test",
      "eu.timepit" %% "refined" % "0.9.27",
      "io.estatico" %% "newtype" % "0.4.4",
      "com.beachape" % "enumeratum_2.13" % "1.7.0",
      "com.github.javafaker" % "javafaker" % "1.0.2",
      "com.banno" %% "kafka4s" % "4.0.0-M4"
    ),
    testFrameworks += new TestFramework("munit.Framework"),
    dockerBaseImage := "openjdk:11",
    packageName in Docker := "dockerraccoon/battle-net-emulator"
  )

scalacOptions ++= Seq("-Ymacro-annotations", "-Yrangepos")

// scalac options come from the sbt-tpolecat plugin so need to set any here

addCompilerPlugin(
  "org.typelevel" %% "kind-projector" % "0.13.2" cross CrossVersion.full)
