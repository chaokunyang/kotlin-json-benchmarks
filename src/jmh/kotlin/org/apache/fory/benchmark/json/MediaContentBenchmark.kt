package org.apache.fory.benchmark.json

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.squareup.moshi.Moshi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.fory.json.ForyJson
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Threads
import org.openjdk.jmh.annotations.Warmup
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(1)
@Threads(1)
open class MediaContentBenchmark {
    @State(Scope.Thread)
    open class BenchmarkState {
        lateinit var foryJson: ForyJson
        lateinit var kotlinxJson: Json
        lateinit var moshiAdapter: com.squareup.moshi.JsonAdapter<MediaContent>
        lateinit var jacksonMapper: com.fasterxml.jackson.databind.ObjectMapper
        lateinit var mediaContent: MediaContent
        lateinit var jsonString: String
        lateinit var jsonBytes: ByteArray

        @Setup
        fun setup() {
            foryJson = ForyJson.builder().build()
            kotlinxJson = Json {
                encodeDefaults = true
                explicitNulls = false
            }
            moshiAdapter = Moshi.Builder().build().adapter(MediaContent::class.java)
            jacksonMapper = jacksonObjectMapper().setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
            jsonString = readResource()
            jsonBytes = jsonString.toByteArray(StandardCharsets.UTF_8)
            mediaContent = kotlinxJson.decodeFromString<MediaContent>(jsonString)

            verifyDecoded("Fory JSON", foryJson.fromJson(jsonString, MediaContent::class.java))
            verifyDecoded("kotlinx.serialization", kotlinxJson.decodeFromString<MediaContent>(jsonString))
            verifyDecoded("Moshi", requireNotNull(moshiAdapter.fromJson(jsonString)))
            verifyDecoded("Jackson", jacksonMapper.readValue(jsonString, MediaContent::class.java))

            verifyRoundTrip("Fory JSON", foryJson.toJson(mediaContent)) {
                foryJson.fromJson(it, MediaContent::class.java)
            }
            verifyRoundTrip("kotlinx.serialization", kotlinxJson.encodeToString(mediaContent)) {
                kotlinxJson.decodeFromString<MediaContent>(it)
            }
            verifyRoundTrip("Moshi", moshiAdapter.toJson(mediaContent)) {
                requireNotNull(moshiAdapter.fromJson(it))
            }
            verifyRoundTrip("Jackson", jacksonMapper.writeValueAsString(mediaContent)) {
                jacksonMapper.readValue(it, MediaContent::class.java)
            }
        }

        private fun verifyDecoded(library: String, decoded: MediaContent) {
            check(decoded == mediaContent) { "$library produced different MediaContent" }
        }

        private fun verifyRoundTrip(
            library: String,
            encoded: String,
            decode: (String) -> MediaContent,
        ) {
            verifyDecoded(library, decode(encoded))
        }

        private fun readResource(): String =
            checkNotNull(javaClass.classLoader.getResource("data/eishay.json")) {
                "Missing data/eishay.json"
            }.readText(StandardCharsets.UTF_8)
    }

    @Benchmark
    fun foryToJsonBytes(state: BenchmarkState): ByteArray = state.foryJson.toJsonBytes(state.mediaContent)

    @Benchmark
    fun kotlinxToJsonBytes(state: BenchmarkState): ByteArray =
        state.kotlinxJson.encodeToString(state.mediaContent).toByteArray(StandardCharsets.UTF_8)

    @Benchmark
    fun moshiToJsonBytes(state: BenchmarkState): ByteArray =
        state.moshiAdapter.toJson(state.mediaContent).toByteArray(StandardCharsets.UTF_8)

    @Benchmark
    fun jacksonToJsonBytes(state: BenchmarkState): ByteArray =
        state.jacksonMapper.writeValueAsBytes(state.mediaContent)

    @Benchmark
    fun foryToJsonString(state: BenchmarkState): String = state.foryJson.toJson(state.mediaContent)

    @Benchmark
    fun kotlinxToJsonString(state: BenchmarkState): String =
        state.kotlinxJson.encodeToString(state.mediaContent)

    @Benchmark
    fun moshiToJsonString(state: BenchmarkState): String = state.moshiAdapter.toJson(state.mediaContent)

    @Benchmark
    fun jacksonToJsonString(state: BenchmarkState): String =
        state.jacksonMapper.writeValueAsString(state.mediaContent)

    @Benchmark
    fun foryFromJsonBytes(state: BenchmarkState): MediaContent =
        state.foryJson.fromJson(state.jsonBytes, MediaContent::class.java)

    @Benchmark
    fun kotlinxFromJsonBytes(state: BenchmarkState): MediaContent =
        state.kotlinxJson.decodeFromString(state.jsonBytes.toString(StandardCharsets.UTF_8))

    @Benchmark
    fun moshiFromJsonBytes(state: BenchmarkState): MediaContent =
        requireNotNull(state.moshiAdapter.fromJson(state.jsonBytes.toString(StandardCharsets.UTF_8)))

    @Benchmark
    fun jacksonFromJsonBytes(state: BenchmarkState): MediaContent =
        state.jacksonMapper.readValue(state.jsonBytes, MediaContent::class.java)

    @Benchmark
    fun foryFromJsonString(state: BenchmarkState): MediaContent =
        state.foryJson.fromJson(state.jsonString, MediaContent::class.java)

    @Benchmark
    fun kotlinxFromJsonString(state: BenchmarkState): MediaContent =
        state.kotlinxJson.decodeFromString(state.jsonString)

    @Benchmark
    fun moshiFromJsonString(state: BenchmarkState): MediaContent =
        requireNotNull(state.moshiAdapter.fromJson(state.jsonString))

    @Benchmark
    fun jacksonFromJsonString(state: BenchmarkState): MediaContent =
        state.jacksonMapper.readValue(state.jsonString, MediaContent::class.java)
}
