// D011 backend-evaluation harness. NOT production code and not on any
// production module's dependency path. It exists so the persistence backend can
// be selected from measurement rather than from convention, and it is the only
// module permitted to link a candidate library that production does not use.
plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // The direct-SQLite candidate. Benchmark scope only: no production module
    // depends on this module, and the selected backend links nothing here.
    implementation(libs.sqlite.jdbc)
}

application {
    mainClass.set("com.animusmachinae.dll17.bench.PersistenceBackendBenchmark")
}
