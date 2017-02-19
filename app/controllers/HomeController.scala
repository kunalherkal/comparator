package controllers

import javax.inject._

import play.api.mvc._
import services.CompareService

import scala.concurrent.ExecutionContext.Implicits.global


@Singleton
class HomeController @Inject()(compareService: CompareService) extends Controller {

  def compare() = Action.async { request =>
    val searchRequest = createSearchRequest(request)

    compareService.compare(searchRequest).map {
      response =>
        Ok(views.html.index(response))
    }
  }

  private def createSearchRequest(request: Request[AnyContent]): SearchRequest = {
    val searchTerm = request.getQueryString("term").getOrElse("")

    SearchRequest(searchTerm)
  }
}

case class SearchRequest(searchTerm: String)
