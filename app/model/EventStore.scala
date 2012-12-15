package model
import play.api.Play.current
import play.modules.reactivemongo._
import concurrent._
import duration.Duration

import reactivemongo.bson._
import reactivemongo.bson.handlers.DefaultBSONHandlers.DefaultBSONDocumentWriter
import reactivemongo.bson.handlers.DefaultBSONHandlers.DefaultBSONReaderHandler
import ExecutionContext.Implicits.global
import play.Logger
import java.util.concurrent.TimeUnit
import reactivemongo.core.commands.GetLastError

object EventStore {


  def updateItem(event: Event, value: String, item: Item) = {
    val find = BSONDocument("_id" -> event.id.get, "items.name" -> BSONString(value))
    val update = BSONDocument("$set" -> BSONDocument("items.$" -> Item.ItemBSONWriter.toBSON(item)))
    eventCollection.update(find, update, GetLastError())
  }


  def updateGuest(event: Event, value: String, guest: User) = {
    val find = BSONDocument("_id" -> event.id.get, "guests.email" -> BSONString(value))
    val update = BSONDocument("$set" -> BSONDocument("guests.$" -> User.UserBSONWriter.toBSON(guest)))
    eventCollection.update(find, update, GetLastError())
  }


  val db = ReactiveMongoPlugin.db
  val eventCollection = db("events")
  implicit val reader = Event.EventBSONReader



  def updateEvent(stored: Event, changed: Event) =  {

    val toBeStoredEvent = stored.copy(
      title = changed.title,
      content = changed.content,
      owner = changed.owner,
      creationDate = changed.creationDate,
      eventDate = changed.eventDate
      )

    Logger.info(toBeStoredEvent.toString)
    val find = BSONDocument("_id" -> toBeStoredEvent.id.get)

    eventCollection.update(find, toBeStoredEvent, GetLastError())
  }


  def addItem(event:Event, item:Item) = {

    Logger.info("adding item " + item.toString  + " to event " + event.id)
    val find = BSONDocument("_id" -> event.id.get)

    val itemDoc = Item.ItemBSONWriter.toBSON(item)
    val arr = BSONArray(itemDoc)
    val subdoc = BSONDocument("items" -> arr)
    val update = BSONDocument("$pushAll" -> subdoc)


     eventCollection.update(find, update, GetLastError())

  }

  def addGuest(event: Event, guest: User) =  {
    Logger.info("adding guest " + guest.toString + " to event " + event.id)
    val find = BSONDocument("_id" -> event.id.get)

    val itemDoc = User.UserBSONWriter.toBSON(guest)
    val arr = BSONArray(itemDoc)
    val subdoc = BSONDocument("guests" -> arr)
    val update = BSONDocument("$pushAll" -> subdoc)

    eventCollection.update(find, update, GetLastError())
  }


  def byOwnerEmail(email:String) : BSONDocument = {
    BSONDocument("owner.email" -> new BSONString(email))

  }





  def findForUser(hash:String) : Future[List[Event]] = {

    Logger.info("finding events for user with hash " + hash)
    findByQuery(byOwnerEmail(hash))

  }

  def insert(event: Event) = {
    eventCollection.insert(event, GetLastError())
  }

  def findById(id:String) : Future[Event] = {
    val query = BSONDocument("_id" -> new BSONObjectID(id))
    findOneByQuery(query)
  }


  def findOneByQuery(query:BSONDocument): Future[Event] = {
    eventCollection.find(query).headOption().filter(_.isDefined).map(_.get)
  }


  def findByQuery(query:BSONDocument): Future[List[Event]] = {
    eventCollection.find(query).toList.filter(_.nonEmpty)
  }

}
