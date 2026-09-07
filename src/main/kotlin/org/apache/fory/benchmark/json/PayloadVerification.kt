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

import com.fasterxml.jackson.databind.JsonNode
import java.time.OffsetDateTime

private fun checkTree(expected: JsonNode, actual: JsonNode, path: String = "") {
  if (expected == actual) return
  // ISO-8601 allows optional trailing fractional zeros. Jackson trims them while
  // the JDK/Fory formatter groups fractional digits; compare the same typed value
  // only at Partner.since, keeping decimal numbers and all other strings exact.
  if (path.endsWith("/since") && expected.isTextual && actual.isTextual) {
    check(OffsetDateTime.parse(expected.textValue()) == OffsetDateTime.parse(actual.textValue())) {
      "JSON timestamp differs at $path: $expected vs $actual"
    }
    return
  }
  if (expected.isObject && actual.isObject) {
    check(expected.fieldNames().asSequence().toSet() == actual.fieldNames().asSequence().toSet()) {
      "JSON keys differ at $path"
    }
    for (name in expected.fieldNames()) checkTree(expected[name], actual[name], "$path/$name")
  } else if (expected.isArray && actual.isArray) {
    check(expected.size() == actual.size()) { "JSON array size differs at $path" }
    for (index in 0 until expected.size()) checkTree(expected[index], actual[index], "$path/$index")
  } else {
    error("JSON value differs at $path: $expected vs $actual")
  }
}

fun checkPayload(expected: Any, actual: Any) {
  if (expected !is Clients) {
    check(expected == actual) { "Decoded payload differs from expected model" }
    return
  }
  check(actual is Clients && expected.clients.size == actual.clients.size)
  for ((left, right) in expected.clients.zip(actual.clients)) {
    // Kotlin data-class equality compares array identity. Preserve upstream array
    // fields while checking every element and all remaining fields outside timing.
    check(left.emails.contentEquals(right.emails)) { "Client emails differ" }
    check(left.phones.contentEquals(right.phones)) { "Client phones differ" }
    check(left.copy(emails = right.emails, phones = right.phones) == right) {
      "Client scalar/list/temporal fields differ"
    }
  }
}

fun <T : Any> verifyPayload(codecs: LargePayloadCodecs<T>, fixture: PayloadFixture<T>) {
  val expected = fixture.value
  val referenceTree = codecs.tree(fixture.bytes)
  val strings = listOf(
    Triple("Fory", codecs::foryToString, codecs::foryFromString),
    Triple("kotlinx", codecs::kotlinxToString, codecs::kotlinxFromString),
    Triple("Moshi", codecs::moshiToString, codecs::moshiFromString),
    Triple("Jackson", codecs::jacksonToString, codecs::jacksonFromString),
  )
  for ((name, write, read) in strings) {
    checkPayload(expected, read(fixture.text))
    val output = write(expected)
    checkTree(referenceTree, codecs.tree(output), name)
    checkPayload(expected, read(output))
  }
  val bytes = listOf(
    Triple("Fory", codecs::foryToBytes, codecs::foryFromBytes),
    Triple("kotlinx", codecs::kotlinxToBytes, codecs::kotlinxFromBytes),
    Triple("Moshi", codecs::moshiToBytes, codecs::moshiFromBytes),
    Triple("Jackson", codecs::jacksonToBytes, codecs::jacksonFromBytes),
  )
  for ((name, write, read) in bytes) {
    checkPayload(expected, read(fixture.bytes))
    val output = write(expected)
    checkTree(referenceTree, codecs.tree(output), name)
    checkPayload(expected, read(output))
  }
}
