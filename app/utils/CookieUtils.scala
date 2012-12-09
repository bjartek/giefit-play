package utils

import play.api.mvc.{Cookie, Request}
import play.Logger

case class Login(user: Option[String], lastList: Option[String])

trait CookieUtils {

  implicit def login[A](implicit request: Request[A]) : Login =  {
    val login = Login(request.cookies.get("user").map(_.value),
          request.cookies.get("list").map(_.value))

    Logger.info("User and active list are " + login.toString)

    login
  }


  def createUserCookie(email:String) =  Cookie("user", email, Some(60 * 24 * 7))
  def createListCookie(value:String) =  Cookie("list", value, Some(60 * 24 * 7))

}