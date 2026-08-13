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
    // Canonical hashing and the canonical codec are primitives that lookup-table
    // verification depends on. core-crypto is itself pure Kotlin/JVM, so this
    // does not weaken the runtime boundary.
    api(project(":core-crypto"))

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
