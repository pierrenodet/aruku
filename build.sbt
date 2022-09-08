lazy val sparkVersion      = "3.2.2"
lazy val scalaCheckVersion = "1.15.4"
lazy val scalatestVersion  = "3.2.13"
lazy val scala212Version   = "2.12.15"
lazy val scala213Version   = "2.13.8"

lazy val commonSettings = Seq(
  resolvers ++= Resolver.sonatypeOssRepos("public"),
  resolvers ++= Resolver.sonatypeOssRepos("snapshots"),
  organization := "com.github.pierrenodet",
  homepage := Some(url(s"https://github.com/pierrenodet/aruku")),
  licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  developers := List(
    Developer(
      "pierrenodet",
      "Pierre Nodet",
      "nodet.pierre@gmail.com",
      url("https://github.com/pierrenodet")
    )
  ),
  headerLicense := Some(
    HeaderLicense.Custom(
      """|Copyright 2019 Pierre Nodet
         |
         |Licensed under the Apache License, Version 2.0 (the "License");
         |you may not use this file except in compliance with the License.
         |You may obtain a copy of the License at
         |
         |    http://www.apache.org/licenses/LICENSE-2.0
         |
         |Unless required by applicable law or agreed to in writing, software
         |distributed under the License is distributed on an "AS IS" BASIS,
         |WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
         |See the License for the specific language governing permissions and
         |limitations under the License.""".stripMargin
    )
  ),
  scalaVersion := scala213Version,
  crossScalaVersions := Seq(scala212Version, scala213Version),
  Compile / doc / scalacOptions --= Seq("-Xfatal-warnings"),
  Compile / doc / scalacOptions ++= Seq(
    "-groups",
    "-sourcepath",
    (LocalRootProject / baseDirectory).value.getAbsolutePath,
    "-doc-source-url",
    "https://github.com/pierrenodet/aruku/blob/v" + version.value + "€{FILE_PATH}.scala"
  ),
  Test / run / fork := true,
  Test/ parallelExecution := false
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
    name := "aruku-core",
    description := "A Random Walk Engine for Apache Spark",
    libraryDependencies ++= Seq(
      "org.apache.spark" %% "spark-core"   % sparkVersion      % Provided,
      "org.apache.spark" %% "spark-graphx" % sparkVersion      % Provided,
      "org.scalacheck"   %% "scalacheck"   % scalaCheckVersion % Test,
      "org.scalatest"    %% "scalatest"    % scalatestVersion  % Test,
      "org.scalatest" %% "scalatest-funsuite" % scalatestVersion % Test
    )
  )

lazy val docs = project
  .in(file("modules/aruku-docs"))
  .dependsOn(core)
  .enablePlugins(MdocPlugin, DocusaurusPlugin, ScalaUnidocPlugin)
  .settings(commonSettings)
  .settings(
    moduleName := "aruku-docs",
    publish / skip := true,
    mdocVariables := Map("VERSION" -> version.value.takeWhile(_ != '+')),
    mdocIn := new File("modules/docs"),
    ScalaUnidoc / unidoc / unidocProjectFilter := inProjects(core),
    ScalaUnidoc / unidoc / target := (LocalRootProject / baseDirectory ).value / "website" / "static" / "api",
    cleanFiles += (ScalaUnidoc / unidoc / target).value,
    docusaurusCreateSite := docusaurusCreateSite.dependsOn(Compile / unidoc ).value,
    docusaurusPublishGhpages := docusaurusPublishGhpages.dependsOn(Compile / unidoc ).value
  )
