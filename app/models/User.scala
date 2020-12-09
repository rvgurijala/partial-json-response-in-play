package models

case class User(id: Long, name: String, email: String, gender: String, status: String, addresses: List[Address])

case class Address(city: String, country: String, pinCode: String)


