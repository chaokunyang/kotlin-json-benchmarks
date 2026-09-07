// Field distributions adapted from java-json-benchmark's Users/Clients generators.
// Copyright (c) 2016 Fabien Renaud. See LICENSES/java-json-benchmark-MIT.txt.
package org.apache.fory.benchmark.json

import java.math.BigDecimal
import java.security.MessageDigest
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Random
import java.util.UUID

data class PayloadFixture<T>(val value: T, val bytes: ByteArray, val records: Int) {
  val text: String get() = bytes.toString(Charsets.UTF_8)
  val sha256: String get() = MessageDigest.getInstance("SHA-256")
    .digest(bytes).joinToString("") { "%02x".format(it) }
}

object LargePayloadFixture {
  const val SEED = 20260811114233L

  fun users(sizeKb: Int): PayloadFixture<Users> {
    val generator = PayloadGenerator(Random(SEED))
    return generate("users", sizeKb, generator::user, ::Users)
  }

  fun clients(sizeKb: Int): PayloadFixture<Clients> {
    val generator = PayloadGenerator(Random(SEED))
    return generate("clients", sizeKb, generator::client, ::Clients)
  }

  private fun <E, T> generate(
    key: String, sizeKb: Int, next: () -> E, wrap: (List<E>) -> T,
  ): PayloadFixture<T> {
    require(sizeKb in 1..1000)
    val mapper = payloadMapper()
    val records = mutableListOf<E>()
    var size = key.length + 7 // {"key":[]} including both enclosing delimiters.
    while (size < sizeKb * 1000) {
      val record = next()
      size += mapper.writeValueAsBytes(record).size + if (records.isEmpty()) 0 else 1
      records.add(record)
    }
    val value = wrap(records)
    val bytes = mapper.writeValueAsBytes(value)
    check(bytes.size == size)
    return PayloadFixture(value, bytes, records.size)
  }
}

private class PayloadGenerator(private val random: Random) {
  private val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
  private fun text(length: Int, chars: String = alphabet): String =
    String(CharArray(length) { chars[random.nextInt(chars.length)] })

  fun user(): User = User(
    id = text(20, "0123456789"), index = random.nextInt(Int.MAX_VALUE),
    guid = text(20), isActive = random.nextBoolean(), balance = text(20),
    picture = text(100), age = random.nextInt(100), eyeColor = text(20), name = text(20),
    gender = text(20), company = text(20), email = text(20), phone = text(20),
    address = text(20), about = text(20), registered = text(20),
    latitude = random.nextDouble() * 90, longitude = random.nextDouble() * 180,
    tags = List(random.nextInt(50)) { text(10) },
    friends = List(random.nextInt(50)) {
      Friend(random.nextInt(10000).toString(), text(30, alphabet.take(52)))
    },
    greeting = text(20), favoriteFruit = text(20),
  )

  fun client(): Client = Client(
    id = random.nextLong().ushr(1), index = random.nextInt(Int.MAX_VALUE),
    guid = UUID(random.nextLong(), random.nextLong()), isActive = random.nextBoolean(),
    balance = BigDecimal.valueOf(random.nextDouble()), picture = text(100),
    age = random.nextInt(100), eyeColor = EyeColor.entries[random.nextInt(3)],
    name = text(20), gender = text(20), company = text(20),
    emails = Array(random.nextInt(10)) { text(20) },
    phones = LongArray(random.nextInt(10)) { random.nextInt(Int.MAX_VALUE).toLong() },
    address = text(20), about = text(20), registered = date(),
    latitude = random.nextDouble() * 90, longitude = random.nextDouble() * 180,
    tags = List(random.nextInt(50)) { text(10) },
    partners = List(random.nextInt(30)) {
      Partner(random.nextLong(), text(30, alphabet.take(52)),
        OffsetDateTime.of(date(), java.time.LocalTime.of(
          random.nextInt(24), random.nextInt(60), random.nextInt(60), random.nextInt(1_000_000_000),
        ), ZoneOffset.UTC))
    },
  )

  private fun date(): LocalDate = LocalDate.of(
    1900 + random.nextInt(110), 1 + random.nextInt(12), 1 + random.nextInt(28),
  )
}
