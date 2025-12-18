package database

import domain.{UserPreference, Movie}
import scalikejdbc._

object Database {
  Class.forName("org.h2.Driver")
  ConnectionPool.singleton("jdbc:h2:mem:test", "user", "pass")

  def init(): Unit = {
    DB autoCommit { implicit session =>
      sql"""
        CREATE TABLE IF NOT EXISTS user_preferences (
          id BIGINT AUTO_INCREMENT PRIMARY KEY,
          user_id VARCHAR(255),
          movie_id VARCHAR(255),
          rating INT
        )
      """.execute.apply()

      sql"""
        CREATE TABLE IF NOT EXISTS movies (
          id BIGINT AUTO_INCREMENT PRIMARY KEY,
          title VARCHAR(255) UNIQUE,
          movie_year VARCHAR(255),
          plot TEXT,
          imdb_id VARCHAR(255),
          genre VARCHAR(255)
        )
      """.execute.apply()
    }
  }
  
  def addPreference(userId: String, movieId: String, rating: Int): UserPreference = {
    DB autoCommit { implicit session =>
      val id = sql"INSERT INTO user_preferences (user_id, movie_id, rating) VALUES (${userId}, ${movieId}, ${rating})"
        .updateAndReturnGeneratedKey.apply()
      UserPreference(id, userId, movieId, rating)
    }
  }
  
  def getUserPreferences(userId: String): List[UserPreference] = {
    DB readOnly { implicit session =>
      sql"SELECT * FROM user_preferences WHERE user_id = ${userId}"
        .map(rs => UserPreference(rs.long("id"), rs.string("user_id"), rs.string("movie_id"), rs.int("rating")))
        .list.apply()
    }
  }

  def getMovieByTitle(title: String): Option[Movie] = {
    DB readOnly { implicit session =>
      sql"SELECT * FROM movies WHERE title = ${title}"
        .map(rs => Movie(rs.string("title"), rs.string("movie_year"), rs.string("plot"), rs.string("imdb_id"), rs.stringOpt("genre").getOrElse("")))
        .single.apply()
    }
  }

  def saveMovie(movie: Movie): Movie = {
    DB autoCommit { implicit session =>
      sql"MERGE INTO movies (title, movie_year, plot, imdb_id, genre) KEY(title) VALUES (${movie.title}, ${movie.year}, ${movie.plot}, ${movie.imdbID}, ${movie.genre})"
        .update.apply()
      movie
    }
  }

  def getAllMovies(): List[Movie] = {
    DB readOnly { implicit session =>
      sql"SELECT * FROM movies"
        .map(rs => Movie(rs.string("title"), rs.string("movie_year"), rs.string("plot"), rs.string("imdb_id"), rs.stringOpt("genre").getOrElse("")))
        .list.apply()
    }
  }

  def getHighRatedMoviesByUser(userId: String, minRating: Int = 4): List[String] = {
    DB readOnly { implicit session =>
      sql"SELECT movie_id FROM user_preferences WHERE user_id = ${userId} AND rating >= ${minRating}"
        .map(rs => rs.string("movie_id"))
        .list.apply()
    }
  }

  def getAllRatedMoviesByUser(userId: String): List[String] = {
    DB readOnly { implicit session =>
      sql"SELECT movie_id FROM user_preferences WHERE user_id = ${userId}"
        .map(rs => rs.string("movie_id"))
        .list.apply()
    }
  }

  def getUserPreferredGenres(userId: String, minRating: Int = 4): List[String] = {
    DB readOnly { implicit session =>
      sql"""
        SELECT DISTINCT m.genre 
        FROM movies m 
        JOIN user_preferences up ON m.imdb_id = up.movie_id 
        WHERE up.user_id = ${userId} AND up.rating >= ${minRating} AND m.genre != ''
      """
        .map(rs => rs.string("genre"))
        .list.apply()
    }
  }
}
