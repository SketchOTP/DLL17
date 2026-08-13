// Pure Kotlin/JVM module. The Android Gradle plugin is deliberately not applied
// here: core-* modules must build and test with no Android framework on the
// classpath (Implementation Plan E2E, section 2).
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    explicitApi()
    jvmToolchain(17)
}

dependencies {
    api(project(":core-crypto"))
    api(project(":core-math"))

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
