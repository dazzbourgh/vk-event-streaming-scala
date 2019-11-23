package zhi.yest.aws.sns

import java.util.concurrent.CompletableFuture

import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sns.model.{PublishRequest, PublishResponse}
import zhi.yest.vk.config.Config.config

trait SnsTopicPublisher {
  def publish(message: String): CompletableFuture[PublishResponse]
}

class SnsTopicService(private val snsClient: SnsAsyncClient,
                      private val topicArn: String = config.getString("aws.sns.topic-arn"))
  extends SnsTopicPublisher {
  override def publish(message: String): CompletableFuture[PublishResponse] = {
    val publishRequest = PublishRequest.builder()
      .message(message)
      .topicArn(topicArn)
      .build()
    snsClient.publish(publishRequest)
  }
}
