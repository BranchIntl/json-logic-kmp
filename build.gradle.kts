import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

group = "co.branch"
version = "0.1.0-SNAPSHOT"

kotlin {
    jvm {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    androidLibrary {
        namespace = "co.branch.jsonlogic"
        compileSdk = 36
        minSdk = 21

        // Gives the Android target a JVM host-test compilation so commonTest can run on
        // the Linux CI lane without a device or emulator.
        withHostTestBuilder {}.configure {
            isIncludeAndroidResources = false
        }
    }

    iosArm64()
    iosSimulatorArm64()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        nodejs()
    }

    sourceSets {
        commonMain.dependencies {
            // Runtime artifact only: the JSON tree types are used directly, so the
            // serialization compiler plugin is not applied.
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 11
}
