package model
import play.api.Play.current
import play.modules.reactivemongo._
import concurrent._
import models.User


import reactivemongo.bson._
import reactivemongo.bson.handlers.DefaultBSONHandlers.DefaultBSONDocumentWriter
import reactivemongo.bson.handlers.DefaultBSONHandlers.DefaultBSONReaderHandler
import ExecutionContext.Implicits.global
import play.api.mvc.Cookies

object UserStore {
  val db = ReactiveMongoPlugin.db
  val userCollection = db("users")

  def findById(id:String) : Future[User] = {
    implicit val reader = User.UserBSOnReader

    val query = BSONDocument("$query" -> BSONDocument("_id" -> new BSONObjectID(id)))
    userCollection.find(query).headOption().filter(_.isDefined).map(_.get)

  }

  def findUserFromCookie(cookies:Cookies): Future[User] = {
    for {
      c <- future { cookies.get("user")}.filter(_.isDefined).map(_.get)
      u <- findById(c.value)
    } yield u

  }
}
