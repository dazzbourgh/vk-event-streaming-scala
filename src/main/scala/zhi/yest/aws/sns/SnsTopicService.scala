package zhi.yest.aws.sns

import java.util.concurrent.CompletableFuture

import com.google.gson.Gson
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sns.model.{PublishRequest, PublishResponse}
import zhi.yest.aws.sns.SnsTopicService.snsClient
import zhi.yest.vk.config.Configuration.config

trait SnsTopicPublisher[T] {
  def publish(message: T): CompletableFuture[PublishResponse]
}

class SnsTopicService[T](private val topicArn: String = config.getString("aws.sns.topic-arn"))
  extends SnsTopicPublisher[T] {
  override def publish(message: T): CompletableFuture[PublishResponse] = {
    val publishRequest = PublishRequest.builder()
      .message(new Gson().toJson(message))
      .topicArn(topicArn)
      .build()
    snsClient.publish(publishRequest)
  }
}

object SnsTopicService {
  private val snsClient = SnsAsyncClient.builder()
    .region(Region.US_WEST_1)
    .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
    .build()

  def apply[T](topicArn: String): SnsTopicService[T] = new SnsTopicService(topicArn)

  def apply[T](): SnsTopicService[T] = {
    new SnsTopicService()
  }
}
