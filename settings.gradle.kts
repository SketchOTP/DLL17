pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "DLL17"

// R000 module topology, per Implementation Plan E2E section 2.
// core-* and desktop-runner are pure Kotlin/JVM. android-host is the only
// module permitted to depend on the Android framework.
include(":core-math")
include(":core-crypto")
include(":core-state")
// R002 continuity and durability (directive D007). Pure Kotlin/JVM like every
// other core-* module.
include(":core-continuity")
include(":desktop-runner")
include(":android-host")

// A000 disposable aliveness research track (directive D008). Isolated from the
// production organism dependency graph: nothing under `research/` is depended
// on by any core-* module, `desktop-runner` or `android-host`.
include(":research:aliveness-spike:cohorts")
include(":research:aliveness-spike:accelerated-sim")
include(":research:aliveness-spike:analysis")
include(":research:aliveness-spike:realtime-viewer")
