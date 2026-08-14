// Pure Kotlin/JVM module. R012 network substrate (D014): the S3-compatible
// recovery-package provider and the identity-authority transport client.
//
// It has **no third-party dependency**, and that is a requirement rather than an
// accident. This code is the only network code that runs on the destination
// device during a cold recovery, and D014's external landscape check
// (`EXTERNAL_PRIOR_ART.md`, PA-0002 and PA-0003) recorded why every serious
// candidate was materially unsuitable there: each brings a second cryptographic
// provider onto a device whose key hierarchy is already frozen and qualified.
//
// The only APIs it uses are the project's own crypto and the subset of the JDK
// that is present on Android API 29. `AndroidApiSurfaceTest` reads the compiled
// classes back and enforces that, so the claim is checked rather than asserted.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    explicitApi()
    jvmToolchain(17)
}

dependencies {
    api(project(":core-recovery"))
    api(project(":core-crypto"))

    testImplementation(kotlin("test"))
    testImplementation(project(":services:s3-qualification-endpoint"))
    testImplementation(project(":services:identity-authority"))
}

tasks.test {
    useJUnitPlatform()
}
