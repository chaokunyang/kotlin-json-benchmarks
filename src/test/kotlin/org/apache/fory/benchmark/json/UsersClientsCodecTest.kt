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

import java.lang.reflect.Modifier
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.*
import org.apache.fory.json.kotlin.jsonTypeRef

class UsersClientsCodecTest {
  private fun usersCodecs() =
    LargePayloadCodecs(jsonTypeRef<Users>(), Users.serializer(), Users::class.java)
  private fun clientsCodecs() =
    LargePayloadCodecs(jsonTypeRef<Clients>(), Clients.serializer(), Clients::class.java)

  @Test
  fun allPayloadPathsAgree() {
    val users = LargePayloadFixture.users(1000)
    val clients = LargePayloadFixture.clients(1000)
    verifyPayload(usersCodecs(), users)
    verifyPayload(clientsCodecs(), clients)
    for (fixture in listOf(users, clients)) {
      assertTrue(fixture.bytes.size >= 1_000_000)
      assertTrue(fixture.bytes.size < 1_010_000)
      assertTrue(fixture.records > 1)
    }
  }

  @Test
  fun fixturesAreReproducible() {
    assertContentEquals(LargePayloadFixture.users(10).bytes, LargePayloadFixture.users(10).bytes)
    assertContentEquals(LargePayloadFixture.clients(10).bytes, LargePayloadFixture.clients(10).bytes)
    assertFailsWith<IllegalArgumentException> { LargePayloadFixture.users(0) }
    assertFailsWith<IllegalArgumentException> { LargePayloadFixture.clients(1001) }
  }

  @Test
  fun modelsKeepUpstreamFieldTypes() {
    val types = listOf(Users::class.java, User::class.java, Friend::class.java,
      Clients::class.java, Client::class.java, Partner::class.java)
    for (type in types) {
      assertFalse(type.constructors.any { it.parameterCount == 0 })
      assertTrue(type.declaredFields.filterNot { Modifier.isStatic(it.modifiers) }
        .all { Modifier.isFinal(it.modifiers) })
    }
    assertEquals(Array<String>::class.java, Client::class.java.getDeclaredField("emails").type)
    assertEquals(LongArray::class.java, Client::class.java.getDeclaredField("phones").type)
    assertEquals(UUID::class.java, Client::class.java.getDeclaredField("guid").type)
    assertEquals(BigDecimal::class.java, Client::class.java.getDeclaredField("balance").type)
    assertEquals(LocalDate::class.java, Client::class.java.getDeclaredField("registered").type)
    assertEquals(OffsetDateTime::class.java, Partner::class.java.getDeclaredField("since").type)
    assertEquals(String::class.java, User::class.java.getDeclaredField("registered").type)
    val tree = usersCodecs().tree(LargePayloadFixture.users(1).bytes).get("users").get(0)
    assertTrue(tree.has("isActive"))
    assertFalse(tree.has("active"))
    assertEquals(22, tree.size())
  }

  @Test
  fun preciseJdkValuesRoundTrip() {
    val base = LargePayloadFixture.clients(1).value.clients.first()
    val value = Clients(listOf(base.copy(
      id = Long.MAX_VALUE,
      balance = BigDecimal("123456789012345678901234567890.1234567890123456789"),
      emails = arrayOf("quoted\"value", "雪@example.test"),
      phones = longArrayOf(Long.MIN_VALUE, Long.MAX_VALUE),
      partners = listOf(Partner(Long.MIN_VALUE, "Unicode 雪",
        OffsetDateTime.parse("2024-02-29T12:34:56.123456789+05:30"))),
    )))
    val bytes = payloadMapper().writeValueAsBytes(value)
    val fixture = PayloadFixture(value, bytes, 1)
    val codecs = clientsCodecs()
    verifyPayload(codecs, fixture)
    assertTrue(codecs.tree(bytes).get("clients").get(0).get("balance").isNumber)
    assertFails { checkPayload(value, Clients(listOf(value.clients[0].copy(phones = longArrayOf(0))))) }
  }
}
