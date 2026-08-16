// D016-C agentic governance/review harness. Research code, not production.
//
// Deliberately depends on nothing. The harness must be able to review evidence
// about the organism without being able to run the organism, and a reviewer that
// could import the thing it adjudicates is not isolated from it.
plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(kotlin("test"))
}

application {
    mainClass.set(
        "com.animusmachinae.dll17.research.aliveness.agentic.AgenticReviewQualification",
    )
}

// Regenerates the committed agentic-review qualification evidence. CI runs this
// and diffs it against the committed file, so the harness cannot change without
// its evidence moving with it.
tasks.register<JavaExec>("agenticReviewQualification") {
    group = "verification"
    mainClass.set(
        "com.animusmachinae.dll17.research.aliveness.agentic.AgenticReviewQualification",
    )
    classpath = sourceSets["main"].runtimeClasspath
}

// D016-L: contract-only dry run. It emits zero formal AI executions and zero
// owner reviews; CI compares it byte-for-byte with the committed evidence.
tasks.register<JavaExec>("a001V2DryRun") {
    group = "verification"
    mainClass.set("com.animusmachinae.dll17.research.aliveness.agentic.A001V2DryRun")
    classpath = sourceSets["main"].runtimeClasspath
}

tasks.test {
    useJUnitPlatform()
}

// D016-G. Both contact the owner's private router and need a runtime credential,
// so neither is part of CI or of `check`. The shakeout uses a synthetic review
// and is safe to re-run; the formal qualification spends the single permitted
// attempt and must not be re-run until it passes.
tasks.register<JavaExec>("paragonShakeout") {
    group = "verification"
    mainClass.set("com.animusmachinae.dll17.research.aliveness.agentic.ParagonShakeout")
    classpath = sourceSets["main"].runtimeClasspath
}

tasks.register<JavaExec>("paragonFormalQualification") {
    group = "verification"
    mainClass.set(
        "com.animusmachinae.dll17.research.aliveness.agentic.ParagonFormalQualification",
    )
    classpath = sourceSets["main"].runtimeClasspath
}
