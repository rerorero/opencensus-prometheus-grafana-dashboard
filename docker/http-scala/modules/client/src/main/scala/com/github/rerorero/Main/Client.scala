package com.github.rerorero

import java.util.concurrent.Executors

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.ByteString
import com.github.rerorero.akkacensus.HttpClientStatsRecorder
import io.opencensus.exporter.stats.prometheus.PrometheusStatsCollector
import io.prometheus.client.exporter.HTTPServer

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.Random

object Client {
  val rnd = new Random
  val paths = List("hello", "world")

  def main(args: Array[String]): Unit = {

    val destHost = sys.props.getOrElse("dest.host", "127.0.0.1")
    val destPort = sys.props.getOrElse("dest.port", "9900").toInt
    val promPort = sys.props.getOrElse("prom.port", "9910").toInt

    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    HttpClientStatsRecorder.register()
    PrometheusStatsCollector.createAndRegister()
    val promServer = new HTTPServer(promPort, true)

    val connectionFlow: Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]] =
      Http().outgoingConnection(destHost, destPort)
    val connWithStats = HttpClientStatsRecorder.connectionWithStats(connectionFlow)

    def wait = Future {
      val msec = rnd.nextInt(1000) + 10
      println(s"Sleep ${msec} msec...")
      Thread.sleep(msec)
      msec
    }
    def request(count: Int, wait: Int) = {
      val req = Random.nextBoolean() match {
        case true =>
          HttpRequest(uri = s"/${destHost}:${destPort}/${Random.shuffle(paths).head}")
        case false =>
          HttpRequest(HttpMethods.POST, uri = s"/${Random.shuffle(paths).head}", entity = ByteString("TestByteStrrrrrrrrrring"))
      }
      println(s"($count) request ${req.uri}, after $wait")
      Source
        .single(req)
        .via(connWithStats)
        .runWith(Sink.head)
    }

    implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(2))
    val f = Future.sequence((1 to 3000).map { count =>
      for {
        ms <- wait
        resp <- request(count, ms)
      } yield println(s"Respond: ${resp.status}")
    })

    Await.ready(f, 5 minutes)
    Thread.sleep(30 * 1000)
    promServer.stop()
    materializer.shutdown()
    system.terminate()
  }
}
