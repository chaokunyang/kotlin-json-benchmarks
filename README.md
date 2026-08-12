# Kotlin JSON Benchmarks

This project compares JSON serialization and deserialization of the same `MediaContent` payload
with:

- Fory JSON
- kotlinx.serialization
- Moshi code generation
- Jackson Kotlin

The benchmark follows Apache Fory's Java JSON benchmark shape. It measures both `String` and UTF-8
`ByteArray` operations with one thread, one fork, three 2-second warmup iterations, and five
2-second measurement iterations. Library instances and adapters are created outside the measured
methods.

Fory JSON and Jackson use direct UTF-8 byte APIs. kotlinx.serialization and Moshi expose String
APIs for this setup, so their byte benchmarks include UTF-8 conversion.

## Requirements

- JDK 25

## Verify

```bash
./gradlew test jmhClasses
```

## Run

```bash
./gradlew jmh
```

Results are written to `build/reports/jmh/results.json`.
