// A000 disposable research code. Not production.
//
// The only production dependency permitted by the canonical plan is the frozen
// R001 fixed-point numeric library. `core-crypto` arrives transitively because
// `core-math` publishes it as `api` for lookup-table verification; no source
// file in this module imports it, and `SpikeIsolationTest` enforces that.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core-math"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
