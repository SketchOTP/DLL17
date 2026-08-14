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
    // D016-C: the activation audit derives its agentic-governance items from the
    // harness itself rather than restating them, so the audit cannot report a
    // qualification the harness does not have.
    implementation(project(":research:aliveness-spike:agentic-review"))
    implementation(project(":core-math"))
    testImplementation(kotlin("test"))
}

application {
    mainClass.set("com.animusmachinae.dll17.research.aliveness.analysis.CuriosityEnvelopeSearch")
}

// D010: synthetic dry run of the A001 activation package. Produces the
// preregistered analysis, the sealed pilot channel and the feasibility budget
// against fixtures. No human data is involved and none may be.
tasks.register<JavaExec>("a001DryRun") {
    group = "verification"
    mainClass.set("com.animusmachinae.dll17.research.aliveness.analysis.A001DryRun")
    classpath = sourceSets["main"].runtimeClasspath
}

tasks.register<JavaExec>("governanceAudit") {
    group = "verification"
    mainClass.set("com.animusmachinae.dll17.research.aliveness.analysis.AlivenessGovernanceAudit")
    classpath = sourceSets["main"].runtimeClasspath
}

tasks.test {
    useJUnitPlatform()
}
