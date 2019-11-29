package zhi.yest.kafka

import akka.Done
import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.scaladsl.Sink
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer

import scala.concurrent.Future

object KafkaEvents {
  def sink()(implicit system: ActorSystem):
  Sink[ProducerRecord[String, String], Future[Done]] = {
    val config = system.settings
      .config
      .getConfig("akka.kafka.producer")
    val bootstrapServers = system.settings
      .config
      .getString("akka.kafka.bootstrap-servers")
    val producerSettings =
      ProducerSettings(config, new StringSerializer, new StringSerializer)
        .withBootstrapServers(bootstrapServers)
    Producer plainSink producerSettings
  }
}
