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

tasks.test {
    useJUnitPlatform()
}
