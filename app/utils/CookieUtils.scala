package utils

import play.api.mvc.{Cookie, Request}
import play.Logger
import model.User

case class Login(user: Option[String], username:Option[String], lastList: Option[String])

trait CookieUtils {

  implicit def login[A](implicit request: Request[A]) : Login =  {
    val login = Login(request.cookies.get("user").map(_.value),
                      request.cookies.get("username").map(_.value),
                      request.cookies.get("list").map(_.value))

    Logger.info("User and active list are " + login.toString)

    login
  }

  val dur = Some(60 * 24 * 7)

  def createUserCookies(user:User): List[Cookie] =  List(Cookie("user", user.email, dur), Cookie("username", user.name, dur))
  def createListCookie(value:String) =  Cookie("list", value, Some(60 * 24 * 7))

}