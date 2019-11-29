package zhi.yest.vk.config

import com.typesafe.config.{Config, ConfigFactory}

object Configuration {
  val config: Config = ConfigFactory.load("application.conf")
}
