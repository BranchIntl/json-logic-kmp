plugins {
    // Declared but not applied here: this puts the Kotlin plugins on the root buildscript
    // classpath, which subprojects share, so the binary-compatibility-validator applied below can
    // load the Kotlin Multiplatform extension classes of the projects it inspects. Applying them
    // only in :lib puts them in a child classloader BCV cannot see.
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false

    alias(libs.plugins.binary.compatibility.validator)
}

// Applied at the root because binary-compatibility-validator configures every project in the
// build from wherever it is applied; the root itself has no Kotlin plugin, so it registers no
// dump or check tasks there.
//
// KLib ABI validation is experimental in BCV 0.17.0, but deterministic here: iosArm64 and
// iosSimulatorArm64 main klibs compile without a full Xcode install (only linking a test
// binary needs the Xcode toolchain), and wasmJs needs nothing platform-specific. Without this,
// BCV only validates the jvm dump; it does not support this project's androidLibrary target
// either way, so klib is the only route to covering the native and wasmJs public surface.
apiValidation {
    // :playground is a demo app with no published API to validate.
    ignoredProjects.add("playground")
    ignoredProjects.add("playground-js-plain")

    @OptIn(kotlinx.validation.ExperimentalBCVApi::class)
    klib {
        enabled = true
    }
}
