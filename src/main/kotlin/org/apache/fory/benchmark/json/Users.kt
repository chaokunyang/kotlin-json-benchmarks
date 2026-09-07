// Kotlin port of java-json-benchmark's Users schema, Copyright (c) 2016 Fabien Renaud.
// See LICENSES/java-json-benchmark-MIT.txt for the upstream license.
package org.apache.fory.benchmark.json

import com.squareup.moshi.JsonClass
import kotlinx.serialization.Serializable
import org.apache.fory.json.annotation.JsonType

@JsonType
@Serializable
@JsonClass(generateAdapter = true)
data class Users(val users: List<User>)

@JsonType
@Serializable
@JsonClass(generateAdapter = true)
data class User(
  val id: String,
  val index: Int,
  val guid: String,
  val isActive: Boolean,
  val balance: String,
  val picture: String,
  val age: Int,
  val eyeColor: String,
  val name: String,
  val gender: String,
  val company: String,
  val email: String,
  val phone: String,
  val address: String,
  val about: String,
  val registered: String,
  val latitude: Double,
  val longitude: Double,
  val tags: List<String>,
  val friends: List<Friend>,
  val greeting: String,
  val favoriteFruit: String,
)

@JsonType
@Serializable
@JsonClass(generateAdapter = true)
data class Friend(val id: String, val name: String)
