package controllers

import model._
import utils._
import play.api.mvc._
import play.modules.reactivemongo._

import concurrent._
import play.Logger
import play.api.libs.Crypto

object Events extends Controller with MongoController with CookieUtils {


  def index(hash:String) = Action { implicit request =>
    showEvents(Crypto.decryptAES(hash))
  }

  lazy val redirectToMain = Some(Redirect(routes.Application.index))
  lazy val redirectToEventList = Redirect(routes.Events.indexCookie())


  def indexCookie = Action {  implicit request =>
    login.user.map(u => showEvents(u))
       .orElse(redirectToMain).get

  }

  def showEvents[A](email:String)(implicit request: Request[A]) = {

    Logger.info("Showing events for hash " + email)

    Async {
      val events = EventStore.findForUser(email)

      events.map{
        ev =>Ok(views.html.event.list(ev)).withCookies(createUserCookie(ev.head.owner.email))
      }.fallbackTo(
        future{Ok(views.html.event.list(List()))})
    }
  }

  def newEventForm = Action {implicit request =>

    val formWithEmail = login.user.map(u => {
      val name = EventStore.findNameForUser(u)
      Ok(views.html.event.create(Event.formWithEmail(name, u)))
    })

    formWithEmail.orElse(Some(Ok(views.html.event.create(Event.form)))).get

  }


  def details(eventId:String) = Action { implicit request =>
    Async {

      Logger.info(login.toString)

      val event = EventStore.findById(eventId)
      event.map{ ev => Ok(views.html.event.details(ev, Event.form.fill(ev), Item.form, User.form))

      }.fallbackTo(future{redirectToEventList})
    }
  }


  def editEvent(eventId: String) = Action { implicit request =>
    Event.form.bindFromRequest.fold(
      errors =>  Async {
        val event = EventStore.findById(eventId)
        event.map( ev => BadRequest(views.html.event.details(ev, errors, Item.form, User.form)))
      },
      newEv => Async {

        val res = for {
          event <- EventStore.findById(eventId)
          insert <- EventStore.updateEvent(event, newEv)
        } yield (insert)

        res.map(_ => Redirect(routes.Events.details(eventId)))
      }
    )
  }


  def newEvent = Action { implicit request =>
    Event.form.bindFromRequest.fold(
      errors =>     BadRequest(views.html.event.create(errors)),
      event => AsyncResult {
        Logger.info("Storing event " + event.toString)
        val res = EventStore.insert(event)
        res.map( _ =>
          Redirect(routes.Events.details(event.id.get.stringify))
            .withCookies(createListCookie(event.id.get.stringify), createUserCookie(event.owner.email)))
      }
    )
  }
}