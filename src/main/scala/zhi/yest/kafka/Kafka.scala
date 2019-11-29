package zhi.yest.kafka

import akka.actor.ActorSystem
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.scaladsl.{Flow, Sink}
import akka.{Done, NotUsed}
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer

import scala.concurrent.Future

/**
 * Provides methods to create [[Sink]] and [[akka.stream.scaladsl.Source]] for Kafka streaming.
 */
object Kafka {
  def sink(implicit system: ActorSystem):
  Sink[ProducerRecord[String, String], Future[Done]] = {
    val config = system.settings.config.getConfig("akka.kafka.producer")
    val bootstrapServers = system.settings.config.getString("akka.kafka.bootstrap-servers")
    val producerSettings = ProducerSettings(config, new StringSerializer, new StringSerializer)
      .withBootstrapServers(bootstrapServers)
    Producer plainSink producerSettings
  }

  def source()(implicit system: ActorSystem) = ???

  def messageFlow: Flow[Message, ProducerRecord[String, String], NotUsed] = Flow[Message]
    .filter { message => message.isInstanceOf[TextMessage.Strict] }
    .map { message => message.asTextMessage.getStrictText }
    .map[ProducerRecord[String, String]] { message => new ProducerRecord("events", message) }
}
