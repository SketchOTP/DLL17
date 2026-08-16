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
    mainClass.set("com.animusmachinae.dll17.research.aliveness.viewer.ViewerMain")
}

tasks.test {
    useJUnitPlatform()
}

// D016-M-R1: materialize only the deterministic, outward-observation bundles.
// This task never contacts a model or any network endpoint.
tasks.register<JavaExec>("a001ObservationBundles") {
    group = "verification"
    mainClass.set(
        "com.animusmachinae.dll17.research.aliveness.viewer.A001EvaluatorObservationGeneratorV1",
    )
    classpath = sourceSets["main"].runtimeClasspath
    args("--root=${rootProject.projectDir.absolutePath}")
}
