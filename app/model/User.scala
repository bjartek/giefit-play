package models


import reactivemongo.bson._
import reactivemongo.bson.handlers._
import play.api.libs.Crypto
import play.Logger

case class User(name:String, email: String)  {

  lazy val hash = Crypto.encryptAES(email)
}

object User {

  def fromForm(name:String, email:String):User = {
    Logger.info("Name %s and email %s".format(name, email))
    User(name, email)
  }

  implicit object UserBSOnReader extends BSONReader[User] {
    def fromBSON(document: BSONDocument) :User = {
      val doc = document.toTraversable
      User(
        doc.getAs[BSONString]("name").get.value,
        doc.getAs[BSONString]("email").get.value
      )

    }
  }
  implicit object UserBSONWriter extends BSONWriter[User] {
    def toBSON(user: User) = {
      BSONDocument(
        "name" -> BSONString(user.name),
        "email" -> BSONString(user.email))

    }
  }

}