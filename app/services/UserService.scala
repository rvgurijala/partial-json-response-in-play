package services


import models.User
import play.api.libs.json.{JsArray, JsValue, Json}
import util.ReadersAndWriters._

import scala.concurrent.Future

class UserService {

   def getUsers(): Future[List[User]] = {
     val fileStream = getClass.getResourceAsStream("/users.json")
     val users: List[User] = jsonToUserData(Json.parse(fileStream))
     Future.successful(users)
   }

  private def jsonToUserData(requestBody: JsValue) = {
    (requestBody \ "data").getOrElse(Json.obj()) match {
      case jsArray: JsArray => {
        jsArray.value.map(
          value => value.as[User]
        )
      }.toList
      case _ => {
        Nil
      }
    }
  }
}
