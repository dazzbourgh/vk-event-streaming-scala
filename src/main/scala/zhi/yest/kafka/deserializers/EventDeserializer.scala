package zhi.yest.kafka.deserializers

import java.util

import com.google.gson.Gson
import org.apache.commons.codec.binary.StringUtils
import org.apache.kafka.common.serialization.Deserializer
import zhi.yest.vk.dto.EventCodeResponseDto

class EventDeserializer extends Deserializer[EventCodeResponseDto] {
  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {
    // nothing to do
  }

  override def deserialize(topic: String, data: Array[Byte]): EventCodeResponseDto = {
    val objectString = StringUtils.newString(data, "UTF-8")
    new Gson().fromJson(objectString, classOf[EventCodeResponseDto])
  }

  override def close(): Unit = {
    // nothing to do
  }
}
