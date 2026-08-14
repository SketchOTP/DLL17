pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "DLL17"

// R000 module topology, per Implementation Plan E2E section 2.
// core-* and desktop-runner are pure Kotlin/JVM. android-host is the only
// module permitted to depend on the Android framework.
include(":core-math")
include(":core-crypto")
include(":core-state")
// R002 continuity and durability (directive D007). Pure Kotlin/JVM like every
// other core-* module.
include(":core-continuity")
// R012 production persistence, recovery and identity substrate (directive D011).
// Pure Kotlin/JVM like every other core-* module.
include(":core-persistence")
include(":core-recovery")
// R012 network substrate (directive D014). The S3-compatible recovery provider
// and the identity-authority transport client. Kept out of `core-recovery` so
// that the provider-neutral contract cannot acquire a transport dependency by
// accident: recovery correctness stays provable with no network at all.
include(":core-recovery-net")
include(":desktop-runner")
include(":android-host")

// A000 disposable aliveness research track (directive D008). Isolated from the
// production organism dependency graph: nothing under `research/` is depended
// on by any core-* module, `desktop-runner` or `android-host`.
include(":research:aliveness-spike:cohorts")
include(":research:aliveness-spike:accelerated-sim")
include(":research:aliveness-spike:analysis")
include(":research:aliveness-spike:realtime-viewer")
// D016-C agentic A001 governance/review harness. Depends on nothing at all, so
// a reviewer cannot import the organism it adjudicates.
include(":research:aliveness-spike:agentic-review")

// D011 persistence backend evaluation harness. Isolated like `research/`:
// nothing under `benchmarks/` is depended on by any production module, and it is
// the only place a non-production candidate library may be linked.
include(":benchmarks:persistence-bench")

// The separately deployable identity-epoch authority. Deliberately not part of
// the organism core dependency graph: normal local runtime never calls it for
// cognition, physiology, memory, action selection or ordinary startup.
include(":services:identity-authority")

// The in-repository S3-compatible qualification endpoint (directive D014).
// Isolated like `benchmarks/` and `research/`: nothing in the organism
// dependency graph depends on it, and it is never shipped. It exists so the
// network path — real sockets, real HTTP, real SigV4 verification — is exercised
// by a check that runs anywhere, including CI. It does not replace qualification
// against a real third-party endpoint, which D014 requires separately.
include(":services:s3-qualification-endpoint")
