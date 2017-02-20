package services

import java.net.URLEncoder
import javax.inject.Inject

import client.JsonHttpClient
import controllers.SearchRequest
import play.api.Configuration
import play.api.libs.json.{JsArray, JsValue}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DifferenceService @Inject()(jsonClient: JsonHttpClient, configuration: Configuration) {
  val originalSearchApiUrl: String = "ORIGINAL_SEARCH_API_URL"
  val newSearchApiUrl: String = "NEW_SEARCH_API_URL"

  def diff(searchRequest: SearchRequest): Future[(Set[String], Set[String])] = {
    val originalSearchUrl = getSearchUrl(searchRequest, originalSearchApiUrl)
    val newSearchUrl = getSearchUrl(searchRequest, newSearchApiUrl)

    val originalResultsFuture = jsonClient.get(originalSearchUrl).map {
      result => transformResult(result, searchRequest)
    }

    val newResultsFuture = jsonClient.get(newSearchUrl).map {
      result => transformResult(result, searchRequest)
    }

    originalResultsFuture.zip(newResultsFuture).map {
      case (originalResults, newResults) => {
        //newResults.groupBy(a => a).mapValues(_.size).toList.filter(_._2 > 1).foreach(println)
        val extraInQA = originalResults.toSet.diff(newResults.toSet)
        val extraInDEV = newResults.toSet.diff(originalResults.toSet)
        (extraInQA, extraInDEV)
      }
    }
  }

  private def transformResult(searchResponse: JsValue, searchRequest: SearchRequest): List[String] = {
    val result = searchResponse \ "searchResults"
    val searchResults = (result \ "results").get.as[JsArray]

    searchResults.value.indices.map(index => {
      (searchResults.value(index) \ "doi").get.as[String].replaceAll("<mark>|</mark>", "")
    }
    ).toList
  }

  def getSearchUrl(searchRequest: SearchRequest, searchApiUrl: String): String = {

    StringBuilder.newBuilder
      .append(configuration.getString(searchApiUrl).get)
      .append("/search?query=" + URLEncoder.encode(searchRequest.searchTerm, "UTF-8"))
      .append("&offset=1&limit=1000")
      .append("&sortType=relevance&startDate=1900-01-01&endDate=2017-02-17&disableFacets=country,technique,source,articleType")
      .toString()
  }

}
