package zhi.yest.processing

import java.time.{LocalDate, LocalTime, ZoneOffset}
import java.util.concurrent.TimeUnit

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Sink, Source}
import org.apache.kafka.clients.consumer.ConsumerRecord
import zhi.yest.aws.sns.SnsTopicService
import zhi.yest.dto._

import scala.concurrent.duration.FiniteDuration

object Processing {
  def recordToEvent: Flow[ConsumerRecord[String, EventCodeResponseDto], EventCodeResponseDto, NotUsed] = {
    Flow[ConsumerRecord[String, EventCodeResponseDto]].map(record => record.value())
  }

  def toRateDto: Flow[ConsumerRecord[String, EventCodeResponseDto], RateDto, NotUsed] = {
    var eventCount = 0
    var startTime = LocalTime.now()

    def rateDto = {
      val now = LocalTime.now()
      val today = LocalDate.now()
      val result = RateDto(
        startTime.toEpochSecond(today, ZoneOffset.UTC),
        now.toEpochSecond(today, ZoneOffset.UTC),
        eventCount
      )
      eventCount = 0
      startTime = now
      result
    }

    val minute = FiniteDuration(1, TimeUnit.MINUTES)
    Flow.fromSinkAndSource(Sink.foreach(_ => eventCount += 1),
      Source.tick(minute, minute, None).map(_ => rateDto))
  }

  def awsEventSink = {
    val snsTopicService = SnsTopicService[EventCodeResponseDto]()
    Sink.foreach[EventCodeResponseDto](snsTopicService.publish)
  }

  def consoleRateSink = {
    Sink.foreach[RateDto] { rateDto =>
      println(s"Events during last 60 seconds: ${rateDto.events}")
    }
  }
}
