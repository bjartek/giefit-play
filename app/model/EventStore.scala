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
import java.util.concurrent.TimeUnit
import reactivemongo.core.commands.GetLastError
import scala.util.{Failure, Success}

object EventStore {

  val db = ReactiveMongoPlugin.db
  val eventCollection = db("events")
  implicit val reader = Event.EventBSONReader



  def addItem(event:Event, item:Item) = {

    //This will not work for me.
    Logger.info("adding item to event " + event.id)
    val find = BSONDocument("$query" -> BSONDocument("_id" -> event.id.get))

    val itemDoc = Item.ItemBSONWriter.toBSON(item)
    val arr = BSONArray(itemDoc)
    val subdoc = BSONDocument("items" -> BSONArray(arr.makeBuffer))
    val update = BSONDocument("push" -> subdoc)

    Logger.info(BSONDocument.pretty(update))

     eventCollection.update(find, update, GetLastError())

  }


  def byOwnerEmail(email:String) : BSONDocument = {
    BSONDocument("$query" -> BSONDocument("owner.email" -> new BSONString(email)))

  }



  def findNameForUser(s: String): String =  {
    val res = findOneByQueryBlock(byOwnerEmail(s))
    Logger.info("Finding user for email " + res.owner.toString)

    res.owner.name
  }


  def findForUser(hash:String) : Future[List[Event]] = {

    Logger.info("finding events for user with hash " + hash)
    findByQuery(byOwnerEmail(hash))

  }

  def insert(event: Event) = {
    eventCollection.insert(event, GetLastError())
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
