package com.github.rerorero

import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import com.github.rerorero.akkacensus.HttpServerStatsRecorder
import io.opencensus.exporter.stats.prometheus.PrometheusStatsCollector
import io.prometheus.client.exporter.HTTPServer

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Promise}
import scala.util.Random

object WebServer {
  def main(args: Array[String]) {

    val appPort = scala.sys.props.getOrElse("app.port", "9900").toInt
    val promPort = scala.sys.props.getOrElse("prom.port", "9901").toInt

    implicit val system = ActorSystem("my-system")
    implicit val materializer = ActorMaterializer()
    implicit val ec = system.dispatcher
    HttpServerStatsRecorder.register()
    val census = HttpServerStatsRecorder()

    val route: Route =
      census.statsDirective {
        path("hello") {
          entity(as[String])(handler)
        } ~
        path("world") {
          entity(as[String])(handler)
        }
      }
    val bindingFuture = Http().bindAndHandle(route, "0.0.0.0", appPort)

    PrometheusStatsCollector.createAndRegister()
    val promServer = new HTTPServer(promPort, true)

    println(s"Server online at http://0.0.0.0:${appPort}/")
    val f = for {
      server <- bindingFuture
      done <- Promise[Done].future
    } yield server.unbind()

    sys.addShutdownHook {
      system.terminate()
    }
    Await.ready(f, Duration.Inf)
  }

  val rnd = new Random

  val handler: String => Route = { _ =>
    Thread.sleep(rnd.nextInt(3)*100)

    rnd.nextInt(30) match {
      case 0 =>
        println(s"response: 400")
        complete(StatusCodes.BadRequest)
      case 1 =>
        println(s"response: 500")
        complete(StatusCodes.InternalServerError)
      case _ =>
        println(s"response: 200")
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "Say hello to akka-http"))
    }
  }
}
