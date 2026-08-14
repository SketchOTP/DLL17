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

// D010: the full disclosure of the scripted comparator, plus the matched
// objective probe across the two scripted cohorts and FULL. Replaces the ad-hoc
// diagnostic whose numbers were quoted in prose but checked by nothing.
tasks.register<JavaExec>("baselineManifest") {
    group = "verification"
    mainClass.set("com.animusmachinae.dll17.research.aliveness.sim.BaselineCoverageManifest")
    classpath = sourceSets["main"].runtimeClasspath
}

tasks.test {
    useJUnitPlatform()
}
