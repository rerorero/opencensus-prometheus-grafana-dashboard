package com.github.rerorero

import akka.http.scaladsl.server.Directives.{extractRequestContext, mapRouteResult}
import akka.http.scaladsl.server.RouteResult.{Complete, Rejected}
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.HostDirectives._
import akka.http.scaladsl.server.directives.MethodDirectives._
import akka.stream.scaladsl.Flow
import akka.util.ByteString
import com.github.rerorero.AkkaHttpCensusStats.MeasureContext
import io.opencensus.contrib.http.util.{HttpMeasureConstants, HttpViews}
import io.opencensus.stats.Stats
import io.opencensus.tags.{TagValue, Tags}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

class AkkaHttpCensusStats(private[this] val executionContext: ExecutionContext) {
  implicit val ec: ExecutionContext = executionContext
  val httpStats: Directive0 = {
    val contextF = for {
      started <- AkkaHttpCensusStats.startTimeDirective
      request <- extractRequestContext
      hostname <- extractHost
      method <- extractMethod
    } yield {
      MeasureContext(started, request, hostname, method.value.capitalize)
    }

    contextF.flatMap { rc =>
      val onDone: Try[RouteResult] => Unit = {
        case Success(Complete(response)) =>
          val tags = AkkaHttpCensusStats.tagger.emptyBuilder()
            .put(HttpMeasureConstants.HTTP_SERVER_METHOD, TagValue.create(rc.method))
            .put(HttpMeasureConstants.HTTP_SERVER_PATH, TagValue.create(rc.reqCtx.request.uri.path.toString()))
            .put(HttpMeasureConstants.HTTP_SERVER_HOST, TagValue.create(rc.hostname))
            .put(HttpMeasureConstants.HTTP_SERVER_STATUS, TagValue.create(response.status.intValue.toString))
            .build()

          var measureMap = AkkaHttpCensusStats.statsRecorder.newMeasureMap()
          measureMap = rc.reqCtx.request.entity.contentLengthOption.fold(measureMap)(measureMap.put(HttpMeasureConstants.HTTP_SERVER_RECEIVED_BYTES, _))
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

object AkkaHttpCensusStats {

  def apply()(implicit ec: ExecutionContext): AkkaHttpCensusStats = {
    new AkkaHttpCensusStats(ec)
  }

  def register(): Unit = HttpViews.registerAllServerViews()

  val default = new AkkaHttpCensusStats(ExecutionContext.Implicits.global)

  private val tagger = Tags.getTagger()

  private val statsRecorder = Stats.getStatsRecorder()

  private val startTimeDirective: Directive[Tuple1[Long]] = Directive[Tuple1[Long]] { inner => ctx =>
    val time = System.currentTimeMillis()
    inner(Tuple1[Long](time.toLong))(ctx)
  }

  private case class MeasureContext(
    started: Long,
    reqCtx: RequestContext,
    hostname: String,
    method: String
  )
}
