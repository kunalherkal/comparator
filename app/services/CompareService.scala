package services

import java.net.URLEncoder
import javax.inject.{Inject, Singleton}

import client.JsonHttpClient
import controllers.SearchRequest
import models.{Document, FinalResponse}
import play.api.Configuration
import play.api.libs.json.{JsArray, JsValue}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


@Singleton
class CompareService @Inject()(jsonClient: JsonHttpClient, configuration: Configuration) {
  val originalSearchApiUrl: String = "ORIGINAL_SEARCH_API_URL"
  val newSearchApiUrl: String = "NEW_SEARCH_API_URL"

  def compare(searchRequest: SearchRequest): Future[FinalResponse] = {
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
        FinalResponse(searchRequest.searchTerm,
          configuration.getString(originalSearchApiUrl).get,
          configuration.getString(newSearchApiUrl).get,
          originalResults,
          newResults)
      }
    }
  }

  private def transformResult(searchResponse: JsValue, searchRequest: SearchRequest): List[Document] = {
    val result = searchResponse \ "searchResults"
    val searchResults = (result \ "results").get.as[JsArray]

    searchResults.value.indices.map(index => {
      val title = (searchResults.value(index) \ "title").get.as[String].replaceAll("<mark>|</mark>", "")
      val doi = (searchResults.value(index) \ "doi").get.as[String].replaceAll("<mark>|</mark>", "")
      Document(title, doi)
    }
    ).toList
  }

  def getSearchUrl(searchRequest: SearchRequest, searchApiUrl: String): String = {

    StringBuilder.newBuilder
      .append(configuration.getString(searchApiUrl).get)
      .append("/search?query=" + URLEncoder.encode(searchRequest.searchTerm, "UTF-8"))
      .append("&offset=1&limit=" + configuration.getString("PAGE_LIMIT").getOrElse("20"))
      .append("&sortType=relevance&startDate=1900-01-01&endDate=2017-02-17&disableFacets=country,technique,source,articleType")
      .toString()
  }


}
