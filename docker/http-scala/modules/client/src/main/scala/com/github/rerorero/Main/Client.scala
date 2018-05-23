package com.github.rerorero

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.github.rerorero.akkacensus.HttpClientStatsRecorder
import io.opencensus.exporter.stats.prometheus.PrometheusStatsCollector
import io.prometheus.client.exporter.HTTPServer

import scala.concurrent.Future
import scala.io.StdIn
import scala.util.{Failure, Success}

object Client {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    HttpClientStatsRecorder.register()
    PrometheusStatsCollector.createAndRegister()
    val promServer = new HTTPServer(9092, true)

    val connectionFlow: Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]] =
      Http().outgoingConnection("akka.io")
    val connWithStats = HttpClientStatsRecorder.connectionWithStats(connectionFlow)

    val req = HttpRequest(uri = "http://example.com/")
    Source
      .single(req)
      .via(connWithStats)
      .runWith(Sink.head)
      .onComplete {
        case Success(res) =>
          println(res)
          materializer.shutdown()
          system.terminate()
        case Failure(_)   => sys.error("something wrong")
      }

    StdIn.readLine() // let it run until user presses return
  }
}