package model

import reactivemongo.bson.handlers.{BSONWriter, BSONReader}
import reactivemongo.bson._
import play.api.data.Form
import play.api.data.Forms._
import reactivemongo.bson.BSONString
import scala.Some
import java.net.{HttpURLConnection, MalformedURLException, URL}
import play.api.Logger


case class Item(name: String,
                url: Option[URL] = None,
                status: String = "Active",
                fulfilled: Option[String] = None) {

  def webTitle = {
    url.map(u => """<a href="%s">%s</a>""".format(u, name)).getOrElse(name)
  }
}

object Item {


  implicit object ItemBSOnReader extends BSONReader[Item] {
    def fromBSON(document: BSONDocument): Item = {
      val doc = document.toTraversable
      val helper = new BsonReaderHelper(doc)
      Item(
        doc.getAs[BSONString]("name").get.value,
        helper.optionalUrl("url"),
        doc.getAs[BSONString]("status").get.value,
        doc.getAs[BSONString]("fulfilled").map(_.value)
      )

    }
  }

  implicit object ItemBSONWriter extends BSONWriter[Item] {
    def toBSON(user: Item) = {
      BSONDocument(
        "name" -> BSONString(user.name),
        "url" -> user.url.map(url => BSONString(url.toString)),
        "status" -> BSONString(user.status),
        "fulfilled" -> user.fulfilled.map(BSONString(_)))
    }
  }


  def shouldBeUrl(s: String): Boolean = {

    if (s.isEmpty) {
      true
    } else {
      try {
        val foo = new URL(s)
        true
      } catch {
        case e: MalformedURLException => false
      }
    }
  }

  def createUrl(s: String): Option[URL]  = {
    if (s.isEmpty) None else Some(new URL(s))
  }


  val form = Form(
    mapping(
      "name" -> nonEmptyText,
      "url" -> text.verifying("Should be a valid URL", shouldBeUrl(_))){
      (name, url) => Item(name, createUrl(url))
    } {
      item => Some((item.name, item.url.getOrElse("").toString))
    })

}