package controllers

import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import services.UserService
import util.JsonTransformer
import util.ReadersAndWriters._

import scala.concurrent.ExecutionContext.Implicits.global
@Singleton
class UserController @Inject()(cc: ControllerComponents)
    extends AbstractController(cc) {

  def getAllUsers(fields: Option[String]): Action[AnyContent] = {
    Action.async { _ =>
      {
        val futureUsers = new UserService().getUsers()
        futureUsers.map(users => {
          doFilter(Json.toJson(users), fields)
        })
      }.recover {
        case _ => {
          InternalServerError("Something wrong while processing the request")
        }
      }
    }
  }

  def doFilter(jsValue:JsValue, fields: Option[String]) = {
    JsonTransformer.filter(jsValue, fields) match {
      case Left(str)      => Ok(str)
      case Right(jsValue) => Ok(jsValue)
    }
  }
}
