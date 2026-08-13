# Dependency and license inventory

Declared, exactly pinned dependencies. Versions are authoritative in
`gradle/libs.versions.toml`; this table records what they are and why.

| Dependency | Version | Licence | Why it is present |
|---|---|---|---|
| `com.android.tools.build:gradle` | 9.3.1 | Apache-2.0 | Android application build. |
| `org.jetbrains.kotlin` toolchain | 2.4.10 | Apache-2.0 | Kotlin compilation on JVM and Android. |
| `org.jetbrains.kotlin.plugin.compose` | 2.4.10 | Apache-2.0 | Compose compiler for the Android shell. |
| `androidx.compose:compose-bom` | 2026.06.00 | Apache-2.0 | Pins the Compose library set. |
| `androidx.compose.ui:ui` | via BOM | Apache-2.0 | Shell rendering. |
| `androidx.compose.ui:ui-tooling-preview` | via BOM | Apache-2.0 | Shell preview support. |
| `androidx.compose.material3:material3` | via BOM | Apache-2.0 | Shell typography and surface. |
| `androidx.core:core-ktx` | 1.19.0 | Apache-2.0 | Android platform interop. |
| `androidx.activity:activity-compose` | 1.13.0 | Apache-2.0 | Compose activity host. |
| `androidx.lifecycle:lifecycle-runtime-ktx` | 2.11.0 | Apache-2.0 | Lifecycle-aware host scaffolding. |
| `junit:junit` | 4.13.2 | EPL-1.0 | Android unit tests. |
| `org.jetbrains.kotlin:kotlin-test` | 2.4.10 | Apache-2.0 | JVM module tests. |

Licence identifiers are recorded as published by each project. No dependency is
redistributed in source form by this repository.

## This project's own source licence

Resolved under architect directive D005: **proprietary, all rights reserved, no
redistribution**, matching the `ProjectIdentityBuildContractV1` licensing
clause. The root `LICENSE` file states it.

The repository previously carried an MIT `LICENSE`, applied by the repository
owner in initial commit `96a1f07` before the contract was frozen. D005 directed
that the MIT grant be removed and replaced; that was done, and recorded as
IMPL-0004 in `docs/decisions/DECISION_LOG.md`.

Two facts are recorded rather than claimed away:

- The repository is public and its visibility was **not** changed under D005,
  because the directive explicitly excluded that. Public visibility grants no
  licence, and the notice says so, but the source remains readable by anyone.
- An MIT grant was published for the commits between `96a1f07` and the D005
  commit. Removing the file governs the project going forward; whether the
  earlier published grant remains effective for those specific historical
  revisions is a legal question for the copyright holder, not an implementation
  decision. It is flagged here so it is not mistaken for a settled matter.

## Algorithms implemented from published specifications (R001)

R001 implements two published algorithms. Neither introduces a third-party
licence obligation, because no third-party source was copied into this
repository.

| Algorithm | Source | Licence position | How it was implemented |
|---|---|---|---|
| SHA-256 | FIPS PUB 180-4 (NIST) | A US government publication; an algorithm specification is not copyrightable | Written from the published pseudocode and constants, verified against the document's own test vectors and differentially against `java.security.MessageDigest` |
| SplitMix64 | Steele, Lea and Flood, *Fast Splittable Pseudorandom Number Generators*, OOPSLA 2014; Vigna's reference implementation | Vigna's reference is released to the public domain (CC0), so even copying would carry no obligation. Nothing was copied regardless | Written from the published mixing function and constants |
| `GAMMA` = `0x9E3779B97F4A7C15` | Odd 64-bit approximation of the golden ratio, in wide public use | No licence claim attaches to a mathematical constant | Transcribed and asserted by test against its published unsigned hex |

The distinction between *implementing a specification* and *copying an
implementation* is deliberate and is what keeps the deterministic core
first-party under this repository's proprietary licence.

No new third-party Gradle dependency was added by R001 other than the AndroidX
test-instrumentation artifacts required to execute the determinism matrix on a
device; those are declared in `gradle/libs.versions.toml` as exact pins under the
Apache License 2.0, consistent with the rest of the AndroidX rows above.
