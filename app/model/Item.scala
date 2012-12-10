package model

import reactivemongo.bson.handlers.{BSONWriter, BSONReader}
import reactivemongo.bson.{BSONString, BSONDocument}


case class Item(name:String, url:String, image:String, status:String, fulfilled: String)  {

}

object Item {


  implicit object ItemBSOnReader extends BSONReader[Item] {
    def fromBSON(document: BSONDocument) :Item = {
      val doc = document.toTraversable
      Item(
        doc.getAs[BSONString]("name").get.value,
        doc.getAs[BSONString]("url").get.value,
        doc.getAs[BSONString]("image").get.value,
        doc.getAs[BSONString]("status").get.value,
        doc.getAs[BSONString]("fulfilled").get.value
      )

    }
  }
  implicit object ItemBSONWriter extends BSONWriter[Item] {
    def toBSON(user: Item) = {
      BSONDocument(
        "name" -> BSONString(user.name),
        "url" -> BSONString(user.url),
        "image" -> BSONString(user.image),
        "status" -> BSONString(user.image),
        "fullfilled" -> BSONString(user.fulfilled))

    }
  }

}