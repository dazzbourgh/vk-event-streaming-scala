package zhi.yest

import zhi.yest.vk.methods.{Rules, Streaming}

object Main {
  def main(args: Array[String]): Unit = {
    val streaming: Streaming = Streaming()
    println(Rules(streaming).getRules)
  }
}