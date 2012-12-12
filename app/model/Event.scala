package models

import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._
import play.api.data.validation.Constraints._

import reactivemongo.bson._
import reactivemongo.bson.handlers._
import org.joda.time.format.ISODateTimeFormat
import play.Logger
import model.Item
import reactivemongo.bson.BSONDateTime
import reactivemongo.bson.BSONString
import scala.Some

case class Event(
                  id: Option[BSONObjectID],
                  title: String,
                  content: String,
                  owner: User,
                  creationDate: Option[DateTime],
                  eventDate: Option[DateTime],
                  guests: List[User] = List(),
                  items: List[Item] = List()) {

  def prettyDate = eventDate.map(x => x.toString(Event.dateFormat)).getOrElse(" ukjent ")

}


class BsonReaderHelper(doc: TraversableBSONDocument) {
  def document[T](fieldName: String)(implicit reader: BSONReader[T]) = {
    reader.fromBSON(doc.getAs[BSONDocument](fieldName).get)
  }

  def listDocument[T](fieldName: String)(implicit reader: BSONReader[T]) = {
    doc.getAs[BSONArray](fieldName).get.toTraversable.toList.map {
      t =>
        reader.fromBSON(t.asInstanceOf[TraversableBSONDocument])
    }
  }


  def date(fieldName: String) = doc.getAs[BSONDateTime](fieldName).map(dt => new DateTime(dt.value))

}

class BsonWriterHelper() {
  def listDocument[T](xs: List[T])(implicit writer: BSONWriter[T]) = {
    BSONArray(xs.map {
      t => writer.toBSON(t)
    }: _*)
  }

  def document[T](doc: T)(implicit writer: BSONWriter[T]) = writer.toBSON(doc)

}

object Event {

  val dateFormat = ISODateTimeFormat.date()

  implicit val userWriter = User.UserBSONWriter
  implicit val itemWriter = Item.ItemBSONWriter

  implicit val userReader = User.UserBSOnReader
  implicit val itemReader = Item.ItemBSOnReader

  def formWithEmail(name: String, email: String) = {
    form.fill(Event(None, "", "", User.fromForm(name, email), None, None, List(), List()))
  }

  implicit object EventBSONReader extends BSONReader[Event] {
    def fromBSON(document: BSONDocument): Event = {
      val doc = document.toTraversable
      val helper = new BsonReaderHelper(doc)
      Event(
        doc.getAs[BSONObjectID]("_id"),
        doc.getAs[BSONString]("title").get.value,
        doc.getAs[BSONString]("content").get.value,
        helper.document[User]("owner"),
        doc.getAs[BSONDateTime]("creationDate").map(dt => new DateTime(dt.value)),
        doc.getAs[BSONDateTime]("eventDate").map(dt => new DateTime(dt.value)),
        helper.listDocument[User]("guests"),
        helper.listDocument[Item]("items")
      )
    }
  }

  implicit object EventBSONWriter extends BSONWriter[Event] {


    def toBSON(event: Event) = {

      val helper = new BsonWriterHelper()
      BSONDocument(
        "_id" -> event.id.getOrElse(BSONObjectID.generate),
        "title" -> BSONString(event.title),
        "content" -> BSONString(event.content),
        "owner" -> helper.document(event.owner),
        "creationDate" -> event.creationDate.map(date => BSONDateTime(date.getMillis)),
        "eventDate" -> event.eventDate.map(date => BSONDateTime(date.getMillis)),
        "guests" -> helper.listDocument(event.guests),
        "items" -> helper.listDocument(event.items)
      )
    }
  }


  val form = Form(
    mapping(
      "id" -> optional(of[String] verifying pattern(
        """[a-fA-F0-9]{24}""".r,
        "constraint.objectId",
        "error.objectId")),
      "title" -> nonEmptyText,
      "content" -> text,
      "owner" -> nonEmptyText,
      "email" -> nonEmptyText,
      "creationDate" -> optional(of[Long]),
      "eventDate" -> optional(of[String])
    ) {
      (id, title, content, owner, email, creationDate, eventDate) => {
        Logger.info(content + " " + owner)
        Event(
          id.map(new BSONObjectID(_)).orElse(Some(BSONObjectID.generate)),
          title,
          content,
          User.fromForm(owner, email),
          creationDate.map(new DateTime(_)).orElse(Some(new DateTime())),
          eventDate.map(DateTime.parse(_, dateFormat)),
          List(),
          List())
      }
    } {
      event =>
        Some(
          (event.id.map(_.stringify),
            event.title,
            event.content,
            event.owner.name,
            event.owner.email,
            event.creationDate.map(_.getMillis),
            event.eventDate.map(_.toString(dateFormat))))
    })

}
