package com.github.rerorero

import akka.actor.ActorSystem
import akka.http.scaladsl.{ClientTransport, Http}
import akka.http.scaladsl.model._
import akka.http.scaladsl.settings.{ClientConnectionSettings, ConnectionPoolSettings}
import akka.stream.ActorMaterializer
import com.github.rerorero.Main.AkkaHttpCensusStats

import scala.concurrent.Future
import scala.util.{Failure, Success}

object Client {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher

    val settings = ConnectionPoolSettings(system).withConnectionSettings(
      //ClientConnectionSettings(system).withTransport(ClientTransport.TCP)
      ClientConnectionSettings(system).withTransport(AkkaHttpCensusStats.transport)
    )
    //val responseFuture: Future[HttpResponse] = Http().singleRequest(HttpRequest(uri = "http://localhost:8080/hello"), settings = settings)
    val responseFuture: Future[HttpResponse] = Http().singleRequest(HttpRequest(uri = "http://example.com/"), settings = settings)
    //val responseFuture: Future[HttpResponse] = Http().singleRequest(HttpRequest(uri = "https://example.com/"))

    responseFuture
      .onComplete {
        case Success(res) =>
          println(res)
          materializer.shutdown()
          system.terminate()
        case Failure(_)   => sys.error("something wrong")
      }
  }
}