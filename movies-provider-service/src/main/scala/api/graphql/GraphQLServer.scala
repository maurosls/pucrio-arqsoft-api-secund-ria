package api.graphql

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.Http.ServerBinding
import sangria.execution._
import sangria.parser.QueryParser
import sangria.marshalling.circe._
import io.circe.Json
import io.circe.generic.auto._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Failure}
import scala.io.StdIn

case class GraphQLRequest(query: String, variables: Option[Json] = None)

object GraphQLServer {

  def executeGraphQL(query: String, variables: Option[Json], movieResolver: MovieResolver)(implicit ec: ExecutionContext): Future[Json] = {
    QueryParser.parse(query) match {
      case Success(queryAst) =>
        Executor.execute(
          schema = MovieSchema.schema,
          queryAst = queryAst,
          userContext = movieResolver,
          variables = variables.getOrElse(Json.obj())
        )
      case Failure(error) =>
        Future.successful(Json.obj("errors" -> Json.arr(Json.obj("message" -> Json.fromString(error.getMessage)))))
    }
  }

  def createRoute(movieResolver: MovieResolver)(implicit ec: ExecutionContext): Route = 
    path("graphql") {
      post {
        entity(as[GraphQLRequest]) { request =>
          onComplete(executeGraphQL(request.query, request.variables, movieResolver)) {
            case Success(result) => complete(result)
            case Failure(error) => complete(StatusCodes.BadRequest -> Json.obj("errors" -> Json.arr(Json.obj("message" -> Json.fromString(error.getMessage)))))
          }
        }
      } ~
      get {
        // GraphQL Playground/introspection endpoint
        complete("GraphQL endpoint is ready. Send POST requests with query.")
      }
    }

  def startService()(implicit system: ActorSystem, ec: ExecutionContext): Future[ServerBinding] = {
    val movieResolver = new MovieResolver()(ec)
    Http().newServerAt("localhost", 8081).bind(createRoute(movieResolver))
  }

  // Keep the main method for standalone execution
  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem("graphql-server")
    implicit val executionContext: ExecutionContext = system.dispatcher

    val bindingFuture = startService()
    println("GraphQL server online at http://localhost:8081/graphql")
    println("Example query: { movie(title: \"Inception\") { title year plot imdbID } }")
    StdIn.readLine()
    bindingFuture.flatMap(_.unbind()).onComplete(_ => system.terminate())
  }
}
