// A000 disposable research code. Not production.
plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":research:aliveness-spike:cohorts"))
    implementation(project(":core-math"))
    testImplementation(kotlin("test"))
}

application {
    mainClass.set("com.animusmachinae.dll17.research.aliveness.sim.A000QualificationKernel")
}

tasks.register<JavaExec>("probe2") {
    group = "verification"
    mainClass.set("com.animusmachinae.dll17.research.aliveness.sim.Probe2")
    classpath = sourceSets["main"].runtimeClasspath
}

tasks.test {
    useJUnitPlatform()
}
