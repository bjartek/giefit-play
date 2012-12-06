package models

import reactivemongo.bson._
import reactivemongo.bson.handlers._

case class User(id: Option[BSONObjectID],email: String)

object User {

  implicit object UserBSOnReader extends BSONReader[User] {
    def fromBSON(document: BSONDocument) :User = {
      val doc = document.toTraversable
      User(
        doc.getAs[BSONObjectID]("_id"),
        doc.getAs[BSONString]("email").get.value
      )

    }
  }
  implicit object UserBSONWriter extends BSONWriter[User] {
    def toBSON(user: User) = {
      BSONDocument(
        "_id" -> user.id.getOrElse(BSONObjectID.generate),
        "email" -> BSONString(user.email))

    }
  }

}