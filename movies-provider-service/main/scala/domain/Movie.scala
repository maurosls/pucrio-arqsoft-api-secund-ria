package domain

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._

case class Movie(title: String, year: String, plot: String, imdbID: String, genre: String = "")
case class MovieRequest(title: String)
case class MovieResponse(title: String, year: String, plot: String, imdbID: String, genre: String = "")

object MovieResponse {
  implicit val encoder: Encoder[MovieResponse] = deriveEncoder
  implicit val decoder: Decoder[MovieResponse] = deriveDecoder
}
