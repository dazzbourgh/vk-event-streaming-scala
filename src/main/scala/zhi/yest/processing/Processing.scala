package zhi.yest.processing

import java.time.temporal.ChronoUnit
import java.time.{LocalDate, LocalTime, ZoneOffset}
import java.util.concurrent.TimeUnit

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Sink, Source}
import org.apache.kafka.clients.consumer.ConsumerRecord
import zhi.yest.aws.sns.SnsTopicService
import zhi.yest.dto._

import scala.concurrent.duration.FiniteDuration

object Processing {
  def tickSource = {
    val minute = FiniteDuration(1, TimeUnit.MINUTES)
    Source.tick(minute, minute, true)
  }

  def toTimerTickEvent: Flow[Boolean, TickEvent, NotUsed] = {
    Flow[Boolean].map(_ => TickEvent(LocalTime.now()
      .minus(1, ChronoUnit.MINUTES), LocalTime.now()))
  }

  def toTimerVkEvent: Flow[ConsumerRecord[String, EventCodeResponseDto], VkEvent, NotUsed] = {
    Flow[ConsumerRecord[String, EventCodeResponseDto]].map(_ => VkEvent())
  }

  def recordToEvent = {
    Flow[ConsumerRecord[String, EventCodeResponseDto]].map(record => record.value())
  }

  def toRateDto: Flow[TimeEvent, RateDto, NotUsed] = {
    var eventCount = 0
    var startTime = LocalTime.now()
    Flow.fromFunction[TimeEvent, RateDto] {
      case _: TickEvent =>
        val now = LocalTime.now()
        val today = LocalDate.now()
        val result = RateDto(
          startTime.toEpochSecond(today, ZoneOffset.UTC),
          now.toEpochSecond(today, ZoneOffset.UTC),
          eventCount / 60
        )
        eventCount = 0
        startTime = now
        result
      case _: VkEvent =>
        eventCount += 1
        null
    }.filter(rate => rate != null)
  }

  def awsEventSink = {
    val snsTopicService = SnsTopicService[EventCodeResponseDto]()
    Sink.foreach[EventCodeResponseDto](snsTopicService.publish)
  }

  def awsRateSink = {
    // TODO: assign real arn
    //    val snsTopicService = SnsTopicService[RateDto]("")
    //    Sink.foreach[RateDto](snsTopicService.publish)
    Sink.foreach[RateDto](println)
  }
}
