package controllers

import javax.inject._

import play.api.mvc._
import services.{CompareService, DifferenceService}

import scala.concurrent.ExecutionContext.Implicits.global


@Singleton
class HomeController @Inject()(compareService: CompareService, differenceService: DifferenceService) extends Controller {

  def compare() = Action.async { request =>
    val searchRequest = createSearchRequest(request)

    compareService.compare(searchRequest).map {
      response =>
        Ok(views.html.index(response))
    }
  }

  def difference() = Action.async { request =>
    val searchRequest = createSearchRequest(request)

    differenceService.diff(searchRequest).map {
      response =>
        val stringResponse = "Extra In QA: " + response._1 + "\nExtra in DEV: " + response._2
        Ok(stringResponse)
    }
  }

  private def createSearchRequest(request: Request[AnyContent]): SearchRequest = {
    val searchTerm = request.getQueryString("term").getOrElse("")

    SearchRequest(searchTerm)
  }
}

case class SearchRequest(searchTerm: String)
