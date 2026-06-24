import org.typelevel.scalacoptions.ScalacOptions

lazy val scala212 = "2.12.18"
lazy val scala213 = "3.8.4"
lazy val scala3   = "3.3.1"

ThisBuild / organization       := "dev.hshn"
ThisBuild / homepage           := Some(url("https://github.com/hshn/slick-codegen-patch"))
ThisBuild / licenses           := Seq(License.MIT)
ThisBuild / versionScheme      := Some("early-semver")
ThisBuild / developers         := List(Developer("hshn", "Shota Hoshino", "sht.hshn@gmail.com", url("https://github.com/hshn")))
ThisBuild / scalaVersion       := scala213
ThisBuild / crossScalaVersions := Seq(scala212, scala213, scala3)

lazy val root = (project in file("."))
  .settings(
    publish / skip := true,
  )
  .aggregate(slickCodegenPatch)

lazy val slickCodegenPatch = (project in file("slick-codegen-patch") withId "slick-codegen-patch")
  .settings(settings)
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.slick" %% "slick-codegen"                 % "3.6.1",
      "org.scalatest"      %% "scalatest"                     % "3.2.20" % Test,
      "org.typelevel"      %% "cats-effect"                   % "3.5.1"  % Test,
      "org.typelevel"      %% "cats-effect-testing-scalatest" % "1.8.0"  % Test,
      "com.mysql"           % "mysql-connector-j"             % "9.7.0"  % Test,
    ),
  )

lazy val settings = Seq(
  tpolecatExcludeOptions += ScalacOptions.warnUnusedImports,
  Test / tpolecatExcludeOptions += ScalacOptions.warnNonUnitStatement,
)

// sonatype