// Pure Kotlin/JVM module. R012 recovery cryptography, the cold-package format,
// the provider-neutral recovery package store, and the identity-authority wire
// protocol.
//
// It does not depend on `core-persistence`: recovery must be able to read a
// package on a device that has no local organism yet, which is the whole point
// of cold recovery.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    explicitApi()
    jvmToolchain(17)
}

dependencies {
    api(project(":core-crypto"))
    api(project(":core-state"))

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
