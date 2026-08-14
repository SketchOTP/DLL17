// Pure Kotlin/JVM module. R012 production persistence: the selected append-log
// backend, the snapshot store, compaction, and the local encrypted-record key
// lifecycle.
//
// Android is absent here for the same reason it is absent from every other
// core-* module. The Keystore is one implementation of `DeviceKeyContainer` and
// is not canonical authority: durability and key-lifecycle correctness must be
// provable off-device, and a module that could reach for Keystore directly
// would be able to hide a platform dependency inside canonical logic.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    explicitApi()
    jvmToolchain(17)
}

dependencies {
    api(project(":core-continuity"))
    // One-way: a cold package is restored *into* local storage and activation
    // rewraps local keys, so persistence knows about recovery. Recovery does not
    // know about persistence, because a destination device must be able to read
    // a package before it has any local organism at all.
    api(project(":core-recovery"))
    api(project(":core-state"))
    api(project(":core-crypto"))

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
