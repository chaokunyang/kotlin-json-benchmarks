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

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectReader
import com.fasterxml.jackson.databind.ObjectWriter
import com.squareup.moshi.JsonAdapter
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import okio.Buffer
import org.apache.fory.json.ForyJson
import org.apache.fory.json.kotlin.ForyJsonKotlin
import org.apache.fory.reflect.TypeRef

@OptIn(ExperimentalSerializationApi::class)
class LargePayloadCodecs<T : Any>(
  val foryType: TypeRef<T>,
  val kotlinxSerializer: KSerializer<T>,
  modelClass: Class<T>,
) {
  // Finish code generation before timing, and make null/default output equivalent
  // across libraries: Kotlin nulls cannot be omitted when omission changes defaults.
  val fory: ForyJson =
    ForyJsonKotlin.builder().withAsyncCompilation(false).writeNullFields(true).build()

  val kotlinx: Json = Json {
    encodeDefaults = true
    explicitNulls = true
  }

  val moshiAdapter: JsonAdapter<T> =
    payloadMoshi().adapter(modelClass).serializeNulls()

  val jackson: ObjectMapper =
    payloadMapper().setDefaultPropertyInclusion(JsonInclude.Include.ALWAYS)
  val jacksonReader: ObjectReader = jackson.readerFor(modelClass)
  val jacksonWriter: ObjectWriter = jackson.writerFor(modelClass)

  fun foryToString(value: T): String = fory.toJson(value, foryType)

  fun foryToBytes(value: T): ByteArray = fory.toJsonBytes(value, foryType)

  fun foryFromString(value: String): T = fory.fromJson(value, foryType)

  fun foryFromBytes(value: ByteArray): T = fory.fromJson(value, foryType)

  fun kotlinxToString(value: T): String =
    kotlinx.encodeToString(kotlinxSerializer, value)

  fun kotlinxToBytes(value: T): ByteArray {
    // Exercise the library's UTF-8 API; include the output allocation required by
    // the ByteArray contract without adding a String conversion to the workload.
    val output = ByteArrayOutputStream()
    kotlinx.encodeToStream(kotlinxSerializer, value, output)
    return output.toByteArray()
  }

  fun kotlinxFromString(value: String): T =
    kotlinx.decodeFromString(kotlinxSerializer, value)

  fun kotlinxFromBytes(value: ByteArray): T =
    kotlinx.decodeFromStream(kotlinxSerializer, ByteArrayInputStream(value))

  fun moshiToString(value: T): String = moshiAdapter.toJson(value)

  fun moshiToBytes(value: T): ByteArray {
    val buffer = Buffer()
    moshiAdapter.toJson(buffer, value)
    return buffer.readByteArray()
  }

  fun moshiFromString(value: String): T = checkNotNull(moshiAdapter.fromJson(value))

  fun moshiFromBytes(value: ByteArray): T =
    checkNotNull(moshiAdapter.fromJson(Buffer().write(value)))

  fun jacksonToString(value: T): String = jacksonWriter.writeValueAsString(value)

  fun jacksonToBytes(value: T): ByteArray = jacksonWriter.writeValueAsBytes(value)

  fun jacksonFromString(value: String): T = jacksonReader.readValue(value)

  fun jacksonFromBytes(value: ByteArray): T = jacksonReader.readValue(value)

  fun tree(value: String): JsonNode = jackson.readTree(value)

  fun tree(value: ByteArray): JsonNode = jackson.readTree(value)
}
