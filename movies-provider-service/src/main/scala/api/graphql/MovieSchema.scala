package api.graphql

import sangria.schema._
import sangria.macros.derive._
import domain.Movie
import service.{MovieApiService, MovieSuggestionService}
import scala.concurrent.{ExecutionContext, Future}

object MovieSchema {
  
  implicit val MovieType: ObjectType[Unit, Movie] = deriveObjectType[Unit, Movie](
    ObjectTypeDescription("A movie from OMDb API")
  )
  
  val QueryType = ObjectType(
    "Query",
    fields[MovieResolver, Unit](
      Field("movie", OptionType(MovieType),
        description = Some("Get movie by title"),
        arguments = Argument("title", StringType, description = "Movie title") :: Nil,
        resolve = ctx => ctx.ctx.getMovie(ctx.arg[String]("title"))
      ),
      Field("suggestion", OptionType(MovieType),
        description = Some("Get movie suggestion for user"),
        arguments = Argument("userId", StringType, description = "User ID") :: Nil,
        resolve = ctx => ctx.ctx.getSuggestion(ctx.arg[String]("userId"))
      )
    )
  )
  
  val schema = Schema(QueryType)
}

class MovieResolver(implicit ec: ExecutionContext) {
  def getMovie(title: String): Future[Option[Movie]] = {
    MovieApiService.getMovie(title)
  }
  
  def getSuggestion(userId: String): Future[Option[Movie]] = {
    MovieSuggestionService.getSuggestionForUser(userId)
  }
}
