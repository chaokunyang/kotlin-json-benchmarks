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

## Performance results

Higher throughput is better. These results describe this benchmark run on one machine; they are
not a general performance guarantee.

### String

![Kotlin JSON String benchmark throughput](results/string_throughput.png)

### UTF-8 bytes

![Kotlin JSON UTF-8 bytes benchmark throughput](results/utf8_bytes_throughput.png)

### Throughput

| Representation | Operation   | Fory JSON ops/sec | kotlinx.serialization ops/sec | Moshi ops/sec | Jackson ops/sec |
| -------------- | ----------- | ----------------: | ----------------------------: | ------------: | --------------: |
| String         | Serialize   |         7,768,791 |                     2,196,901 |     1,019,311 |       2,086,059 |
| String         | Deserialize |         3,535,239 |                       608,065 |       525,298 |         510,031 |
| UTF-8 bytes    | Serialize   |        10,970,448 |                     2,171,843 |     1,006,488 |       1,962,426 |
| UTF-8 bytes    | Deserialize |         3,840,859 |                       563,904 |       461,808 |         536,068 |

### Fory JSON throughput advantage

Each value is `Fory JSON throughput / compared library throughput` for the same operation. In this
run, Fory JSON delivered 3.54x to 10.90x the throughput of the compared libraries.

| Representation | Operation   | vs kotlinx.serialization | vs Moshi | vs Jackson |
| -------------- | ----------- | -----------------------: | -------: | ---------: |
| String         | Serialize   |                    3.54x |    7.62x |      3.72x |
| String         | Deserialize |                    5.81x |    6.73x |      6.93x |
| UTF-8 bytes    | Serialize   |                    5.05x |   10.90x |      5.59x |
| UTF-8 bytes    | Deserialize |                    6.81x |    8.32x |      7.16x |

### Benchmark environment

- Date: 2026-08-12
- Source commit: `a132b4f`
- Machine: Apple M4 Pro, arm64
- OS: macOS 15.7.2
- JDK: OpenJDK 25.0.3
- JMH: 1.37
- Configuration: 1 fork, 1 thread, 3 × 2-second warmup iterations, 5 × 2-second measurement
  iterations
- Libraries: Fory JSON 1.6.0, kotlinx.serialization 1.11.0, Moshi 1.15.2 with code generation,
  Jackson Kotlin 2.22.1

The complete JMH output is available in
[`results/benchmark_results.json`](results/benchmark_results.json).

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

Regenerate the checked-in charts from a completed run with:

```bash
cp build/reports/jmh/results.json results/benchmark_results.json
python3 -m pip install matplotlib numpy
python3 plot_json_benchmark.py
```
