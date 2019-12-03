package zhi.yest

import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.model.ws.Message
import akka.stream.SinkShape
import akka.stream.scaladsl.{Broadcast, GraphDSL, Keep, Merge, Sink}
import org.apache.kafka.clients.consumer.ConsumerRecord
import zhi.yest.dto.{EventCodeResponseDto, TimeEvent}
import zhi.yest.processing.Kafka
import zhi.yest.processing.Processing._
import zhi.yest.vk.methods.Streaming

import scala.concurrent.Future
import scala.io.StdIn

object Main {
  def main(args: Array[String]): Unit = {
    implicit val actorSystem: ActorSystem = ActorSystem()
    val streaming = Streaming()

    val producerGraph = buildProducerGraph(Kafka.messageFlow.toMat(Kafka.sink)(Keep.right))(Sink.foreach(println))
    val consumerClosedGraph = Kafka.source.to(consumerSink)

    val completeStreaming = streaming.openConnection(producerGraph)
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
      val FAN_OUT = builder.add(Broadcast[ConsumerRecord[String, EventCodeResponseDto]](2))
      val FAN_IN = builder.add(Merge[TimeEvent](2))

      val TICK_SOURCE = builder.add(tickSource).out
      val RECORD_TO_EVENT = builder.add(recordToEvent)
      val TO_TIMER_TICK_EVENT = builder.add(toTimerTickEvent)
      val TO_TIMER_VK_EVENT = builder.add(toTimerVkEvent)
      val TO_RATE_DTO = builder.add(toRateDto)
      val AWS_EVENT_SINK = builder.add(awsEventSink)
      val AWS_RATE_SINK = builder.add(awsRateSink)

      FAN_OUT.out(0) ~> TO_TIMER_VK_EVENT ~> FAN_IN.in(0)
      TICK_SOURCE ~> TO_TIMER_TICK_EVENT ~> FAN_IN.in(1)
      FAN_IN.out ~> TO_RATE_DTO ~> AWS_RATE_SINK
      FAN_OUT.out(1) ~> RECORD_TO_EVENT ~> AWS_EVENT_SINK

      SinkShape(FAN_OUT.in)
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
