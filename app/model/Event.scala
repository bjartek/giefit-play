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
import play.Logger
import model.Item

case class Event(
                    id: Option[BSONObjectID],
                    title: String,
                    content: String,
                    owner: User,
                    creationDate: Option[DateTime],
                    eventDate: Option[DateTime],
                    guests:List[User] = List(),
                    items:List[Item] = List())    {

  def prettyDate = eventDate.map(x => x.toString(Event.dateFormat)).getOrElse(" ukjent ")
}


object Event {

  val dateFormat = ISODateTimeFormat.date()

  def formWithEmail(name:String, email:String) = {
   form.fill(Event(None, "", "", User.fromForm(name, email), None, None, List(), List()))
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
        doc.getAs[BSONDateTime]("eventDate").map(dt => new DateTime(dt.value)),
        doc.getAs[BSONArray]("guests").get.toTraversable.toList.map { bsonUser =>
          User.UserBSOnReader.fromBSON(bsonUser.asInstanceOf[TraversableBSONDocument])
        },
        doc.getAs[BSONArray]("items").get.toTraversable.toList.map { bsonItem =>
          Item.ItemBSOnReader.fromBSON(bsonItem.asInstanceOf[TraversableBSONDocument])
        }
      )
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
        "eventDate" -> event.eventDate.map(date => BSONDateTime(date.getMillis)),
        "guests" -> BSONArray(event.guests.map {
          guest => User.UserBSONWriter.toBSON(guest)
        }: _*),
        "items" -> BSONArray(event.items.map {
          item => Item.ItemBSONWriter.toBSON(item)
        }: _*)
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
    ) { (id, title, content, owner, email, creationDate, updateDate) => {
        Logger.info(content + " " + owner)
      Event(
        id.map(new BSONObjectID(_)),
        title,
        content,
        User.fromForm(owner, email),
        creationDate.map(new DateTime(_)),
        updateDate.map(DateTime.parse(_, dateFormat)), List(), List())
      }
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
