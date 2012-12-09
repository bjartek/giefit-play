package models

import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._
import play.api.data.validation.Constraints._

import reactivemongo.bson._
import reactivemongo.bson.handlers._
import play.api.libs.Crypto

case class User(name:String, email: String, hash:String)

object User {

  def fromForm(name:String, email:String):User = {
    User(name, email, Crypto.sign(email))
  }
  implicit object UserBSOnReader extends BSONReader[User] {
    def fromBSON(document: BSONDocument) :User = {
      val doc = document.toTraversable
      User(
        doc.getAs[BSONString]("name").get.value,
        doc.getAs[BSONString]("email").get.value,
        doc.getAs[BSONString]("hash").get.value
      )

    }
  }
  implicit object UserBSONWriter extends BSONWriter[User] {
    def toBSON(user: User) = {
      BSONDocument(
        "name" -> BSONString(user.email),
        "email" -> BSONString(user.email),
        "hash" -> BSONString(user.hash))

    }
  }

}