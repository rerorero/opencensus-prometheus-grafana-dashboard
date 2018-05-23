package com.github.rerorero.akkacensus

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.scaladsl.{Flow, GraphDSL, Keep, UnzipWith, Zip}
import akka.stream.{FlowShape, OverflowStrategy}
import io.opencensus.contrib.http.util.{HttpMeasureConstants, HttpViews}
import io.opencensus.stats.Stats
import io.opencensus.tags.{TagValue, Tags}

object HttpClientStatsRecorder {
  private val tagger = Tags.getTagger()
  private val statsRecorder = Stats.getStatsRecorder()

  def register(): Unit = HttpViews.registerAllClientViews()

  def connectionWithStats[Mat](connection: Flow[HttpRequest, HttpResponse, Mat]): Flow[HttpRequest, HttpResponse, Mat] = {
    val preRequest = Flow[HttpRequest].map(onRequestStarted)
    val doRequest = branchFlow(connection)
    val postRequest = Flow[(HttpResponse, RequestContext)]
      .map(tpl => onRequestSucceed(tpl._1, tpl._2))

    preRequest
      .viaMat(doRequest)(Keep.right)
      .viaMat(postRequest)(Keep.left)
  }

  private def onRequestStarted(request: HttpRequest): (HttpRequest, RequestContext) = {
    val started = System.currentTimeMillis()
    val con = RequestContext.fromRequest(request, started)
    (request, con)
  }

  private def onRequestSucceed(response: HttpResponse, rc: RequestContext): HttpResponse = {
    val tags = tagger.emptyBuilder()
      .put(HttpMeasureConstants.HTTP_CLIENT_METHOD, TagValue.create(rc.method))
      .put(HttpMeasureConstants.HTTP_CLIENT_STATUS, TagValue.create(response.status.intValue.toString))
      .build()

    var measureMap = statsRecorder.newMeasureMap()
    measureMap = rc.lengthOption.fold(measureMap)(measureMap.put(HttpMeasureConstants.HTTP_CLIENT_SENT_BYTES, _))
    measureMap = response.entity.contentLengthOption.fold(measureMap)(measureMap.put(HttpMeasureConstants.HTTP_CLIENT_RECEIVED_BYTES, _))

    val d = System.currentTimeMillis() - rc.started
    measureMap
      .put(HttpMeasureConstants.HTTP_CLIENT_ROUNDTRIP_LATENCY, d)
      .record(tags)

    response
  }

  private def branchFlow[In, Out, Mat](underlying: Flow[In, Out, Mat]) =
    Flow.fromGraph(GraphDSL.create(underlying) { implicit b => under =>
      import GraphDSL.Implicits._

      val unzip = b.add(UnzipWith[(In, RequestContext), In, RequestContext](identity))
      val zip = b.add(Zip[Out, RequestContext])
      val bufferForZip = Flow[RequestContext].buffer(1000, OverflowStrategy.backpressure)

      unzip.out0 ~> under        ~> zip.in0
      unzip.out1 ~> bufferForZip ~> zip.in1

      FlowShape(unzip.in, zip.out)
    })
}
