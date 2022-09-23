lazy val sparkVersion      = "3.2.2"
lazy val scalaCheckVersion = "1.16.0"
lazy val scalatestVersion  = "3.2.13"
lazy val scala212Version   = "2.12.15"
lazy val scala213Version   = "2.13.8"

Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / organization                 := "com.github.pierrenodet"
ThisBuild / organizationName             := "Pierre Nodet"
ThisBuild / homepage                     := Some(url(s"https://github.com/pierrenodet/aruku"))
ThisBuild / startYear                    := Some(2019)
ThisBuild / licenses                     := Seq(License.Apache2)
ThisBuild / developers                   := List(
  Developer(
    "pierrenodet",
    "Pierre Nodet",
    "nodet.pierre@gmail.com",
    url("https://github.com/pierrenodet")
  )
)
ThisBuild / scalaVersion                 := scala213Version
ThisBuild / crossScalaVersions           := Seq(scala212Version, scala213Version)
ThisBuild / githubWorkflowJavaVersions   := Seq("8", "11").map(JavaSpec.temurin(_))
ThisBuild / githubWorkflowTargetTags ++= Seq("v*")
ThisBuild / githubWorkflowPublishTargetBranches +=
  RefPredicate.StartsWith(Ref.Tag("v"))
ThisBuild / githubWorkflowPublish        := Seq(
  WorkflowStep.Sbt(List("ci-release")),
  WorkflowStep.Sbt(List("docs/docusaurusPublishGhpages"))
)
ThisBuild / githubWorkflowBuild          := Seq(WorkflowStep.Sbt(List("coverage", "test", "coverageReport")))
ThisBuild / githubWorkflowBuildPostamble := Seq(WorkflowStep.Run(List("bash <(curl -s https://codecov.io/bash)")))
ThisBuild / resolvers ++= Resolver.sonatypeOssRepos("public")
ThisBuild / resolvers ++= Resolver.sonatypeOssRepos("snapshots")

lazy val commonSettings = Seq(
  Compile / doc / scalacOptions --= Seq("-Xfatal-warnings"),
  Compile / doc / scalacOptions ++= Seq(
    "-groups",
    "-sourcepath",
    (LocalRootProject / baseDirectory).value.getAbsolutePath,
    "-doc-source-url",
    "https://github.com/pierrenodet/aruku/blob/v" + version.value + "€{FILE_PATH}.scala"
  ),
  Test / run / fork        := true,
  Test / parallelExecution := false
)

lazy val aruku = project
  .in(file("."))
  .enablePlugins(AutomateHeaderPlugin)
  .enablePlugins(ScalaUnidocPlugin)
  .settings(commonSettings)
  .settings(publish / skip := true)
  .dependsOn(core)
  .aggregate(core)

lazy val core = project
  .in(file("modules/core"))
  .enablePlugins(AutomateHeaderPlugin)
  .settings(commonSettings)
  .settings(
    name        := "aruku-core",
    description := "A Random Walk Engine for Apache Spark",
    libraryDependencies ++= Seq(
      "org.apache.spark"  %% "spark-core"              % sparkVersion      % Provided,
      "org.apache.spark"  %% "spark-graphx"            % sparkVersion      % Provided,
      "org.scalacheck"    %% "scalacheck"              % scalaCheckVersion % Test,
      "org.scalatest"     %% "scalatest"               % scalatestVersion  % Test,
      "org.scalatestplus" %% ("scalacheck" + "-" + scalaCheckVersion
        .split("\\.")
        .toList
        .take(2)
        .mkString("-"))    % (scalatestVersion + ".0") % Test,
      "org.scalatest"     %% "scalatest-funsuite"      % scalatestVersion  % Test
    )
  )

lazy val docs = project
  .in(file("modules/aruku-docs"))
  .dependsOn(core)
  .enablePlugins(MdocPlugin, DocusaurusPlugin, ScalaUnidocPlugin)
  .settings(commonSettings)
  .settings(
    moduleName                                 := "aruku-docs",
    publish / skip                             := true,
    mdocVariables                              := Map("VERSION" -> version.value.takeWhile(_ != '+')),
    mdocIn                                     := new File("modules/docs"),
    ScalaUnidoc / unidoc / unidocProjectFilter := inProjects(core),
    ScalaUnidoc / unidoc / target              := (LocalRootProject / baseDirectory).value / "website" / "static" / "api",
    cleanFiles += (ScalaUnidoc / unidoc / target).value,
    docusaurusCreateSite                       := docusaurusCreateSite.dependsOn(Compile / unidoc).value,
    docusaurusPublishGhpages                   := docusaurusPublishGhpages.dependsOn(Compile / unidoc).value,
    githubWorkflowArtifactUpload               := false
  )

lazy val benchmarks = project
  .in(file("modules/benchmarks"))
  .dependsOn(core)
  .settings(
    publish / skip               := true,
    githubWorkflowArtifactUpload := false,
    name                         := "aruku-benchmarks"
  )
  .enablePlugins(JmhPlugin)
