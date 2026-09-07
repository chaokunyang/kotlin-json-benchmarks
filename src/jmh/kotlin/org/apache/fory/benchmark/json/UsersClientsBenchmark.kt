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

import java.util.concurrent.TimeUnit
import org.apache.fory.json.kotlin.jsonTypeRef
import org.openjdk.jmh.annotations.*

@State(Scope.Thread)
open class UsersClientsState {
  @Param("users", "clients")
  lateinit var payload: String

  @Param("1000")
  var sizeKb: Int = 1000

  lateinit var codecs: LargePayloadCodecs<Any>
  lateinit var expected: Any
  lateinit var fixtureString: String
  lateinit var fixtureBytes: ByteArray

  @Setup
  fun setup() {
    when (payload) {
      "users" -> install(LargePayloadFixture.users(sizeKb),
        LargePayloadCodecs(jsonTypeRef<Users>(), Users.serializer(), Users::class.java))
      "clients" -> install(LargePayloadFixture.clients(sizeKb),
        LargePayloadCodecs(jsonTypeRef<Clients>(), Clients.serializer(), Clients::class.java))
      else -> error("Unknown payload: $payload")
    }
  }

  private fun <T : Any> install(fixture: PayloadFixture<T>, typedCodecs: LargePayloadCodecs<T>) {
    verifyPayload(typedCodecs, fixture)
    // Erase the state only after pairing and checking the exact model/type token.
    // Every timed method uses this same pair; no runtime schema selection is timed.
    @Suppress("UNCHECKED_CAST")
    val erased = typedCodecs as LargePayloadCodecs<Any>
    codecs = erased
    expected = fixture.value
    fixtureBytes = fixture.bytes
    fixtureString = fixture.text
    println("PAYLOAD payload=$payload sizeKb=$sizeKb records=${fixture.records} " +
      "bytes=${fixture.bytes.size} sha256=${fixture.sha256}")
  }
}

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(1)
@Threads(1)
open class UsersClientsBenchmark {
  @Benchmark
  fun foryToJsonString(s: UsersClientsState): String = s.codecs.foryToString(s.expected)
  @Benchmark
  fun foryToJsonBytes(s: UsersClientsState): ByteArray = s.codecs.foryToBytes(s.expected)
  @Benchmark
  fun foryFromJsonString(s: UsersClientsState): Any = s.codecs.foryFromString(s.fixtureString)
  @Benchmark
  fun foryFromJsonBytes(s: UsersClientsState): Any = s.codecs.foryFromBytes(s.fixtureBytes)
  @Benchmark
  fun kotlinxToJsonString(s: UsersClientsState): String = s.codecs.kotlinxToString(s.expected)
  @Benchmark
  fun kotlinxToJsonBytes(s: UsersClientsState): ByteArray = s.codecs.kotlinxToBytes(s.expected)
  @Benchmark
  fun kotlinxFromJsonString(s: UsersClientsState): Any = s.codecs.kotlinxFromString(s.fixtureString)
  @Benchmark
  fun kotlinxFromJsonBytes(s: UsersClientsState): Any = s.codecs.kotlinxFromBytes(s.fixtureBytes)
  @Benchmark
  fun moshiToJsonString(s: UsersClientsState): String = s.codecs.moshiToString(s.expected)
  @Benchmark
  fun moshiToJsonBytes(s: UsersClientsState): ByteArray = s.codecs.moshiToBytes(s.expected)
  @Benchmark
  fun moshiFromJsonString(s: UsersClientsState): Any = s.codecs.moshiFromString(s.fixtureString)
  @Benchmark
  fun moshiFromJsonBytes(s: UsersClientsState): Any = s.codecs.moshiFromBytes(s.fixtureBytes)
  @Benchmark
  fun jacksonToJsonString(s: UsersClientsState): String = s.codecs.jacksonToString(s.expected)
  @Benchmark
  fun jacksonToJsonBytes(s: UsersClientsState): ByteArray = s.codecs.jacksonToBytes(s.expected)
  @Benchmark
  fun jacksonFromJsonString(s: UsersClientsState): Any = s.codecs.jacksonFromString(s.fixtureString)
  @Benchmark
  fun jacksonFromJsonBytes(s: UsersClientsState): Any = s.codecs.jacksonFromBytes(s.fixtureBytes)
}
