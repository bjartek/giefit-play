package controllers

import model._
import utils._
import play.api.mvc._
import play.modules.reactivemongo._

import concurrent._
import play.Logger
import play.api.libs.Crypto

object Items extends Controller with MongoController with CookieUtils {


  def showEditItemForm(eventId:String, itemName:String) = Action { implicit request =>

    Async {
      val events = EventStore.findById(eventId)
      events.map{ ev =>

        val itemForm =  ev.items.find(it => it.name == itemName).map(it => Item.form.fill(it)).getOrElse(Item.form)

        Ok(views.html.event.details(ev, Event.form.fill(ev), itemForm, User.form))
          .withCookies(createUserCookie(ev.owner.email))
      }.fallbackTo(
        future{Ok(views.html.event.list(List()))})
    }
  }

  def addItem(eventId: String) = Action { implicit request =>
    Item.form.bindFromRequest.fold(
      errors =>  Async {
        val event = EventStore.findById(eventId)
        event.map( ev => BadRequest(views.html.event.details(ev, Event.form.fill(ev), errors, User.form)))
      },
      item => Async {

        val res = for {
          event <- EventStore.findById(eventId)
          insert <- EventStore.addItem(event, item)
        } yield (insert)

        res.map(_ => Redirect(routes.Events.details(eventId)))
      }
    )
  }



  def editItem(eventId: String, itemName:String) = Action { implicit request =>
    Item.form.bindFromRequest.fold(
      errors =>  Async {
        val event = EventStore.findById(eventId)
        event.map( ev => BadRequest(views.html.event.details(ev, Event.form.fill(ev), errors, User.form)))
      },
      item => Async {

        val res = for {
          event <- EventStore.findById(eventId)
          insert <- EventStore.updateItem(event, itemName, item)
        } yield (insert)

        res.map(_ => Redirect(routes.Events.details(eventId)))
      }
    )
  }

}
