// Pure Kotlin/JVM module. R002 continuity and durability: the four clocks,
// trusted elapsed time, offline reconciliation, journal generations, durability
// admission, platform protection, and the encrypted-record boundary.
//
// Android is deliberately absent here for the same reason it is absent from the
// other core-* modules: continuity correctness must be provable off-device, and
// a module that could reach for `SystemClock` would be able to hide a platform
// dependency inside canonical logic.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    explicitApi()
    jvmToolchain(17)
}

dependencies {
    api(project(":core-state"))
    api(project(":core-crypto"))
    api(project(":core-math"))

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
