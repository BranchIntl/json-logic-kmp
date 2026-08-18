import java.util.Base64

plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

/**
 * Stages the library's own tests for its number handling, so they run on Kotlin/JS as well.
 *
 * `CanonicalNumber` and `BigUInt` do their work in `Long` bit patterns, and Kotlin/JS is the only
 * target that emulates `Long` instead of having one, so this is the target most able to disagree
 * with the others and the one with no coverage. Named file by file because the rest of the
 * library's `commonTest` either needs the generated fixture corpus or asserts `is Double` guards
 * that Kotlin/JS cannot satisfy, having one runtime type for both `Int` and `Double`.
 *
 * A copy, because a Kotlin source directory is a directory and these are three files inside a
 * tree of a hundred.
 */
val stageEngineNumberTests by tasks.registering(Sync::class) {
    from(rootProject.layout.projectDirectory.dir("lib/src/commonTest/kotlin")) {
        include("co/branch/jsonlogic/ast/JsonLogicParserNumberLiteralTest.kt")
        include("co/branch/jsonlogic/internal/EcmaStringifyTest.kt")
        include("co/branch/jsonlogic/internal/JavaStringifyTest.kt")
    }
    into(layout.buildDirectory.dir("engine-number-tests"))
}

/**
 * Hands the shipped stylesheet to the browser tests, which run on a page Karma assembles rather
 * than on `index.html`. A layout test needs the real declarations: one that carried its own copy
 * of them would keep passing while the stylesheet moved out from under it.
 *
 * The monospace face rides along as its own bytes. Karma serves the tests from a directory that
 * holds no `fonts/`, so the relative URL the page uses resolves to nothing there, and a face that
 * does not arrive is not an error — it is the fallback face, quietly measured instead, in the one
 * suite whose numbers are made of text metrics.
 */
val generateStylesheetSource by tasks.registering {
    val page = layout.projectDirectory.file("src/jsMain/resources/index.html")
    val faceUrl = "fonts/JetBrainsMono-Regular-subset.woff2"
    val face = layout.projectDirectory.file("src/jsMain/resources/$faceUrl")
    val sourceDir = layout.buildDirectory.dir("generated-stylesheet")
    inputs.file(page)
    inputs.file(face)
    outputs.dir(sourceDir)

    doLast {
        val markup = page.asFile.readText()
        // substringAfter and substringBefore return their whole input when the delimiter is
        // absent, so without this a marker that moved would emit the page itself as the
        // stylesheet, and the first sign of it would be the layout suite measuring against markup.
        listOf("<style>", "</style>").forEach { marker ->
            check(marker in markup) { "${page.asFile} holds no $marker to slice the stylesheet at" }
        }

        val inlined = "data:font/woff2;base64," + Base64.getEncoder().encodeToString(face.asFile.readBytes())
        val css = markup
            .substringAfter("<style>")
            .substringBefore("</style>")
            .replace(faceUrl, inlined)
        val file = sourceDir.get().asFile.resolve("co/branch/jsonlogic/playground/Stylesheet.kt")
        file.parentFile.mkdirs()
        file.writeText(
            "package co.branch.jsonlogic.playground\n\n" +
                "internal val Stylesheet: String = \"\"\"" + css.replace("$", "\${'$'}") + "\"\"\"\n",
        )
    }
}

kotlin {
    js(IR) {
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
        jsTest {
            // The Provider registers the staged dir and its task dependency together.
            kotlin.srcDir(stageEngineNumberTests.map { it.destinationDir })
            kotlin.srcDir(generateStylesheetSource.map { it.outputs.files.singleFile })
        }
        jsTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
