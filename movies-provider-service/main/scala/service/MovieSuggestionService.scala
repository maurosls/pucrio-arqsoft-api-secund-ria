package service

import domain.Movie
import database.Database
import client.OMDbClient
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

object MovieSuggestionService {
  
  def getSuggestionForUser(userId: String)(implicit ec: ExecutionContext): Future[Option[Movie]] = {
    val userRatedMovies = Database.getAllRatedMoviesByUser(userId)
    val userPreferredGenres = Database.getUserPreferredGenres(userId)
    
    if (userPreferredGenres.nonEmpty) {
      fetchMovieByGenre(userPreferredGenres, userRatedMovies)
    } else {
      fetchUnratedPopularMovie(userRatedMovies)
    }
  }
  
  private def fetchMovieByGenre(preferredGenres: List[String], userRatedMovies: List[String])(implicit ec: ExecutionContext): Future[Option[Movie]] = {
    val genreMovies = Map(
      "sci-fi" -> List("Blade Runner 2049", "The Matrix", "Dune", "Ex Machina", "Arrival"),
      "action" -> List("Mad Max: Fury Road", "John Wick", "The Dark Knight", "Mission: Impossible", "Die Hard"),
      "drama" -> List("The Shawshank Redemption", "Forrest Gump", "The Godfather", "Schindler's List", "12 Years a Slave"),
      "comedy" -> List("The Grand Budapest Hotel", "Superbad", "Anchorman", "The Hangover", "Borat"),
      "horror" -> List("Get Out", "Hereditary", "The Conjuring", "A Quiet Place", "It Follows"),
      "thriller" -> List("Gone Girl", "Se7en", "Zodiac", "Shutter Island", "No Country for Old Men")
    )
    
    def tryGenreMovies(genres: List[String]): Future[Option[Movie]] = {
      if (genres.isEmpty) {
        fetchUnratedPopularMovie(userRatedMovies)
      } else {
        val genre = genres.head.toLowerCase
        val movies = genreMovies.find(_._1.contains(genre)).map(_._2).getOrElse(List.empty)
        
        if (movies.nonEmpty) {
          val randomTitle = movies(Random.nextInt(movies.length))
          MovieCacheService.getMovie(randomTitle).flatMap {
            case Some(movie) if !userRatedMovies.contains(movie.imdbID) => 
              Future.successful(Some(movie))
            case _ => 
              tryGenreMovies(genres.tail)
          }
        } else {
          tryGenreMovies(genres.tail)
        }
      }
    }
    
    tryGenreMovies(Random.shuffle(preferredGenres))
  }
  
  private def fetchUnratedPopularMovie(userRatedMovies: List[String])(implicit ec: ExecutionContext): Future[Option[Movie]] = {
    val popularTitles = List("The Shawshank Redemption", "The Godfather", "Pulp Fiction", "Inception", "Interstellar")
    
    // Try to find a popular movie the user hasn't rated
    def tryPopularMovie(titles: List[String]): Future[Option[Movie]] = {
      if (titles.isEmpty) {
        Future.successful(None)
      } else {
        val title = titles.head
        MovieCacheService.getMovie(title).flatMap {
          case Some(movie) if !userRatedMovies.contains(movie.imdbID) => 
            Future.successful(Some(movie))
          case _ => 
            tryPopularMovie(titles.tail)
        }
      }
    }
    
    tryPopularMovie(Random.shuffle(popularTitles))
  }
}
