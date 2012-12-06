package model
import play.api.Play.current
import play.modules.reactivemongo._
import concurrent._
import models.{Event, User}

import reactivemongo.bson._
import reactivemongo.bson.handlers.DefaultBSONHandlers.DefaultBSONDocumentWriter
import reactivemongo.bson.handlers.DefaultBSONHandlers.DefaultBSONReaderHandler
import ExecutionContext.Implicits.global

object EventStore {
  val db = ReactiveMongoPlugin.db
  val eventCollection = db("events")

  def findEventsForUser(user:User) : Future[List[Event]] = {

      implicit val reader = Event.EventBSONReader
      val query = BSONDocument("$query" -> BSONDocument("owner" -> new BSONString(user.email)))
      eventCollection.find(query).toList

  }


}
