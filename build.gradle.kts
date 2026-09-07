plugins {
    kotlin("jvm") version "2.3.20"
    kotlin("plugin.serialization") version "2.3.20"
    id("com.google.devtools.ksp") version "2.3.8"
    id("me.champeau.jmh") version "0.7.3"
}

group = "org.apache.fory.benchmark"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.apache.fory:fory-json-kotlin:1.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    implementation("com.squareup.moshi:moshi:1.15.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.22.1")

    ksp("com.squareup.moshi:moshi-kotlin-codegen:1.15.2")

    jmh("org.openjdk.jmh:jmh-core:1.37")
    jmh("org.openjdk.jmh:jmh-generator-annprocess:1.37")

    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(25)
}

tasks.test {
    useJUnitPlatform()
}

jmh {
    warmupIterations.set(3)
    warmup.set("2s")
    iterations.set(5)
    timeOnIteration.set("2s")
    fork.set(1)
    threads.set(1)
    benchmarkMode.set(listOf("thrpt"))
    timeUnit.set("s")
    includes.set(listOf("MediaContentBenchmark"))
    jvmArgsAppend.set(listOf("--add-opens=java.base/java.lang.invoke=ALL-UNNAMED"))
    resultFormat.set("JSON")
    failOnError.set(true)
    resultsFile.set(layout.buildDirectory.file("reports/jmh/results.json"))
}
