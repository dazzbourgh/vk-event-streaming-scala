package zhi.yest.processing

import akka.actor.ActorSystem
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.kafka.scaladsl.{Consumer, Producer}
import akka.kafka.{ConsumerSettings, ProducerSettings, Subscriptions}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.{Done, NotUsed}
import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecord}
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}
import zhi.yest.dto.EventCodeResponseDto
import zhi.yest.kafka.deserializers.EventDeserializer
import zhi.yest.vk.config.Configuration

import scala.concurrent.Future

/**
 * Provides methods to create [[Sink]] and [[akka.stream.scaladsl.Source]] for Kafka streaming.
 */
object Kafka {
  val TOPIC: String = Configuration.config.getString("kafka.topic")

  def sink(implicit system: ActorSystem):
  Sink[ProducerRecord[String, String], Future[Done]] = {
    val config = system.settings.config.getConfig("akka.kafka.producer")
    val bootstrapServers = system.settings.config.getString("akka.kafka.bootstrap-servers")
    val producerSettings = ProducerSettings(config, new StringSerializer, new StringSerializer)
      .withBootstrapServers(bootstrapServers)
    Producer plainSink producerSettings
  }

  def source(implicit system: ActorSystem):
  Source[ConsumerRecord[String, EventCodeResponseDto], Consumer.Control] = {
    val config = system.settings.config.getConfig("akka.kafka.consumer")
    val bootstrapServers = system.settings.config.getString("akka.kafka.bootstrap-servers")
    val consumerSettings =
      ConsumerSettings(config, new StringDeserializer, new EventDeserializer)
        .withBootstrapServers(bootstrapServers)
        .withGroupId("group1")
        .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
    Consumer.plainSource(consumerSettings, Subscriptions.topics(TOPIC))
  }

  def messageFlow: Flow[Message, ProducerRecord[String, String], NotUsed] = Flow[Message]
    .filter { message => message.isInstanceOf[TextMessage.Strict] }
    .map { message => message.asTextMessage.getStrictText }
    .map[ProducerRecord[String, String]] { message => new ProducerRecord(TOPIC, message) }
}
