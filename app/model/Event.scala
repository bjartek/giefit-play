package models

import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._
import play.api.data.validation.Constraints._

import reactivemongo.bson._
import reactivemongo.bson.handlers._
import org.joda.time.format.ISODateTimeFormat
import play.api.libs.Crypto

case class Event(
                    id: Option[BSONObjectID],
                    title: String,
                    content: String,
                    owner: User,
                    creationDate: Option[DateTime],
                    eventDate: Option[DateTime]
                    )    {

  def prettyDate = eventDate.map(x => x.toString(Event.dateFormat)).getOrElse(" ukjent ")
}


object Event {

  val dateFormat = ISODateTimeFormat.date()

  def formWithEmail(email:String, name:String) = {
   form.fill(Event(None, "", "", User.fromForm(name, email), None, None))
  }

  implicit object EventBSONReader extends BSONReader[Event] {
    def fromBSON(document: BSONDocument) :Event = {
      val doc = document.toTraversable
      Event(
        doc.getAs[BSONObjectID]("_id"),
        doc.getAs[BSONString]("title").get.value,
        doc.getAs[BSONString]("content").get.value,
        User.UserBSOnReader.fromBSON(doc.getAs[BSONDocument]("owner").get),
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
        "owner" -> User.UserBSONWriter.toBSON(event.owner),
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
      "email" -> nonEmptyText,
      "creationDate" -> optional(of[Long]),
      "eventDate" -> optional(of[String])
    ) { (id, title, content, owner, email, creationDate, updateDate) =>
      Event(
        id.map(new BSONObjectID(_)),
        title,
        content,
        User.fromForm(owner, email),
        creationDate.map(new DateTime(_)),
        updateDate.map(DateTime.parse(_, dateFormat)))
    } { event =>
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
