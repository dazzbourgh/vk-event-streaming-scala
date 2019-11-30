package zhi.yest.vk.dto

import com.google.gson.annotations.SerializedName
import zhi.yest.vk.domain.Rule
import zhi.yest.vk.dto.DtoHelper.Alias

import scala.annotation.meta.{field, getter, setter}

object DtoHelper {
  type Alias = SerializedName@field
}

case class StreamingResponse(endpoint: String, key: String)

case class StreamingResponseDto(response: StreamingResponse)

object StreamingResponse {
  def apply(endpoint: String, key: String): StreamingResponse = new StreamingResponse(endpoint, key)
}

case class RuleDto(rule: Rule)

case class RuleCodeResponseDto(code: Int, error: ErrorDto)

case class RulesCodeResponseDto(code: Int, rules: Array[Rule], error: ErrorDto)

case class ErrorDto(message: String, @Alias("error_code") @setter @getter errorCode: Int)

case class Author(@Alias("author_url") authorUrl: String, id: Int, platform: Int)

case class Event(action: String,
                 author: Author,
                 @Alias("creation_time") creationTime: Long,
                 @Alias("event_type") eventType: String,
                 @Alias("event_url") eventUrl: String,
                 tags: Array[String],
                 text: String)

case class EventCodeResponseDto(code: Int, event: Event)