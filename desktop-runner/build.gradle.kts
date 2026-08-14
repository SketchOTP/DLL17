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
tasks.register<JavaExec>("r012Performance") {
    group = "verification"
    mainClass.set("com.animusmachinae.dll17.desktop.R012PerformanceMain")
    classpath = sourceSets["main"].runtimeClasspath
}
