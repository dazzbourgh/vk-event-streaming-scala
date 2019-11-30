package zhi.yest

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

    implicit val actorSystem: ActorSystem = ActorSystem()
    val streaming: Streaming = Streaming()
    val kafkaSink = KafkaEvents.sink()
    val processingGraph = GraphDSL.create(kafkaSink) { implicit builder => s =>
      import GraphDSL.Implicits._

      val B = builder.add(Broadcast[Message](2))
      val CONSOLE: Inlet[Any] = builder.add(Sink.foreach(println)).in
      val messageToTextMapper = Flow[Message]
        .filter { message => message.isInstanceOf[TextMessage.Strict] }
        .map { message => message.asTextMessage.getStrictText }

      val textToRecordMapper = Flow[String].map[ProducerRecord[String, String]] { message => new ProducerRecord("events", message) }

      B.out(0) ~> CONSOLE
      B.out(1) ~> messageToTextMapper ~> textToRecordMapper ~> s

      SinkShape(B.in)
    }
    val complete = streaming.openConnection(processingGraph)
    println("Press any key to stop the program.")
    StdIn.readLine()
    println("Exiting...")
    complete.success(None)
  }
}
