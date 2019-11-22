package zhi.yest.vk.methods

import com.google.gson.Gson
import org.apache.commons.io.IOUtils
import org.apache.http.client.methods.{CloseableHttpResponse, HttpGet, HttpPost, HttpUriRequest}
import org.apache.http.client.utils.URIBuilder
import org.apache.http.entity.BasicHttpEntity
import org.apache.http.impl.client.{CloseableHttpClient, HttpClients}
import org.apache.http.util.EntityUtils
import zhi.yest.vk.config.Config.config

import scala.util.Using

abstract class BaseMethod {
  protected val httpClient: CloseableHttpClient = HttpClients.createDefault()
  protected val baseUrl: String
  protected val methodUrl: String
  protected val accessToken: String = config.getString("vk.accessToken")
  protected val v: String = "5.103"

  protected def getResponse[T](response: CloseableHttpResponse, clazz: Class[T]): T = {
    Using(response) {
      response =>
        val entity = response.getEntity
        val stringContent = IOUtils.toString(entity.getContent, "UTF-8")
        val res = new Gson().fromJson(stringContent, clazz)
        EntityUtils.consume(entity)
        res
    }.get
  }
}

private[methods] class RequestBuilder {
  private var url = ""
  private var body = ""
  private var method = "get"
  private val parameters = scala.collection.mutable.Map[String, String]()

  def withParameter(key: String, value: String) = {
    parameters += (key -> value)
    this
  }

  def withUrl(url: String) = {
    this.url = url
    this
  }

  def withBody[T](body: T) = {
    this.body = new Gson().toJson(body)
    this
  }

  def withMethod(method: String) = {
    this.method = method
    this
  }

  def build(): HttpUriRequest = {
    val builder = new URIBuilder(url)
    for (param <- parameters) {
      builder.addParameter(param._1, param._2)
    }
    MethodExtractor(method) match {
      case "get" => new HttpGet(builder.build())
      case "post" => val req = new HttpPost(builder.build())
        val requestEntity = new BasicHttpEntity()
        val requestString = new Gson().toJson(body)
        requestEntity.setContent(IOUtils.toInputStream(requestString, "UTF-8"))
        req.setEntity(requestEntity)
        req
    }
  }

  private class MethodExtractor(private val method: String)

  private object MethodExtractor {
    def apply(method: String): String = method

    def unapply(arg: String): Boolean = arg.equalsIgnoreCase(method)
  }

}

object RequestBuilder {
  def apply(): RequestBuilder = new RequestBuilder()
}