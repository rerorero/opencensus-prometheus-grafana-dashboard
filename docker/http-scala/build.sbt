name := "akka-census"

val commonSettings = Seq(
  version := "0.1-SNAPSHOT",
  scalaVersion := "2.12.4",
  organization := "com.github.rerorero",
  test in assembly := {},
  libraryDependencies ++= Seq(
    "io.opencensus" % "opencensus-exporter-stats-prometheus" % "0.13.2",
    "io.opencensus" % "opencensus-api" % "0.13.2",
    "io.opencensus" % "opencensus-impl" % "0.13.2",
    "io.opencensus" % "opencensus-contrib-http-util" % "0.13.2",
    "io.prometheus" % "simpleclient_httpserver" % "0.3.0",
    "com.typesafe.akka" %% "akka-http"   % "10.1.1",
    "com.typesafe.akka" %% "akka-stream" % "2.5.11"
  )
)

lazy val stats = (project in file("modules/stats"))
  .settings(commonSettings)

lazy val server = (project in file("modules/server"))
  .dependsOn(stats)
  .settings(commonSettings)
  .settings(
    mainClass in assembly := Some("com.github.rerorero.WebServer")
  )

lazy val client = (project in file("modules/client"))
  .dependsOn(stats)
  .settings(commonSettings)
  .settings(
    mainClass in assembly := Some("com.github.rerorero.Client")
  )
