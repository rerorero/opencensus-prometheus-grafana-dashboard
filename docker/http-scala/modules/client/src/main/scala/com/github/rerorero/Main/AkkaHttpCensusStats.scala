package com.github.rerorero.Main

import akka.actor.ActorSystem
import akka.http.scaladsl.ClientTransport
import akka.http.scaladsl.Http.OutgoingConnection
import akka.http.scaladsl.model.{ErrorInfo, HttpMethod, ParsingException, StatusCodes}
import akka.http.scaladsl.settings.{ClientConnectionSettings, ParserSettings}
import akka.stream.scaladsl.{BidiFlow, Flow, Keep}
import akka.stream.stage._
import akka.stream.{Attributes, BidiShape, Inlet, Outlet}
import akka.util.ByteString

import scala.annotation.{switch, tailrec}
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class CensusGraph(host: String, settings: ClientConnectionSettings) extends GraphStage[BidiShape[ByteString, ByteString, ByteString, ByteString]] {
  val bytesIn: Inlet[ByteString] = Inlet("OutgoingCensusTCP.in")
  val bytesOut: Outlet[ByteString] = Outlet("OutgoingCensusTCP.out")
  val sslIn: Inlet[ByteString] = Inlet("OutgoingCensusSSL.in")
  val sslOut: Outlet[ByteString] = Outlet("OutgoingCensusSSL.out")

  override def shape: BidiShape[ByteString, ByteString, ByteString, ByteString] = BidiShape.apply(sslIn, bytesOut, bytesIn, sslOut)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) with StageLogging {
    setHandler(sslIn, new InHandler {
      override def onPush() = {
        val v = grab(sslIn)
        log.error("natoring: outgoing request: " + host + ":" + v.utf8String)
        push(bytesOut, v)
      }
      override def onUpstreamFinish(): Unit = complete(bytesOut)
    })

    setHandler(bytesIn, new InHandler {
      override def onPush() = {
        val v= grab(bytesIn)
        log.error("natoring: inbound response: " + v.utf8String)
        push(sslOut, v)
      }
      override def onUpstreamFinish(): Unit = complete(sslOut)
    })

    setHandler(bytesOut, new OutHandler {
      override def onPull() = {
        log.error("natoring: require outgoing request: ")
        pull(sslIn)
      }
      override def onDownstreamFinish(): Unit = cancel(sslIn)
    })

    setHandler(sslOut, new OutHandler {
      override def onPull() = {
        log.error("natoring: require inbound response")
        pull(bytesIn)
      }
      override def onDownstreamFinish(): Unit = cancel(bytesIn)
    })
  }
}

object AkkaHttpCensusStats {
  val transport = new ClientTransport {
    override def connectTo(host: String, port: Int, settings: ClientConnectionSettings)(implicit system: ActorSystem): Flow[ByteString, ByteString, Future[OutgoingConnection]] = {
      BidiFlow.fromGraph(new CensusGraph(host, settings))
        .joinMat(ClientTransport.TCP.connectTo(host, port, settings))(Keep.right)
        .mapMaterializedValue(s => s)
    }
  }

  def byteChar(input: ByteString, ix: Int): Char = byteAt(input, ix).toChar
  def byteAt(input: ByteString, ix: Int): Byte =
    if (ix < input.length) input(ix) else throw new StringIndexOutOfBoundsException()

  // ref. https://github.com/akka/akka-http/blob/f3e83935ffc27bc94f586d9387b54b499c6250d4/akka-http-core/src/main/scala/akka/http/impl/engine/parsing/HttpRequestParser.scala#L82
  def parseMethod(input: ByteString, cursor: Int, settings: ParserSettings): Try[HttpMethod] = {
    @tailrec def parseCustomMethod(ix: Int = 0, sb: StringBuilder = new StringBuilder(16)): Try[HttpMethod] =
      if (ix < settings.maxMethodLength) {
        byteChar(input, cursor + ix) match {
          case ' ' ⇒
            settings.customMethods(sb.toString) match {
              case Some(m) => Success(m)
              case None ⇒ Failure(new ParsingException(ErrorInfo("Unsupported HTTP method", sb.toString)))
            }
          case c ⇒ parseCustomMethod(ix + 1, sb.append(c))
        }
      } else Failure(new ParsingException(
        ErrorInfo("Unsupported HTTP method", s"HTTP method too long (started with '${sb.toString}'). " +
          "Increase `akka.http.server.parsing.max-method-length` to support HTTP methods with more characters.")))

    @tailrec def parseMethod(meth: HttpMethod, ix: Int = 1): Try[HttpMethod] =
      if (ix == meth.value.length)
        if (byteChar(input, cursor + ix) == ' ')
          Success(meth)
        else
          parseCustomMethod()
      else if (byteChar(input, cursor + ix) == meth.value.charAt(ix)) parseMethod(meth, ix + 1)
      else parseCustomMethod()

    import akka.http.scaladsl.model.HttpMethods._
    (byteChar(input, cursor): @switch) match {
      case 'G' ⇒ parseMethod(GET)
      case 'P' ⇒ byteChar(input, cursor + 1) match {
        case 'O' ⇒ parseMethod(POST, 2)
        case 'U' ⇒ parseMethod(PUT, 2)
        case 'A' ⇒ parseMethod(PATCH, 2)
        case _   ⇒ parseCustomMethod()
      }
      case 'D' ⇒ parseMethod(DELETE)
      case 'H' ⇒ parseMethod(HEAD)
      case 'O' ⇒ parseMethod(OPTIONS)
      case 'T' ⇒ parseMethod(TRACE)
      case 'C' ⇒ parseMethod(CONNECT)
      case _   ⇒ parseCustomMethod()
    }
  }
}
