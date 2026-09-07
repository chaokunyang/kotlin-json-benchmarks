/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.fory.benchmark.json

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonUnquotedLiteral
import kotlinx.serialization.json.jsonPrimitive

abstract class JdkStringSerializer<T>(name: String, private val parse: (String) -> T) : KSerializer<T> {
  override val descriptor = PrimitiveSerialDescriptor(name, PrimitiveKind.STRING)
  override fun serialize(encoder: Encoder, value: T) = encoder.encodeString(value.toString())
  override fun deserialize(decoder: Decoder): T = parse(decoder.decodeString())
}

object UuidSerializer : JdkStringSerializer<UUID>("java.util.UUID", UUID::fromString)
object DateSerializer : JdkStringSerializer<LocalDate>("java.time.LocalDate", LocalDate::parse)
object OffsetTimeSerializer :
  JdkStringSerializer<OffsetDateTime>("java.time.OffsetDateTime", OffsetDateTime::parse)

@OptIn(ExperimentalSerializationApi::class)
object DecimalSerializer : KSerializer<BigDecimal> {
  override val descriptor = PrimitiveSerialDescriptor("java.math.BigDecimal", PrimitiveKind.DOUBLE)

  override fun serialize(encoder: Encoder, value: BigDecimal) {
    // A Double conversion would silently lose decimal digits. Keep a JSON number
    // with the original decimal token, matching Fory/Jackson/Moshi's number shape.
    require(encoder is JsonEncoder)
    encoder.encodeJsonElement(JsonUnquotedLiteral(value.toString()))
  }

  override fun deserialize(decoder: Decoder): BigDecimal {
    require(decoder is JsonDecoder)
    return decoder.decodeJsonElement().jsonPrimitive.content.toBigDecimal()
  }
}

private class JdkStringAdapter<T : Any>(private val parse: (String) -> T) : JsonAdapter<T>() {
  override fun fromJson(reader: JsonReader): T = parse(reader.nextString())
  override fun toJson(writer: JsonWriter, value: T?) {
    writer.value(requireNotNull(value).toString())
  }
}

private class DecimalAdapter : JsonAdapter<BigDecimal>() {
  override fun fromJson(reader: JsonReader): BigDecimal = reader.nextString().toBigDecimal()
  override fun toJson(writer: JsonWriter, value: BigDecimal?) {
    // Use the Number overload; writing toPlainString() would quote the number.
    writer.value(requireNotNull(value))
  }
}

fun payloadMoshi(): Moshi = Moshi.Builder()
  .add(UUID::class.java, JdkStringAdapter(UUID::fromString).nullSafe())
  .add(BigDecimal::class.java, DecimalAdapter().nullSafe())
  .add(LocalDate::class.java, JdkStringAdapter(LocalDate::parse).nullSafe())
  .add(OffsetDateTime::class.java, JdkStringAdapter(OffsetDateTime::parse).nullSafe())
  .build()

fun payloadMapper(): ObjectMapper = jacksonObjectMapper()
  .registerModule(JavaTimeModule())
  .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
  .disable(SerializationFeature.WRITE_DATES_WITH_CONTEXT_TIME_ZONE)
  .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
  // Tree comparisons must also retain all decimal digits, not compare rounded Doubles.
  .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
