package controllers

import models._
import utils._
import play.api.mvc._
import play.api.Play.current
import play.modules.reactivemongo._

import org.joda.time.DateTime
import model.{EventStore}
import concurrent._
import reactivemongo.bson.BSONObjectID
import play.Logger
import play.api.libs.Crypto

object Events extends Controller with MongoController with CookieUtils {

  def index(hash:String) = Action { implicit request =>
    showEvents(hash)
  }

  lazy val redirectToMain = Some(Redirect(routes.Application.index))
  lazy val redirectToEventList = Redirect(routes.Events.indexCookie())


  def indexCookie = Action {  implicit request =>
    login.user.map(u => showEvents(Crypto.sign(u)))
       .orElse(redirectToMain).get

  }

  def showEvents[A](hash:String)(implicit request: Request[A]) = {

    Logger.info("Showing events for hash " + hash)

    Async {
      val events = EventStore.findForUser(hash)

      events.map{
        ev =>Ok(views.html.event.list(ev)).withCookies(createUserCookie(ev.head.owner.email))
      }.fallbackTo(
        future{Ok(views.html.event.list(List()))})
    }
  }

  def newEventForm = Action {implicit request =>

    val formWithEmail = login.user.map(u => {
      val name = EventStore.findNameForUser(u)
      Ok(views.html.event.create(Event.formWithEmail(u, name)))
    })

    formWithEmail.orElse(Some(Ok(views.html.event.create(Event.form)))).get

  }


  def details(eventId:String) = Action { implicit request =>
    Async {

      Logger.info(login.toString)

      val event = EventStore.findById(eventId)
      event.map{
        ev =>Ok(views.html.event.details(Event.form.fill(ev)))
      }.fallbackTo(future{redirectToEventList})
    }
  }

  def editEvent(hash:String) = TODO

  def newEvent = Action { implicit request =>
    Event.form.bindFromRequest.fold(
      errors =>     BadRequest(views.html.event.create(errors)),
      event => AsyncResult {
      val newEvent = event.copy(id = Some(BSONObjectID.generate), creationDate = Some(new DateTime() ))
      val res = EventStore.insert(newEvent)

        res.map( res =>
          Redirect(routes.Events.indexCookie())
            .withCookies(createListCookie(newEvent.id.get.stringify), createUserCookie(event.owner.email)))
      }
    )
  }
}