package zhi.yest.vk.methods

import zhi.yest.vk.domain.Rule
import zhi.yest.vk.dto.{RuleCodeResponseDto, RuleDto, RulesCodeResponseDto, StreamingResponse}

final class Rules(private val streamingEndpoint: String,
            private val key: String) extends BaseMethod {

  override protected val baseUrl: String = s"https://$streamingEndpoint"
  override protected val methodUrl: String = "rules"

  def addRule(rule: Rule): RuleCodeResponseDto = {
    val request = RequestBuilder()
      .withMethod("post")
      .withParameter("key", key)
      .withUrl(s"$baseUrl/$methodUrl")
      .withBody(RuleDto(rule))
      .build()
    getResponse(httpClient.execute(request), classOf[RuleCodeResponseDto])
  }

  def getRules: RulesCodeResponseDto = {
    val request = RequestBuilder()
      .withUrl(s"$baseUrl/$methodUrl")
      .withParameter("key", key)
      .build()
    getResponse(httpClient.execute(request), classOf[RulesCodeResponseDto])
  }
}

object Rules {
  def apply(implicit streaming: Streaming): Rules = {
    val StreamingResponse(streamingEndpoint, key) = streaming.streamingResponse
    new Rules(streamingEndpoint, key)
  }
}
