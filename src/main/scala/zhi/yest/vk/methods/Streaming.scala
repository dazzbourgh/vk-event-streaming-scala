package zhi.yest.vk.methods

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{Message, WebSocketRequest}
import akka.stream.{Graph, SinkShape}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import zhi.yest.vk.dto.{StreamingResponse, StreamingResponseDto}

import scala.concurrent.{Future, Promise}

final class Streaming extends BaseMethod {
  override protected val baseUrl: String = "https://api.vk.com/method"
  override protected val methodUrl: String = "streaming"

  lazy val streamingResponse: StreamingResponse = getServerUrl.response

  def getServerUrl: StreamingResponseDto = {
    val request = RequestBuilder()
      .withUrl(s"$baseUrl/$methodUrl.getServerUrl")
      .withParameter("access_token", accessToken)
      .withParameter("v", v)
      .build()
    val response = httpClient.execute(request)
    getResponse(response, classOf[StreamingResponseDto])
  }

  def openConnection(sink: Graph[SinkShape[Message], Future[Done]])
                    (implicit actorSystem: ActorSystem): Promise[Option[Message]] = {
    import actorSystem.dispatcher

    val flow = Flow.fromSinkAndSourceMat(sink, Source.maybe[Message])(Keep.right)

    val (upgradeResponse, promise) =
      Http().singleWebSocketRequest(
        WebSocketRequest(s"wss://${streamingResponse.endpoint}/stream?key=${streamingResponse.key}"),
        flow)

    upgradeResponse.onComplete(_ => println("Websocket connection established."))

    promise
  }
}

object Streaming {
  def apply() = new Streaming()
}
