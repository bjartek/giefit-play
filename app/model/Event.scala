package models

import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._
import play.api.data.validation.Constraints._

import reactivemongo.bson._
import reactivemongo.bson.handlers._
import org.joda.time.format.ISODateTimeFormat
case class Event(
                    id: Option[BSONObjectID],
                    title: String,
                    content: String,
                    owner: String,
                    creationDate: Option[DateTime],
                    eventDate: Option[DateTime]
                    )

object Event {

  val dateFormat = ISODateTimeFormat.date()

  def formWithEmail(email:String) = {
   form.fill(Event(None, "", "", email, None, None))
  }

  implicit object EventBSONReader extends BSONReader[Event] {
    def fromBSON(document: BSONDocument) :Event = {
      val doc = document.toTraversable
      Event(
        doc.getAs[BSONObjectID]("_id"),
        doc.getAs[BSONString]("title").get.value,
        doc.getAs[BSONString]("content").get.value,
        doc.getAs[BSONString]("owner").get.value,
        doc.getAs[BSONDateTime]("creationDate").map(dt => new DateTime(dt.value)),
        doc.getAs[BSONDateTime]("eventDate").map(dt => new DateTime(dt.value)))
    }
  }
  implicit object EventBSONWriter extends BSONWriter[Event] {
    def toBSON(event: Event) = {
      BSONDocument(
        "_id" -> event.id.getOrElse(BSONObjectID.generate),
        "title" -> BSONString(event.title),
        "content" -> BSONString(event.content),
        "owner" -> BSONString(event.owner),
        "creationDate" -> event.creationDate.map(date => BSONDateTime(date.getMillis)),
        "eventDate" -> event.eventDate.map(date => BSONDateTime(date.getMillis)))
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
      "creationDate" -> optional(of[Long]),
      "eventDate" -> optional(of[String])
    ) { (id, title, content, publisher, creationDate, updateDate) =>
      Event(
        id.map(new BSONObjectID(_)),
        title,
        content,
        publisher,
        creationDate.map(new DateTime(_)),
        updateDate.map(DateTime.parse(_, dateFormat)))
    } { event =>
      Some(
        (event.id.map(_.stringify),
          event.title,
          event.content,
          event.owner,
          event.creationDate.map(_.getMillis),
          event.eventDate.map(_.toString(dateFormat))))
    })
}