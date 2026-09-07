# Kotlin JSON Benchmarks

This project compares JSON serialization and deserialization of the same immutable Kotlin
`MediaContent` model with Fory JSON Kotlin, kotlinx.serialization, Moshi code generation, and
Jackson Kotlin.

Fory uses `org.apache.fory:fory-json-kotlin:1.7.1`, `ForyJsonKotlin.builder()`, and a retained
`jsonTypeRef<MediaContent>()`. The model and correctness checks follow the
[Apache Fory Kotlin JSON harness](https://github.com/apache/fory/tree/main/benchmarks/kotlin).

## Methodology

- All model properties are `val`, and the models have no public zero-argument constructor.
  Required constructor arguments, nullable members, and compiler defaults are exercised.
- Every library emits null properties and default values. Setup verifies fixture reads through
  both representations, all eight own-output round trips, and JSON-tree equality across all
  eight outputs before timing. The expected object is defined independently of each decoder.
- Fory's Kotlin type token, the kotlinx serializer, Moshi's generated adapter, and Jackson's typed
  reader/writer are retained outside the measured methods. Fory uses synchronous codec compilation
  so generation finishes before measurement. Runtime code generation remains enabled.
- String operations exclude UTF-8 conversion. Fory and Jackson use direct byte-array APIs;
  kotlinx.serialization uses `encodeToStream` / `decodeFromStream` with byte-array streams, and
  Moshi uses its Okio `Buffer` APIs. Buffer/stream allocation and byte-array extraction are included
  in the byte operations. None of those paths converts through an intermediate String.
- JMH executes all 16 methods in one invocation. A failed benchmark fails the run.

The Eishay fixture is checked at setup against SHA-256
`8faba2f57ab397f319aced5cf1e8411a76785557d4c7d1703ec9d540354310a1`.

## Performance results

Higher throughput is better. These results describe this model and configuration on one machine;
they are not a general performance guarantee. The previous report used mutable models, different
byte APIs, and another machine, so it is not a controlled Fory version-to-version comparison.

### String

![Kotlin JSON String benchmark throughput](results/string_throughput.png)

### UTF-8 bytes

![Kotlin JSON UTF-8 bytes benchmark throughput](results/utf8_bytes_throughput.png)

### Throughput

Scores are rounded to the nearest operation per second. Charts include the errors reported by JMH;
full precision, confidence intervals, and individual samples are retained in the raw JSON.

| Representation | Operation | Fory JSON Kotlin ops/s | kotlinx.serialization ops/s | Moshi ops/s | Jackson Kotlin ops/s |
| --- | --- | ---: | ---: | ---: | ---: |
| String | Serialize | 8,463,544 | 2,331,337 | 1,009,654 | 2,166,486 |
| String | Deserialize | 3,963,797 | 631,164 | 513,445 | 508,099 |
| UTF-8 bytes | Serialize | 12,314,484 | 1,059,643 | 1,015,884 | 1,954,090 |
| UTF-8 bytes | Deserialize | 4,449,742 | 729,606 | 701,187 | 531,665 |

### Fory JSON Kotlin throughput advantage

Each ratio is Fory JSON Kotlin throughput divided by the other library's throughput for the same
operation, computed from unrounded scores.

| Representation | Operation | vs. kotlinx.serialization | vs. Moshi | vs. Jackson Kotlin |
| --- | --- | ---: | ---: | ---: |
| String | Serialize | 3.63× | 8.38× | 3.91× |
| String | Deserialize | 6.28× | 7.72× | 7.80× |
| UTF-8 bytes | Serialize | 11.62× | 12.12× | 6.30× |
| UTF-8 bytes | Deserialize | 6.10× | 6.35× | 8.37× |

In this run, Fory JSON Kotlin delivered 3.63× to 12.12× the throughput of the compared libraries. It had the highest throughput in all four operations.

### Benchmark environment

- Date: 2026-09-07
- Measured source commit: `cbd1b2a4d018d7686a9f5662ef1ed2b3d76e9823`
- Machine: Apple M5, arm64, 10 CPU cores, 32 GiB memory
- OS: macOS 26.4 (25E246)
- JDK: Homebrew OpenJDK 25.0.3, OpenJDK 64-Bit Server VM
- Kotlin/compiler serialization plugin: 2.3.20
- KSP: 2.3.8; Moshi code generation: 1.15.2
- Gradle: 9.3.0; Gradle JMH plugin: 0.7.3; JMH: 1.37
- Configuration: 1 fork, 1 thread, 3 × 2-second warmup iterations, 5 × 2-second measurement iterations
- JVM option: `--add-opens=java.base/java.lang.invoke=ALL-UNNAMED`
- Libraries: Fory JSON Kotlin 1.7.1 (JSON and core also resolve to 1.7.1), kotlinx.serialization
  1.11.0, Moshi 1.15.2, Jackson Kotlin 2.22.1

The complete [JMH JSON](results/benchmark_results.json), [process output](results/jmh-output.txt),
and [environment record](results/environment.json) describe the same run.

## Requirements

- JDK 25
- Python with matplotlib and NumPy to regenerate the figures

## Verify

```bash
./gradlew test jmhClasses
```

Tests cover fixture decoding, String/byte round trips, equivalent JSON trees, immutable constructor
requirements, Kotlin compiler defaults, and rejection of missing or null required properties.

## Run

```bash
./gradlew jmh --console=plain > results/jmh-output.txt 2>&1
```

Results are written to `build/reports/jmh/results.json`.

Regenerate the checked-in charts from the completed run:

```bash
cp build/reports/jmh/results.json results/benchmark_results.json
python3 -m pip install matplotlib numpy
python3 plot_json_benchmark.py
```

Update the report tables and environment record from that same run when publishing new results.
