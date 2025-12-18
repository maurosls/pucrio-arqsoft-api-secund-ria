package api.http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.Http.ServerBinding
import io.circe.generic.auto._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import domain.{MovieResponse, UserPreference}
import database.Database
import service.{MovieApiService, MovieSuggestionService}
import scala.concurrent.{ExecutionContext, Future}
import scala.io.StdIn

object MovieService {

  def getMovie(title: String)(implicit ec: ExecutionContext): Future[MovieResponse] = {
    MovieApiService.getMovie(title).map {
      case Some(movie) => MovieResponse(movie.title, movie.year, movie.plot, movie.imdbID, movie.genre)
      case None => MovieResponse("", "", "Movie not found", "", "")
    }
  }

  def getSuggestion(userId: String)(implicit ec: ExecutionContext): Future[MovieResponse] = {
    MovieSuggestionService.getSuggestionForUser(userId).map {
      case Some(movie) => MovieResponse(movie.title, movie.year, movie.plot, movie.imdbID, movie.genre)
      case None => MovieResponse("", "", "No suggestion available", "", "")
    }
  }

  def createRoute()(implicit ec: ExecutionContext): Route = 
    path("movie" / "suggestion") {
      get {
        parameter("userId") { userId =>
          onComplete(getSuggestion(userId)) {
            case scala.util.Success(movie) => complete(movie)
            case scala.util.Failure(_) => complete(MovieResponse("", "", "Error getting suggestion", ""))
          }
        }
      }
    } ~
    path("movie" / Segment) { title =>
      get {
        onComplete(getMovie(title)) {
          case scala.util.Success(movie) => complete(movie)
          case scala.util.Failure(_) => complete(MovieResponse("", "", "Error", ""))
        }
      }
    } ~
    pathPrefix("preferences") {
      pathEndOrSingleSlash {
        post {
          entity(as[UserPreference]) { preference =>
            try {
              val saved = Database.addPreference(preference.userId, preference.movieId, preference.rating)
              complete((201, saved))
            } catch {
              case _: Exception => complete((500, "Failed to save preference"))
            }
          }
        }
      } ~
      path(Segment) { userId =>
        get {
          try {
            val prefs = Database.getUserPreferences(userId)
            complete(prefs)
          } catch {
            case _: Exception => complete((500, "Failed to get preferences"))
          }
        }
      }
    }

  def startService()(implicit system: ActorSystem, ec: ExecutionContext): Future[ServerBinding] = {
    Http().newServerAt("localhost", 9090).bind(createRoute())
  }

  // Keep the main method for standalone execution
  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem("movie-service")
    implicit val executionContext: ExecutionContext = system.dispatcher

    Database.init()

    val bindingFuture = startService()
    println("Movie service online at http://localhost:9090/movie/{title}")
    StdIn.readLine()
    bindingFuture.flatMap(_.unbind()).onComplete(_ => system.terminate())
  }
}
