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

// D016-M-R1: offline readiness check. This task must never contact a model.
tasks.register<JavaExec>("a001V2FormalPreflight") {
    group = "verification"
    mainClass.set(
        "com.animusmachinae.dll17.research.aliveness.agentic.A001V2FormalExecutionRunner",
    )
    classpath = sourceSets["main"].runtimeClasspath
    args("--root=${rootProject.projectDir.absolutePath}", "--preflight")
}

// D016-P P0: verify the versioned D016-O input namespace without contacting a
// provider or creating execution evidence.
tasks.register<JavaExec>("d016PFormalPreflight") {
    group = "verification"
    mainClass.set(
        "com.animusmachinae.dll17.research.aliveness.agentic.D016PFormalPreflight",
    )
    classpath = sourceSets["main"].runtimeClasspath
    val inputRoot = project.findProperty("d016PInputRoot")?.toString()
        ?: error("-Pd016PInputRoot is required")
    args(
        "--root=${rootProject.projectDir.absolutePath}",
        "--input-root=$inputRoot",
        "--preflight",
    )
}

// D016-P: one-shot frozen OpenAI execution. The input namespace and output
// namespace are explicit so D016-M evidence remains immutable and the model
// call cannot silently fall back to the old manifest.
tasks.register<JavaExec>("d016PFormalExecution") {
    group = "verification"
    mainClass.set(
        "com.animusmachinae.dll17.research.aliveness.agentic.D016PFormalExecution",
    )
    classpath = sourceSets["main"].runtimeClasspath
    val inputRoot = project.findProperty("d016PInputRoot")?.toString()
        ?: error("-Pd016PInputRoot is required")
    val outputRoot = project.findProperty("d016POutputRoot")?.toString()
        ?: error("-Pd016POutputRoot is required")
    val stage = project.findProperty("d016PStage")?.toString()
        ?: error("-Pd016PStage is required")
    args(
        "--root=${rootProject.projectDir.absolutePath}",
        "--input-root=$inputRoot",
        "--output-root=$outputRoot",
        "--stage=$stage",
        "--run",
    )
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
