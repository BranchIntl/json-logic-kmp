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

    @OptIn(kotlinx.validation.ExperimentalBCVApi::class)
    klib {
        enabled = true
    }
}

// The Kotlin plugin pins its npm tooling to exact versions. These overrides move those tools, and
// the transitive packages they request, past published security advisories.
plugins.withType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin> {
    the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension>().versions.apply {
        webpack.version = "5.108.1"
        webpackDevServer.version = "5.2.7"
    }
}

plugins.withType<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin> {
    the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension>().apply {
        resolution("serialize-javascript", "^7.1.3")
        resolution("uuid", "^11.1.1")
        resolution("**/mocha/diff", "^8.0.3")
        resolution("**/mocha/glob", "^14.0.2")
        resolution("**/mocha/minimatch", "^11.0.1")
        resolution("**/karma-webpack/minimatch", "^11.0.1")
    }
}
