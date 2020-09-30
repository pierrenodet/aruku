lazy val sparkVersion      = "2.4.5"
lazy val scalaCheckVersion = "1.14.0"
lazy val scalatestVersion  = "3.0.8"
lazy val scala211Version   = "2.11.8"
lazy val scala212Version   = "2.12.10"

lazy val commonSettings = Seq(
  resolvers += Resolver.sonatypeRepo("public"),
  resolvers += Resolver.sonatypeRepo("snapshots"),
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
      """|Copyright 2020 Pierre Nodet
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
  scalaVersion := scala211Version,
  crossScalaVersions := Seq(scala212Version, scala211Version),
  Compile / doc / scalacOptions --= Seq("-Xfatal-warnings"),
  Compile / doc / scalacOptions ++= Seq(
    "-groups",
    "-sourcepath",
    (baseDirectory in LocalRootProject).value.getAbsolutePath,
    "-doc-source-url",
    "https://github.com/pierrenodet/aruku/blob/v" + version.value + "€{FILE_PATH}.scala"
  ),
  fork.in(Test, run) := true,
  parallelExecution.in(Test) := false,
  assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false),
  test in assembly := {},
  assemblyExcludedJars in assembly := { 
  val cp = (fullClasspath in assembly).value
  cp filter {k => k.data.getName.contains("breeze")||k.data.getName.contains("java8")||k.data.getName.contains("math3")||k.data.getName.contains("netty")||k.data.getName.contains("netlib")}
}
)

lazy val aruku = project
  .in(file("."))
  .enablePlugins(AutomateHeaderPlugin)
  .enablePlugins(ScalaUnidocPlugin)
  .settings(commonSettings)
  .settings(publish / skip := true)
  .dependsOn(core,embedding)
  .aggregate(core,embedding)

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
      "org.scalatest"    %% "scalatest"    % scalatestVersion  % Test
    )
  )

lazy val glintWord2Vec = RootProject(uri("https://github.com/MGabr/glint-word2vec.git"))

lazy val embedding = project
  .in(file("modules/embedding"))
  .enablePlugins(AutomateHeaderPlugin)
  .settings(commonSettings)
  .dependsOn(core,glintWord2Vec)
  .settings(
    name := "aruku-embedding",
    description := "Node and Edge Embeddings",
    libraryDependencies ++= Seq(
      "org.apache.spark" %% "spark-mllib" % sparkVersion      % Provided,
      "org.scalacheck"   %% "scalacheck"   % scalaCheckVersion % Test,
      "org.scalatest"    %% "scalatest"    % scalatestVersion  % Test
    )
  )

lazy val docs = project
  .in(file("modules/aruku-docs"))
  .dependsOn(core,embedding)
  .enablePlugins(MdocPlugin, DocusaurusPlugin, ScalaUnidocPlugin)
  .settings(commonSettings)
  .settings(
    moduleName := "aruku-docs",
    skip in publish := true,
    mdocVariables := Map("VERSION" -> version.value.takeWhile(_ != '+')),
    mdocIn := new File("modules/docs"),
    unidocProjectFilter in (ScalaUnidoc, unidoc) := inProjects(core),
    target in (ScalaUnidoc, unidoc) := (baseDirectory in LocalRootProject).value / "website" / "static" / "api",
    cleanFiles += (target in (ScalaUnidoc, unidoc)).value,
    docusaurusCreateSite := docusaurusCreateSite.dependsOn(unidoc in Compile).value,
    docusaurusPublishGhpages := docusaurusPublishGhpages.dependsOn(unidoc in Compile).value
  )
