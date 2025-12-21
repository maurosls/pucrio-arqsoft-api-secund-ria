package service

import domain.Movie
import database.Database
import client.OMDbClient
import scala.concurrent.{ExecutionContext, Future}

object MovieApiService {
  
  def getMovie(title: String)(implicit ec: ExecutionContext): Future[Option[Movie]] = {
    OMDbClient.getMovie(title)
  }
}
