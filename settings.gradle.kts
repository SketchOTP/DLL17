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
include(":desktop-runner")
include(":android-host")
