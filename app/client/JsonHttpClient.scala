package client

import java.net.URLDecoder
import javax.inject.Inject

import com.google.inject.Singleton
import org.slf4j.LoggerFactory
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.JsValue
import play.api.libs.json.Json.parse
import play.api.libs.ws.WSClient

import scala.concurrent.Future

@Singleton
class JsonHttpClient @Inject()(wsClient: WSClient) {
  private val logger = LoggerFactory.getLogger(getClass)

  def get(url: String): Future[JsValue] = {
    wsClient.url(url).get().map(resp => {
      resp.status match {
        case 200 => parse(resp.body.trim)
        case _ => {
          val response = parse(resp.body)
          val errorResponse = (response \ "errorResponse").get
          val statusCode = (errorResponse \ "statusCode").get
          val errorStatus: String = (errorResponse \ "status").get.as[String]
          val searchQuery: String = (errorResponse \ "searchQuery").get.as[String]
          val decodedUrl = URLDecoder.decode(searchQuery, "UTF-8")

          logger.error(s"Api throws error with statusCode = ${statusCode}," +
            s" message = ${errorStatus}, query = ${searchQuery}")

          throw new Exception(s"statusCode: $statusCode, errorStatus: $errorStatus for searchQuery: $decodedUrl")
        }
      }
    }
    )
  }
}
