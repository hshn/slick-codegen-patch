import org.typelevel.scalacoptions.ScalacOptions

lazy val scala212 = "2.12.18"
lazy val scala213 = "2.13.12"
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
      "com.typesafe.slick" %% "slick-codegen"                 % "3.5.0",
      "org.scalatest"      %% "scalatest"                     % "3.2.18" % Test,
      "org.typelevel"      %% "cats-effect"                   % "3.5.1"  % Test,
      "org.typelevel"      %% "cats-effect-testing-scalatest" % "1.5.0"  % Test,
      "com.mysql"           % "mysql-connector-j"             % "8.3.0"  % Test,
    ),
  )

lazy val settings = Seq(
  tpolecatExcludeOptions += ScalacOptions.warnUnusedImports,
  Test / tpolecatExcludeOptions += ScalacOptions.warnNonUnitStatement,
)

// github workflows
ThisBuild / githubWorkflowJavaVersions := Seq(JavaSpec.temurin("17"))
ThisBuild / githubWorkflowTargetTags ++= Seq("v*")
ThisBuild / githubWorkflowBuild ~= { steps =>
  WorkflowStep.Use(
    UseRef.Public("isbang", "compose-action", "v1.5.1"),
    Map(
      "compose-file" -> "./docker-compose.yml",
    ),
  ) +: steps
}
ThisBuild / githubWorkflowPublishTargetBranches := Seq(RefPredicate.StartsWith(Ref.Tag("v")))
ThisBuild / githubWorkflowPublish := Seq(
  WorkflowStep.Sbt(
    commands = List("ci-release"),
    name = Some("Publish project"),
    env = Map(
      "PGP_PASSPHRASE"    -> "${{ secrets.PGP_PASSPHRASE }}",
      "PGP_SECRET"        -> "${{ secrets.PGP_SECRET }}",
      "SONATYPE_USERNAME" -> "${{ secrets.SONATYPE_USERNAME }}",
      "SONATYPE_PASSWORD" -> "${{ secrets.SONATYPE_PASSWORD }}",
    ),
  ),
)
ThisBuild / githubWorkflowPublishTargetBranches := Seq(
  RefPredicate.StartsWith(Ref.Tag("v")),
  RefPredicate.Equals(Ref.Branch("main")),
)

// sonatype
ThisBuild / sonatypeCredentialHost := "s01.oss.sonatype.org"
sonatypeRepository                 := "https://s01.oss.sonatype.org/service/local"
