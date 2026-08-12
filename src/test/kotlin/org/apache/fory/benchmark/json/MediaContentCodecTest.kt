package org.apache.fory.benchmark.json

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.squareup.moshi.Moshi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.fory.json.ForyJson
import kotlin.test.Test
import kotlin.test.assertEquals

class MediaContentCodecTest {
    private val jsonText = checkNotNull(javaClass.classLoader.getResource("data/eishay.json")).readText()
    private val kotlinxJson = Json {
        encodeDefaults = true
        explicitNulls = false
    }
    private val expected = kotlinxJson.decodeFromString<MediaContent>(jsonText)

    @Test
    fun roundTripsWithEveryLibrary() {
        val foryJson = ForyJson.builder().build()
        val moshiAdapter = Moshi.Builder().build().adapter(MediaContent::class.java)
        val jacksonMapper = jacksonObjectMapper().setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)

        assertEquals(expected, foryJson.fromJson(foryJson.toJson(expected), MediaContent::class.java))
        assertEquals(expected, kotlinxJson.decodeFromString(kotlinxJson.encodeToString(expected)))
        assertEquals(expected, moshiAdapter.fromJson(moshiAdapter.toJson(expected)))
        assertEquals(
            expected,
            jacksonMapper.readValue(jacksonMapper.writeValueAsBytes(expected), MediaContent::class.java),
        )

        assertEquals(expected, foryJson.fromJson(jsonText, MediaContent::class.java))
        assertEquals(expected, moshiAdapter.fromJson(jsonText))
        assertEquals(expected, jacksonMapper.readValue(jsonText, MediaContent::class.java))
    }
}
