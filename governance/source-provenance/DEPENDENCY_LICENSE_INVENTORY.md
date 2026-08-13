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
redistributed in source form by this repository. The project's own source
licence is proprietary with no redistribution licence, per
`ProjectIdentityBuildContractV1`.
