package zhi.yest

import java.util.concurrent.{CompletableFuture, TimeUnit}

import com.google.gson.Gson
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sns.SnsAsyncClient
import zhi.yest.aws.sns.SnsTopicService
import zhi.yest.vk.methods.{Rules, Streaming}

object Main {
  def main(args: Array[String]): Unit = {
    val snsClient = SnsAsyncClient.builder()
      .region(Region.US_WEST_1)
      .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
      .build()
    val snsService = new SnsTopicService(snsClient)
    val streaming: Streaming = Streaming()
    val futureResponseArray = Rules(streaming).getRules.rules
      .map { rule =>
        snsService.publish(new Gson().toJson(rule))
      }

    CompletableFuture.allOf(futureResponseArray: _*)
      .get(10000L, TimeUnit.MILLISECONDS)
  }
}
