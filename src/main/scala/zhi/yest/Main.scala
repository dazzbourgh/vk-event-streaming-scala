package zhi.yest

import akka.actor.ActorSystem
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Sink}
import akka.stream.{Inlet, SinkShape}
import org.apache.kafka.clients.producer.ProducerRecord
import zhi.yest.kafka.KafkaEvents
import zhi.yest.vk.methods.Streaming

import scala.io.StdIn

object Main {
  def main(args: Array[String]): Unit = {
    val snsClient = SnsAsyncClient.builder()
      .region(Region.US_WEST_1)
      .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
      .build()
    val snsService = new SnsTopicService(snsClient)
    val streaming: Streaming = Streaming()
    val futureResponseArray = Rules(streaming).getRules.rules
      .map { rule =>
        snsService.publish(new Gson().toJson(rule))
      }

    CompletableFuture.allOf(futureResponseArray: _*)
      .get(10000L, TimeUnit.MILLISECONDS)

    implicit val actorSystem: ActorSystem = ActorSystem()
    val streaming: Streaming = Streaming()
    val processingGraph = buildGraph(Kafka.messageFlow.toMat(Kafka.sink)(Keep.right))(Sink.foreach(println))
    val complete = streaming.openConnection(processingGraph)
    println("Press any key to stop the program.")
    StdIn.readLine()
    println("Exiting...")
    complete.success(None)
    actorSystem.terminate()
  }

  private def buildGraph(webSocketSink: Sink[Message, Future[Done]])(additionalSinks: Sink[Message, Any]*) = {
    GraphDSL.create(webSocketSink) { implicit builder =>
      s =>
        import GraphDSL.Implicits._

        val outputs = additionalSinks.length

        val B = builder.add(Broadcast[Message](outputs + 1))

        B.out(0) ~> s
        for (i <- 1 to outputs) {
          B.out(i) ~> additionalSinks(i - 1)
        }

        SinkShape(B.in)
    }
  }
}
