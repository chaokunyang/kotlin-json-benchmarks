// Kotlin port of java-json-benchmark's Clients schema, Copyright (c) 2016 Fabien Renaud.
// See LICENSES/java-json-benchmark-MIT.txt for the upstream license.
@file:kotlinx.serialization.UseSerializers(
  UuidSerializer::class, DecimalSerializer::class,
  DateSerializer::class, OffsetTimeSerializer::class,
)

package org.apache.fory.benchmark.json

import com.squareup.moshi.JsonClass
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import kotlinx.serialization.Serializable
import org.apache.fory.json.annotation.JsonType

@JsonType
@Serializable
@JsonClass(generateAdapter = true)
data class Clients(val clients: List<Client>)

@JsonType
@Serializable
@JsonClass(generateAdapter = true)
data class Client(
  val id: Long,
  val index: Int,
  val guid: UUID,
  val isActive: Boolean,
  val balance: BigDecimal,
  val picture: String,
  val age: Int,
  val eyeColor: EyeColor,
  val name: String,
  val gender: String,
  val company: String,
  val emails: Array<String>,
  val phones: LongArray,
  val address: String,
  val about: String,
  val registered: LocalDate,
  val latitude: Double,
  val longitude: Double,
  val tags: List<String>,
  val partners: List<Partner>,
)

@Serializable
enum class EyeColor { BROWN, BLUE, GREEN }

@JsonType
@Serializable
@JsonClass(generateAdapter = true)
data class Partner(val id: Long, val name: String, val since: OffsetDateTime)
