package zhi.yest

import java.time.LocalTime
import java.time.temporal.ChronoUnit

import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.model.ws.Message
import akka.stream.SinkShape
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Keep, Sink}
import org.apache.kafka.clients.consumer.ConsumerRecord
import zhi.yest.aws.sns.SnsTopicService
import zhi.yest.kafka.Kafka
import zhi.yest.vk.dto.EventCodeResponseDto
import zhi.yest.vk.methods.Streaming

import scala.concurrent.Future
import scala.io.StdIn

// TODO: create additional source for timer events
object Main {
  def main(args: Array[String]): Unit = {
    implicit val actorSystem: ActorSystem = ActorSystem()
    val streaming = Streaming()
    val snsEventService = SnsTopicService()

    val processingGraph = buildProducerGraph(Kafka.messageFlow.toMat(Kafka.sink)(Keep.right))(Sink.foreach(println))
    val consumerGraph = Kafka.source.to(
      buildConsumerGraph(
        Sink.foreach[ConsumerRecord[String, EventCodeResponseDto]](event => snsEventService.publish(event.value())),
        rateSink { eventCount => println(s"${LocalTime.now()}: $eventCount events per second.") }
      ))

    val completeStreaming = streaming.openConnection(processingGraph)
    consumerGraph.run()

    println("Press any key to stop the program.")
    StdIn.readLine()
    println("Exiting...")

    completeStreaming.success(None)
    actorSystem.terminate()
  }

  private def rateSink(persist: Float => Unit) = {
    val startTime = LocalTime.now()
    var eventCount = 0
    Flow[Any].map(_ => 1).to(
      Sink.foreach {
        _ => {
          eventCount += 1
          val currentTime = LocalTime.now()
          val diff = startTime.until(currentTime, ChronoUnit.SECONDS)
          persist(eventCount / diff)
        }
      })
  }

  private def buildConsumerGraph(sinks: Sink[ConsumerRecord[String, EventCodeResponseDto], Any]*) = {
    val size = sinks.length
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._
      val B = builder.add(Broadcast[ConsumerRecord[String, EventCodeResponseDto]](size))

      for (i <- 0 until size) {
        B.out(i) ~> sinks(i)
      }

      SinkShape(B.in)
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
