package zhi.yest

import akka.actor.ActorSystem
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.scaladsl.{Keep, Sink}
import zhi.yest.vk.methods.Streaming

import scala.io.StdIn

object Main {
  def main(args: Array[String]): Unit = {
    implicit val actorSystem: ActorSystem = ActorSystem()
    val streaming: Streaming = Streaming()
    val complete = streaming.openConnection(Sink.foreach[Message] {
      case message: TextMessage.Strict =>
        println(message)
    })
    println("Press any key to stop the program.")
    StdIn.readLine()
    println("Exiting...")
    complete.success(None)
  }
}