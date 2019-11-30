package zhi.yest.aws.sns

import java.util.concurrent.CompletableFuture

import com.google.gson.Gson
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sns.model.{PublishRequest, PublishResponse}
import zhi.yest.vk.config.Configuration.config
import zhi.yest.vk.dto.EventCodeResponseDto

trait SnsTopicPublisher[T] {
  def publish(message: T): CompletableFuture[PublishResponse]
}

class SnsTopicService(private val snsClient: SnsAsyncClient,
                      private val topicArn: String = config.getString("aws.sns.topic-arn"))
  extends SnsTopicPublisher[EventCodeResponseDto] {
  override def publish(message: EventCodeResponseDto): CompletableFuture[PublishResponse] = {
    val publishRequest = PublishRequest.builder()
      .message(new Gson().toJson(message))
      .topicArn(topicArn)
      .build()
    snsClient.publish(publishRequest)
  }
}

object SnsTopicService {
  def apply(): SnsTopicService = {
    val snsClient = SnsAsyncClient.builder()
      .region(Region.US_WEST_1)
      .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
      .build()
    new SnsTopicService(snsClient)
  }
}
