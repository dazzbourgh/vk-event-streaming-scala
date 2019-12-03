package zhi.yest

import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.model.ws.Message
import akka.stream.SinkShape
import akka.stream.scaladsl.{Broadcast, GraphDSL, Keep, Sink}
import org.apache.kafka.clients.consumer.ConsumerRecord
import zhi.yest.dto.EventCodeResponseDto
import zhi.yest.processing.{Kafka, Processing}
import zhi.yest.vk.methods.Streaming

import scala.concurrent.Future
import scala.io.StdIn

object Main {
  def main(args: Array[String]): Unit = {
    implicit val actorSystem: ActorSystem = ActorSystem()
    val streaming = Streaming()

    val producerGraph = buildProducerGraph(Kafka.messageFlow.toMat(Kafka.sink)(Keep.right))(Sink.foreach(println))
    val completeStreaming = streaming.openConnection(producerGraph)

    val consumerClosedGraph = Kafka.source.to(consumerSink)
    consumerClosedGraph.run()

    println("Press any key to stop the program.")
    StdIn.readLine()
    println("Exiting...")

    completeStreaming.success(None)
    actorSystem.terminate()
  }

  private def consumerSink = {
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._
      val fanOut = builder.add(Broadcast[ConsumerRecord[String, EventCodeResponseDto]](2))

      val toEvent = builder.add(Processing.recordToEvent)
      val toRateDto = builder.add(Processing.toRateDto)
      val awsEventSink = builder.add(Processing.awsEventSink)
      val consoleRateSink = builder.add(Processing.consoleRateSink)

      fanOut.out(0) ~> toRateDto ~> consoleRateSink
      fanOut.out(1) ~> toEvent   ~> awsEventSink

      SinkShape(fanOut.in)
    }
  }

  private def buildProducerGraph(webSocketSink: Sink[Message, Future[Done]])
                                (additionalSinks: Sink[Message, Any]*) = {
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
