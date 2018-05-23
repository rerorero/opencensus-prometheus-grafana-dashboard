name := "akka-census"

val commonSettings = Seq(
  scalaVersion := "2.12.4",
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
  .settings(commonSettings)
  .dependsOn(stats)

lazy val client = (project in file("modules/client"))
  .settings(commonSettings)
  .dependsOn(stats)
