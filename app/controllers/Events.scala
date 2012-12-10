package controllers

import models._
import utils._
import play.api.mvc._
import play.modules.reactivemongo._

import org.joda.time.DateTime
import model.EventStore
import concurrent._
import duration.Duration
import reactivemongo.bson.BSONObjectID
import play.Logger
import play.api.libs.Crypto
import java.util.concurrent.TimeUnit

object Events extends Controller with MongoController with CookieUtils {


  /*
   * This will not work. What is wrong in the addItem method in EventStore?
   */
  def addItem() = Action { implicit request =>

    val id = BSONObjectID.generate
    val user = User("Bjarte", "bjarte@bjartek.org")
    val event = Event(Some(id), "Test", "test", user,None, None)

    val dur = Duration(1, TimeUnit.SECONDS)
    val insertedEvent = Await.result(EventStore.insert(event), dur)

    val item = model.Item("Test item", "", "", "", "")

    Await.result(EventStore.addItem(event, item), dur)


    Logger.info("Raw event is " + event.toString )

    val newEvent = Await.result(EventStore.findById(id.stringify), dur)

    Logger.info("Updated event is " + newEvent.toString )




    Ok(views.html.index("Your new application is ready."))
  }


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
      event.map{ ev => Ok(views.html.event.details(Event.form.fill(ev)))

      }.fallbackTo(future{redirectToEventList})
    }
  }

  def editEvent(hash:String) = TODO

  def newEvent = Action { implicit request =>
    Event.form.bindFromRequest.fold(
      errors =>     BadRequest(views.html.event.create(errors)),
      event => AsyncResult {
        Logger.info("Storing event " + event.toString)
      val newEvent = event.copy(id = Some(BSONObjectID.generate), creationDate = Some(new DateTime() ))
      val res = EventStore.insert(newEvent)

        res.map( res =>
          Redirect(routes.Events.indexCookie())
            .withCookies(createListCookie(newEvent.id.get.stringify), createUserCookie(event.owner.email)))
      }
    )
  }
}