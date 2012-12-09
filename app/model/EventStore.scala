package model
import play.api.Play.current
import play.modules.reactivemongo._
import concurrent._
import duration.Duration
import models.{Event, User}

import reactivemongo.bson._
import reactivemongo.bson.handlers.DefaultBSONHandlers.DefaultBSONDocumentWriter
import reactivemongo.bson.handlers.DefaultBSONHandlers.DefaultBSONReaderHandler
import ExecutionContext.Implicits.global
import play.Logger
import play.api.libs.Crypto
import java.util.concurrent.TimeUnit

object EventStore {


  def byOwnerHash(hash:String) : BSONDocument = {
    BSONDocument("$query" -> BSONDocument("owner.hash" -> new BSONString(hash)))

  }

  def findNameForUser(s: String): String =  {

    val res = findOneByQueryBlock(byOwnerHash(Crypto.sign(s)))
    res.owner.name
  }


  val db = ReactiveMongoPlugin.db
  val eventCollection = db("events")
  implicit val reader = Event.EventBSONReader



  def findForUser(hash:String) : Future[List[Event]] = {

    Logger.info("finding events for user with hash " + hash)
    findByQuery(byOwnerHash(hash))

  }

  def insert(event: Event) = {
    eventCollection.insert(event)
  }

  def findById(id:String) : Future[Event] = {
    val query = BSONDocument("$query" -> BSONDocument("_id" -> new BSONObjectID(id)))
    findOneByQuery(query)
  }


  def findOneByQuery(query:BSONDocument): Future[Event] = {
    eventCollection.find(query).headOption().filter(_.isDefined).map(_.get)
  }

  def findOneByQueryBlock(query:BSONDocument) : Event = {
    val res = findOneByQuery(query)

    Await.result(res, Duration(2, TimeUnit.SECONDS))

  }

  def findByQuery(query:BSONDocument): Future[List[Event]] = {
    eventCollection.find(query).toList.filter(_.nonEmpty)
  }

}
