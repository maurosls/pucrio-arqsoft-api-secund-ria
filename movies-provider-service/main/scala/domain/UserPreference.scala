package domain

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._

case class UserPreference(id: Long, userId: String, movieId: String, rating: Int)

object UserPreference {
  implicit val encoder: Encoder[UserPreference] = deriveEncoder
  implicit val decoder: Decoder[UserPreference] = deriveDecoder
}
