import api.http.MovieService
import api.graphql.GraphQLServer
import database.Database
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import scala.concurrent.ExecutionContext
import scala.util.{Success, Failure}

object Main extends App {
  implicit val system: ActorSystem = ActorSystem("movies-app")
  implicit val executionContext: ExecutionContext = system.dispatcher
  
  // Initialize database
  Database.init()
  println("Database initialized")
  
  // Start services
  val movieServiceBinding = MovieService.startService()
  val graphqlServiceBinding = GraphQLServer.startService()
  
  movieServiceBinding.onComplete {
    case Success(binding) => 
      println(s"Movie HTTP API started at http://${binding.localAddress}")
    case Failure(ex) => 
      println(s"Failed to start Movie HTTP API: ${ex.getMessage}")
  }
  
  graphqlServiceBinding.onComplete {
    case Success(binding) => 
      println(s"GraphQL API started at http://${binding.localAddress}")
    case Failure(ex) => 
      println(s"Failed to start GraphQL API: ${ex.getMessage}")
  }
  
  println("Services starting...")
  println("- Movie HTTP API: http://localhost:9090/movie/{title}")
  println("- GraphQL API: http://localhost:8081/graphql")
  println("Press ENTER to stop...")
  
  scala.io.StdIn.readLine()
  
  // Shutdown
  movieServiceBinding.foreach(_.unbind())
  graphqlServiceBinding.foreach(_.unbind())
  system.terminate()
}
