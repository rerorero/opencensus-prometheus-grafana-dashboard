package com.github.rerorero

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import com.github.rerorero.akkacensus.HttpServerStatsRecorder
import io.opencensus.exporter.stats.prometheus.PrometheusStatsCollector
import io.prometheus.client.exporter.HTTPServer

import scala.io.StdIn

object WebServer {
  def main(args: Array[String]) {

    implicit val system = ActorSystem("my-system")
    implicit val materializer = ActorMaterializer()
    implicit val ec = system.dispatcher
    HttpServerStatsRecorder.register()
    val census = HttpServerStatsRecorder()

    val route: Route =
      census.statsDirective {
        path("hello") {
          get {
            complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>"))
          }
        }
      }
    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

    PrometheusStatsCollector.createAndRegister()
    val promServer = new HTTPServer(9091, true)


    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}
