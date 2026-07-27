plugins {
    `java-library`
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    api(libs.gson)
    testImplementation(libs.junit)
}

tasks.withType<JavaCompile>().configureEach {
    // The engine sources are the frozen parity oracle for the Kotlin port; they are never
    // modernized, so their deprecation and unchecked warnings would be permanent noise.
    options.compilerArgs.addAll(listOf("-Xlint:none", "-nowarn"))
}

tasks.test {
    useJUnit()
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
