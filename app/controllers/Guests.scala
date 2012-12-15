package controllers

import play.api.mvc.{Action, Controller}
import play.modules.reactivemongo.MongoController
import utils.CookieUtils
import play.api.Logger
import model._
import concurrent._

object Guests extends Controller with MongoController with CookieUtils {

  def delete(eventId:String) = TODO

  def showEditForm(eventId:String, guestId:String) = Action { implicit request =>

    Logger.info(guestId)

    Async {
      val events = EventStore.findById(eventId)
      events.map{ ev =>

        val userForm =  ev.guests.find(it => it.email == guestId).map(it => User.form.fill(it)).getOrElse(User.form)

        Ok(views.html.event.details(ev, Event.form.fill(ev), Item.form, userForm))
          .withCookies(createUserCookies(ev.owner) : _*)
      }.fallbackTo(
        future{Ok(views.html.event.list(List()))})
    }
  }

  def addGuest(eventId: String) = Action {
    implicit request =>
      Logger.info("adding guest")
      User.form.bindFromRequest.fold(
        errors => Async {
          Logger.info("ERROR adding guest to event with id " + eventId)
          Logger.info(errors.errorsAsJson.toString())

          val event = EventStore.findById(eventId)
          event.map(ev => BadRequest(views.html.event.details(ev, Event.form.fill(ev), Item.form, errors)))
        },
        user => Async {

          val res = for {
            event <- EventStore.findById(eventId)
            insert <- EventStore.addGuest(event, user)
          } yield (insert)

          res.map(_ => Redirect(routes.Events.details(eventId)))
        }
      )
  }



  def editGuest(eventId: String, guestId:String) = Action { implicit request =>
    User.form.bindFromRequest.fold(
      errors =>  Async {
        val event = EventStore.findById(eventId)
        event.map( ev => BadRequest(views.html.event.details(ev, Event.form.fill(ev), Item.form, errors)))
      },
      guest => Async {
        val res = for {
          event <- EventStore.findById(eventId)
          insert <- EventStore.updateGuest(event, guestId, guest)
        } yield (insert)

        res.map(_ => Redirect(routes.Events.details(eventId)))
      }
    )
  }
}
