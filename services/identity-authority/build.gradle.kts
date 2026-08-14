// The separately deployable identity-epoch authority.
//
// Deliberately outside the organism core dependency graph: no core-* module and
// neither host depends on it, and normal local runtime never calls it for
// cognition, physiology, memory, action selection or ordinary startup. Deleting
// this module breaks new recovery activation and nothing else.
//
// It depends on `core-recovery` for the protocol types only. It has no access to
// canonical organism state and no way to decrypt a recovery package.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    explicitApi()
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core-recovery"))
    implementation(project(":core-crypto"))

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
