import org.typelevel.scalacoptions.ScalacOptions

lazy val scala212 = "2.12.18"
lazy val scala213 = "3.8.1"
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
      "org.scalatest"      %% "scalatest"                     % "3.2.19" % Test,
      "org.typelevel"      %% "cats-effect"                   % "3.5.1"  % Test,
      "org.typelevel"      %% "cats-effect-testing-scalatest" % "1.7.0"  % Test,
      "com.mysql"           % "mysql-connector-j"             % "9.6.0"  % Test,
    ),
  )

lazy val settings = Seq(
  tpolecatExcludeOptions += ScalacOptions.warnUnusedImports,
  Test / tpolecatExcludeOptions += ScalacOptions.warnNonUnitStatement,
)

// github workflows
ThisBuild / githubWorkflowJavaVersions := Seq(JavaSpec.temurin("17"), JavaSpec.temurin("21"))
ThisBuild / githubWorkflowTargetTags ++= Seq("v*")
ThisBuild / githubWorkflowBuild ~= { steps =>
  Seq(
    WorkflowStep.Use(
      UseRef.Public("isbang", "compose-action", "v1.5.1"),
      Map(
        "compose-file" -> "./compose.yml",
      ),
    ),
    WorkflowStep.Run(
      name = Some("Wait for MySQL to be ready"),
      commands = List(
        """for i in {1..30}; do
          |  if mysqladmin ping -h127.0.0.1 -P3307 -uroot -ppassword --silent; then
          |    echo "MySQL is up!"
          |    break
          |  fi
          |  echo "Waiting for MySQL..."
          |  sleep 2
          |done
          |""".stripMargin,
      ),
    ),
  ) ++ steps
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
ThisBuild / sonatypeCredentialHost := xerial.sbt.Sonatype.sonatypeCentralHost
