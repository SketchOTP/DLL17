// A000 disposable research code. Not production.
plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":research:aliveness-spike:accelerated-sim"))
    implementation(project(":research:aliveness-spike:cohorts"))
    implementation(project(":core-math"))
    testImplementation(kotlin("test"))
}

application {
    mainClass.set("com.animusmachinae.dll17.research.aliveness.analysis.CuriosityEnvelopeSearch")
}

tasks.register<JavaExec>("governanceAudit") {
    group = "verification"
    mainClass.set("com.animusmachinae.dll17.research.aliveness.analysis.AlivenessGovernanceAudit")
    classpath = sourceSets["main"].runtimeClasspath
}

tasks.test {
    useJUnitPlatform()
}
