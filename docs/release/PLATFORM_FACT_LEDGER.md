# Platform fact ledger

Primary-source platform facts pinned at R000, to be re-verified whenever the
Android or toolchain targets change.

| Fact | Value | Verified |
|---|---|---|
| Android platform package for `compileSdk = 37` | `platforms;android-37.0` | 2026-08-12, from the Android SDK repository index |
| Build tools | `build-tools;37.0.0` | 2026-08-12, from the Android SDK repository index |
| Android Gradle Plugin | `9.3.1` | 2026-08-12, resolved from Google's Maven repository |
| Gradle | `9.5.0` | 2026-08-12, resolved from the Gradle distribution service |
| Kotlin | `2.4.10` | 2026-08-12, resolved on both the JVM and Android compile classpaths |
| JDK | Temurin 17.0.20+8 | 2026-08-12, `java -version` on the build host |
| AGP 9 Kotlin support | AGP 9 has built-in Kotlin support and rejects `org.jetbrains.kotlin.android` | 2026-08-12, observed directly as a build failure |

Time, process exit, backup and device transfer, Keystore, thermal protection,
asset delivery and background-work facts are not yet pinned. They are required
before the subsystems that depend on them are implemented, not before R000
completes.
