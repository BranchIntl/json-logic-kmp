plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    js(IR) {
        outputModuleName = "playground-js-plain"

        browser {
            commonWebpackConfig {
                // index.html's script tag points at this name.
                outputFileName = "playground.js"
            }
        }

        binaries.executable()
    }

    sourceSets {
        jsMain {
            // The library is compiled from source here instead of resolved as a project
            // dependency, because resolving it would mean a js target on :lib — a published
            // coordinate that can never be unpublished and a target this project would then owe
            // support for, when a browser demo is the only thing that would ever ask for it.
            //
            // The cost is that :lib's sources compile into this module, so its `internal`
            // declarations are visible to playground code. They are not part of the deal: the
            // library's boundary is what :lib publishes, and reaching past it here would be a bug
            // the compiler cannot report.
            kotlin.srcDir(rootProject.layout.projectDirectory.dir("lib/src/commonMain/kotlin"))
        }
        jsMain.dependencies {
            // :lib declares this `api`, which its own consumers inherit; compiling its sources
            // here makes it this module's to declare.
            implementation(libs.kotlinx.serialization.json)
        }
        jsTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
