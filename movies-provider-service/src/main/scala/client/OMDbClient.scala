package client

import domain.Movie
import sttp.client3._
import io.circe.generic.auto._
import io.circe.parser._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

case class OMDbMovie(Title: String, Year: String, Plot: String, imdbID: String, Genre: String)

object OMDbClient {
  private val backend = HttpURLConnectionBackend()
  private val apiKey = sys.env.getOrElse("OMDB_API_KEY", "244ae6ab")
  
  def getMovie(title: String): Future[Option[Movie]] = {
    val request = basicRequest
      .get(uri"http://www.omdbapi.com/?apikey=$apiKey&t=$title")
      .response(asStringAlways)
    
    Future {
      val response = request.send(backend)
      response.body match {
        case body if body.contains("\"Response\":\"True\"") =>
          decode[OMDbMovie](body).toOption.map(m => Movie(m.Title, m.Year, m.Plot, m.imdbID, m.Genre))
        case _ => None
      }
    }
  }
}
