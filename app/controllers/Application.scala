package controllers

import utils._
import play.api.mvc._
import utils.CookieUtils

object Application extends Controller with CookieUtils {
  
  def index = Action {  implicit request =>
    Ok(views.html.index("GiefIt"))
  }

  def logout = Action { implicit request =>

    Ok(views.html.index("GiefIT - Logged out")).discardingCookies("lastList", "user", "username")

  }
  
}
