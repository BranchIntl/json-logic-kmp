pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    // PREFER_SETTINGS instead of FAIL_ON_PROJECT_REPOS because Kotlin/Wasm adds the
    // Node.js distribution repository at configuration time, which the stricter mode blocks.
    repositoriesMode = RepositoriesMode.PREFER_SETTINGS
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "json-logic-kmp"

include(":parity")
