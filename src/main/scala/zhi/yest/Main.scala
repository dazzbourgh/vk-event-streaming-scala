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
    implicit val actorSystem: ActorSystem = ActorSystem()
    val streaming: Streaming = Streaming()
    val kafkaSink = KafkaEvents.sink()
    val processingGraph = GraphDSL.create(kafkaSink) { implicit builder => s =>
      import GraphDSL.Implicits._

      val B = builder.add(Broadcast[Message](2))
      val CONSOLE: Inlet[Any] = builder.add(Sink.foreach(println)).in
      val messageToTextMapper = Flow[Message]
        .filter { message => message.isInstanceOf[TextMessage.Strict] }
        .map { message => message.asTextMessage.getStrictText }

      val textToRecordMapper = Flow[String].map[ProducerRecord[String, String]] { message => new ProducerRecord("events", message) }

      B.out(0) ~> CONSOLE
      B.out(1) ~> messageToTextMapper ~> textToRecordMapper ~> s

      SinkShape(B.in)
    }
    val complete = streaming.openConnection(processingGraph)
    println("Press any key to stop the program.")
    StdIn.readLine()
    println("Exiting...")
    complete.success(None)
  }
}