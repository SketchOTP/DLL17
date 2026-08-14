// Headless JVM runner. Pure Kotlin/JVM: it exercises the core modules outside
// Android so that canonical behavior can later be replayed off-device.
plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

kotlin {
    explicitApi()
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core-math"))
    implementation(project(":core-crypto"))
    implementation(project(":core-state"))
    implementation(project(":core-continuity"))
    // R012 (D011). The runner is the one module allowed to see both the core
    // and the separately deployable authority service, because wiring them
    // together is exactly what a host does.
    implementation(project(":core-persistence"))
    implementation(project(":core-recovery"))
    implementation(project(":services:identity-authority"))
    // R014 (D014). The network provider, the authority transport and the
    // in-repository qualification endpoint. Same reasoning as above: the host is
    // the only place allowed to see the core and the deployable services at
    // once, so the network qualification kernel lives here rather than in a
    // core module that must not know a network exists.
    implementation(project(":core-recovery-net"))
    implementation(project(":services:s3-qualification-endpoint"))
    testImplementation(kotlin("test"))
}

application {
    mainClass.set("com.animusmachinae.dll17.desktop.DesktopRunnerKt")
}

tasks.test {
    useJUnitPlatform()
}

// R012 qualification kernel. Separate from the R001/R002 reference run because
// it writes to real storage and spawns child JVMs for the process-death matrix.
tasks.register<JavaExec>("r012Qualification") {
    group = "verification"
    mainClass.set("com.animusmachinae.dll17.desktop.R012QualificationMain")
    classpath = sourceSets["main"].runtimeClasspath
}

// R012 measured performance. Separate task because it writes to real storage and
// its numbers are machine-specific evidence rather than a reproducible digest.
// R014 network qualification. Separate task because it binds real sockets and,
// when the environment names one, talks to an external object store.
tasks.register<JavaExec>("r014NetworkQualification") {
    group = "verification"
    mainClass.set("com.animusmachinae.dll17.desktop.R014NetworkQualificationMain")
    classpath = sourceSets["main"].runtimeClasspath
    // Only the variables the kernel documents, so a stray ambient credential in
    // the developer's shell cannot silently change what was qualified.
    for (name in listOf(
        "DLL17_RECOVERY_S3_ENDPOINT",
        "DLL17_RECOVERY_S3_REGION",
        "DLL17_RECOVERY_S3_BUCKET",
        "DLL17_RECOVERY_S3_ACCESS_KEY_ID",
        "DLL17_RECOVERY_S3_SECRET_ACCESS_KEY",
        "DLL17_RECOVERY_S3_SESSION_TOKEN",
    )) {
        System.getenv(name)?.let { environment(name, it) }
    }
}

tasks.register<JavaExec>("r012Performance") {
    group = "verification"
    mainClass.set("com.animusmachinae.dll17.desktop.R012PerformanceMain")
    classpath = sourceSets["main"].runtimeClasspath
}
