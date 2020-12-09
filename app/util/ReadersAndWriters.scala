package util

import models.{Address, User}
import play.api.libs.functional.syntax._
import play.api.libs.json.{Reads, _}

object ReadersAndWriters {

  implicit val userReader: Reads[User] =
    ((JsPath \ "id").read[Long] and (JsPath \ "name").read[String] and
      (JsPath \ "email").read[String] and (JsPath \ "gender")
      .read[String] and (JsPath \ "status")
      .read[String] and (JsPath \ "addresses")
      .lazyRead[List[Address]](Reads.list(addressReader)))(User.apply _)

  implicit val addressReader: Reads[Address] =
    ((JsPath \ "city").read[String] and (JsPath \ "country")
      .read[String] and (JsPath \ "pinCode").read[String])(Address.apply _)

  implicit val userWriter: Writes[User] = Writes({ (user: User) =>
    Json.obj(
      "id" -> user.id,
      "name" -> user.name,
      "email" -> user.email,
      "gender" -> user.gender,
      "status" -> user.gender,
      "addresses" -> user.addresses
    )
  })
  implicit val addressWriter: Writes[Address] = Writes({
    (address: Address) =>
      Json.obj(
        "city" -> address.city,
        "country" -> address.country,
        "pinCode" -> address.pinCode
      )
  })

}
