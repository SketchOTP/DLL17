// The in-repository S3-compatible qualification endpoint (D014).
//
// Isolated like `benchmarks/` and `research/`: no core-* module, neither host and
// no shipped artifact depends on it. It exists so the network path — real
// sockets, real HTTP, real SigV4 verification, real failure injection — is
// exercised by a check that runs anywhere, including CI, where reaching a
// third-party object store is neither available nor appropriate.
//
// It does **not** stand in for qualification against a real compatible endpoint.
// D014 requires that separately, and `R014_NETWORK_GATE.md` records it
// separately.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    explicitApi()
    jvmToolchain(17)
}

// It depends on `core-crypto` for the HMAC and SHA-256 primitives and on nothing
// else — in particular **not** on `core-recovery-net`. The signature check here
// is an independent second implementation of SigV4 rather than a call back into
// the signer under test, for the same reason `tools/generate_lookup_tables.py` is
// an independent Python implementation of the codec: a verifier that shares code
// with the thing it verifies agrees with it by construction.
dependencies {
    implementation(project(":core-crypto"))

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
