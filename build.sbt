name := "battlenet_emulator"

version := "0.1"

scalaVersion := "2.13.6"

libraryDependencies += "org.typelevel" %% "cats-core" % "2.6.1"
libraryDependencies += "org.typelevel" %% "cats-effect" % "3.2.7"
libraryDependencies += "com.sksamuel.avro4s" %% "avro4s-core" % "4.0.10"
libraryDependencies += "org.typelevel" %% "munit-cats-effect-3" % "1.0.5" % Test
libraryDependencies += "com.spotify" %% "magnolify-avro" % "0.4.4"
libraryDependencies += "eu.timepit" %% "refined" % "0.9.27"
libraryDependencies += "io.estatico" %% "newtype" % "0.4.4"
libraryDependencies += "com.beachape" % "enumeratum_2.13" % "1.7.0"

scalacOptions += "-Ymacro-annotations"

// scalac options come from the sbt-tpolecat plugin so need to set any here

addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.13.2" cross CrossVersion.full)