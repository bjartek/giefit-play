package controllers

import models._
import play.api.mvc._
import play.api.Play.current
import play.modules.reactivemongo._

import org.joda.time.DateTime
import play.api.Logger
import model.{EventStore, UserStore}
import concurrent._

object Events extends Controller with MongoController {

  val db = ReactiveMongoPlugin.db
  val collection = db("events")


  def index(hash:String) = Action { implicit request =>
    showEvents(hash)
  }

  def indexCookie() = Action {  implicit request =>
    val cookie = request.cookies.get("user")
    cookie match {
      case None => Redirect(routes.Application.index)
      case Some(c) => showEvents(c.value)
    }
  }

  def showEvents(hash:String) = {
    Async {

      val events:Future[List[Event]] = for {
        user <- UserStore.findById(hash)
        ev <- EventStore.findEventsForUser(user)
      } yield ev

      events.map{
        case ev => Ok(views.html.event.list(ev))
      }.fallbackTo(future{Ok(views.html.event.list(List()))})
    }.withCookies(Cookie("user", hash, Some(60 * 24 * 7)))
  }



  def newEventForm = Action {implicit request =>
    Async {
      UserStore.findUserFromCookie(request.cookies).map {
        case u => Ok(views.html.event.create(Event.formWithEmail(u.email)))
     }.fallbackTo(future { Ok(views.html.event.create(Event.form))})

    }
  }


  def newEvent = Action { implicit request =>
    Event.form.bindFromRequest.fold(
      errors => Ok(views.html.event.create(Event.form)),
      event => AsyncResult {
        val res = collection.insert(event.copy(creationDate = Some(new DateTime())))
        res.map( res => Redirect(routes.Events.index(event.owner)))
      }
    )
  }
}