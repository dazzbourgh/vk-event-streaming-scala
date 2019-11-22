package zhi.yest.vk.dto

import zhi.yest.vk.domain.Rule

case class StreamingResponse(endpoint: String, key: String)

case class StreamingResponseDto(response: StreamingResponse)

object StreamingResponse {
  def apply(endpoint: String, key: String): StreamingResponse = new StreamingResponse(endpoint, key)
}

case class RuleDto(rule: Rule)

case class RuleCodeResponseDto(code: Int, error: ErrorDto)

case class RulesCodeResponseDto(code: Int, rules: Array[Rule], error: ErrorDto)

case class ErrorDto(message: String, error_code: Int)