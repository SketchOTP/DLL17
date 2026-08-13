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
    testImplementation(kotlin("test"))
}

application {
    mainClass.set("com.animusmachinae.dll17.desktop.DesktopRunnerKt")
}

tasks.test {
    useJUnitPlatform()
}
