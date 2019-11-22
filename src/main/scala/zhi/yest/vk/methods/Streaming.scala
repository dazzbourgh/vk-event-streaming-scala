package zhi.yest.vk.methods

import zhi.yest.vk.dto.{StreamingResponse, StreamingResponseDto}

final class Streaming extends BaseMethod {
  override protected val baseUrl: String = "https://api.vk.com/method"
  override protected val methodUrl: String = "streaming"

  lazy val streamingResponse: StreamingResponse = getServerUrl.response

  def getServerUrl: StreamingResponseDto = {
    val request = RequestBuilder()
      .withUrl(s"$baseUrl/$methodUrl.getServerUrl")
      .withParameter("access_token", accessToken)
      .withParameter("v", v)
      .build()
    val response = httpClient.execute(request)
    getResponse(response, classOf[StreamingResponseDto])
  }

  def openConnection() = {
    // TODO: implement
    ???
  }
}

object Streaming {
  def apply() = new Streaming()
}
