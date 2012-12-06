package controllers

import models._
import org.joda.time._
import play.api._
import play.api.mvc._
import play.api.Play.current
import play.modules.reactivemongo._
import scala.concurrent.{ExecutionContext, Future}

import reactivemongo.bson._
import reactivemongo.bson.handlers.DefaultBSONHandlers.DefaultBSONDocumentWriter
import reactivemongo.bson.handlers.DefaultBSONHandlers.DefaultBSONReaderHandler

object Events extends Controller with MongoController {
  val db = ReactiveMongoPlugin.db
  val collection = db("events")

  def index = Action { implicit request =>
    Async {
      implicit val reader = Event.EventBSONReader
      val query = BSONDocument("$query" -> BSONDocument())
      val found = collection.find(query)
      found.toList.map { events => Ok(views.html.events(events, Event.form))}
    }
  }


  def newEvent = TODO
  /*Action { implicit request =>
    Event.form.bindFromRequest.fold(
      errors => Ok(views.html.editArticle(None, errors, None)),
      // if no error, then insert the article into the 'articles' collection
      article => AsyncResult {
        collection.insert(article.copy(creationDate = Some(new DateTime()), updateDate = Some(new DateTime()))).map( _ =>
          Redirect(routes.Articles.index)
        )
      }
    )
  }*/
}