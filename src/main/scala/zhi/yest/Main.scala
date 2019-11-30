package zhi.yest

import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.model.ws.Message
import akka.stream.SinkShape
import akka.stream.scaladsl.{Broadcast, GraphDSL, Keep, Sink}
import zhi.yest.aws.sns.SnsTopicService
import zhi.yest.kafka.Kafka
import zhi.yest.vk.methods.Streaming

import scala.concurrent.Future
import scala.io.StdIn

object Main {
  def main(args: Array[String]): Unit = {
    implicit val actorSystem: ActorSystem = ActorSystem()
    val streaming = Streaming()
    val snsService = SnsTopicService()

    val processingGraph = buildGraph(Kafka.messageFlow.toMat(Kafka.sink)(Keep.right))(Sink.foreach(println))
    val consumerGraph = Kafka.source.to(
      Sink.foreach(event => snsService.publish(event.value())))

    val completeStreaming = streaming.openConnection(processingGraph)
    consumerGraph.run()

    println("Press any key to stop the program.")
    StdIn.readLine()
    println("Exiting...")

    completeStreaming.success(None)
    actorSystem.terminate()
  }

  private def buildGraph(webSocketSink: Sink[Message, Future[Done]])
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
