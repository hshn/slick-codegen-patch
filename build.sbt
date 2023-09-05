import org.typelevel.scalacoptions.ScalacOptions

lazy val scala212 = "2.12.18"
lazy val scala213 = "2.13.11"

ThisBuild / organization       := "dev.hshn"
ThisBuild / version            := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion       := scala213
ThisBuild / crossScalaVersions := Seq(scala212, scala213)

lazy val slickCodegenPatch = (project in file("slick-codegen-patch"))
  .settings(settings)
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.slick" %% "slick-codegen"                 % "3.4.1",
      "org.scalatest"      %% "scalatest"                     % "3.2.16" % Test,
      "org.typelevel"      %% "cats-effect"                   % "3.5.1"  % Test,
      "org.typelevel"      %% "cats-effect-testing-scalatest" % "1.5.0"  % Test,
      "com.mysql"           % "mysql-connector-j"             % "8.1.0"  % Test,
    ),
  )

lazy val settings = Seq(
  versionScheme := Some("early-semver"),
  tpolecatExcludeOptions += ScalacOptions.warnUnusedImports,
  Test / tpolecatExcludeOptions += ScalacOptions.warnNonUnitStatement,
)

// github workflows
ThisBuild / githubWorkflowJavaVersions := Seq(JavaSpec.temurin("17"))
ThisBuild / githubWorkflowTargetTags ++= Seq("v*")
ThisBuild / githubWorkflowPublishTargetBranches := Seq(RefPredicate.StartsWith(Ref.Tag("v")))
ThisBuild / githubWorkflowJobSetup ~= { steps =>
  steps :+ WorkflowStep.Use(
    UseRef.Public("isbang", "compose-action", "v1.5.1"),
    Map(
      "compose-file" -> "./docker-compose.yml",
    ),
  )
}
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
