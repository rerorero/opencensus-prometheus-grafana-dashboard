package com.github.rerorero.akkacensus

import akka.http.scaladsl.server.Directives.{extractRequestContext, mapRouteResult}
import akka.http.scaladsl.server.RouteResult.{Complete, Rejected}
import akka.http.scaladsl.server._
import akka.stream.scaladsl.Flow
import akka.util.ByteString
import io.opencensus.contrib.http.util.{HttpMeasureConstants, HttpViews}
import io.opencensus.stats.Stats
import io.opencensus.tags.{TagValue, Tags}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

class HttpServerStatsRecorder(private[this] val executionContext: ExecutionContext) {
  implicit val ec: ExecutionContext = executionContext
  import HttpServerStatsRecorder._
  val statsDirective: Directive0 = {
    val contextF = for {
      started <- startTimeDirective
      request <- extractRequestContext
    } yield {
      RequestContext.fromRequest(request.request, started)
    }

    contextF.flatMap { rc =>
      val onDone: Try[RouteResult] => Unit = {
        case Success(Complete(response)) =>
          val tags = tagger.emptyBuilder()
            .put(HttpMeasureConstants.HTTP_SERVER_METHOD, TagValue.create(rc.method))
            .put(HttpMeasureConstants.HTTP_SERVER_PATH, TagValue.create(rc.uri.path.toString()))
            .put(HttpMeasureConstants.HTTP_SERVER_HOST, TagValue.create(rc.hostname))
            .put(HttpMeasureConstants.HTTP_SERVER_STATUS, TagValue.create(response.status.intValue.toString))
            .build()

          var measureMap = statsRecorder.newMeasureMap()
          measureMap = rc.lengthOption.fold(measureMap)(measureMap.put(HttpMeasureConstants.HTTP_SERVER_RECEIVED_BYTES, _))
          measureMap = response.entity.contentLengthOption.fold(measureMap)(measureMap.put(HttpMeasureConstants.HTTP_SERVER_SENT_BYTES, _))

          val d = System.currentTimeMillis() - rc.started
          measureMap
            .put(HttpMeasureConstants.HTTP_SERVER_LATENCY, d)
            .record(tags)
        case Success(Rejected(_)) =>
        case Failure(_)           =>
      }

      mapRouteResult {
        case c @ Complete(response) =>
          Complete(response.mapEntity { entity =>
            // https://www.reddit.com/r/scala/comments/7ymz62/measuring_response_time_in_akkahttp/duo3kp8/
            if (entity.isKnownEmpty()) {
              onDone(Success(c))
              entity
            } else {
              entity.transformDataBytes(Flow[ByteString].watchTermination() {
                case (m, f) =>
                  f.map(_ => c).onComplete(onDone)
                  m
              })
            }
          })
        case r: Rejected =>
          onDone(Success(r))
          r
      }
    }
  }
}

object HttpServerStatsRecorder {

  def apply()(implicit ec: ExecutionContext): HttpServerStatsRecorder = {
    new HttpServerStatsRecorder(ec)
  }

  def register(): Unit = HttpViews.registerAllServerViews()

  val default = new HttpServerStatsRecorder(ExecutionContext.Implicits.global)

  private val tagger = Tags.getTagger()

  private val statsRecorder = Stats.getStatsRecorder()

  private lazy val startTimeDirective: Directive[Tuple1[Long]] = Directive[Tuple1[Long]] { inner => ctx =>
    val time = System.currentTimeMillis()
    inner(Tuple1[Long](time.toLong))(ctx)
  }
}
