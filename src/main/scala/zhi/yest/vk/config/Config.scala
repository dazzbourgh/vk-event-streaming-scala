package zhi.yest.vk.config

import com.typesafe.config.{Config, ConfigFactory}

object Config {
  val config: Config = ConfigFactory.load("application.conf")
}
