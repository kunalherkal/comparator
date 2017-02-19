package models

case class FinalResponse(searchTerm: String, originalURL: String, newURL: String, private val originalResults: List[Document], private val newResults: List[Document]) {
  private val originalResultsWithIndex = originalResults.zipWithIndex
  private val newResultsWithIndex = newResults.zipWithIndex

  private val originalResultsWithIndexMap = originalResults.map(_.doi).zipWithIndex.toMap
  private val newResultsWithIndexMap = newResults.map(_.doi).zipWithIndex.toMap

  val newResultsWithDelta = newResultsWithIndex.map {
    case (document, newIndex) => {
      val originalIndex = originalResultsWithIndexMap.getOrElse(document.doi, 9999999)
      val delta = originalIndex - newIndex

      Title(document.title, document.doi, delta)
    }
  }

  val originalResultsWithDelta = originalResultsWithIndex.map {
    case (document, originalIndex) => {
      val newIndex = newResultsWithIndexMap.get(document.doi)
      if (newIndex.isDefined)
        Title(document.title, document.doi, 0)
      else
        Title(document.title, document.doi, -9999999)
    }
  }
}

case class Title(value: String, doi: String, private val delta: Int) {
  private val singlePageLimit = 1000

  val stringDelta = delta match {
    case 0 => ""
    case a if a < 0 => "↓" + getDeltaToShow(a)
    case b => "↑" + getDeltaToShow(b)
  }

  private def getDeltaToShow(delta: Int) = math.abs(delta) match {
    case a if a < singlePageLimit => a.toString
    case b if delta < 0 => "To Next Page"
    case c if delta > 0 => "From Next Page"

  }

  val level = math.abs(delta) match {
    case 0 => "LimeGreen"
    case a if a < 4 => "YELLOW"
    case b if b < singlePageLimit => "DarkOrange"
    case _ => "RED"
  }
}