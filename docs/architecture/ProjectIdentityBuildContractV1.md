# ProjectIdentityBuildContractV1

- Contract version: `V1`
- Status: `FROZEN`
- Frozen by: architect directive D004
- Frozen on: 2026-08-12
- Phase: R000 — greenfield project initialization

Every value below was supplied by the architect in D004. No value in this
document was selected by the implementer. Changing any frozen value requires a
new architect directive and a new contract version; it is not a build-tuning
decision.

## Project identity

| Field | Value |
|---|---|
| Repository / project | `DLL17` |
| Product display name | `Digital Living Lifeform` |
| Android application ID | `com.animusmachinae.dll17` |
| Android namespace | `com.animusmachinae.dll17` |
| Authoritative remote | `git@github.com:SketchOTP/DLL17.git` |
| Default branch | `main` |

## Toolchain

| Field | Value |
|---|---|
| Kotlin | `2.4.10` |
| JDK | `17` |
| Android Gradle Plugin | `9.3.1` |
| Gradle | `9.5.0` |
| compileSdk | `37` |
| targetSdk | `37` |
| minSdk | `29` |
| Compose BOM | `2026.06.00` |

## Release identity

| Field | Value |
|---|---|
| versionCode | `1` |
| versionName | `0.1.0-dev` |
| Build variants | `debug`, `release` |
| Product flavors | none |
| Production signing | deferred; R000 uses development/debug signing only |
| Distribution channel | not yet authorized; no release channel is configured |

## Dependency and source policy

- Dynamic dependency versions are prohibited. No `+`, no version ranges, no
  `latest.release`. Every version is an exact pin in `gradle/libs.versions.toml`.
- Source licensing policy: proprietary, no redistribution license, unless a later
  architect directive amends this contract.
- No copied code or assets may enter the repository without provenance and
  explicit approval.

## Where these values are enforced

| Value | Enforcing file |
|---|---|
| Gradle version | `gradle/wrapper/gradle-wrapper.properties` |
| AGP, Kotlin, Compose BOM, AndroidX pins | `gradle/libs.versions.toml` |
| applicationId, namespace, SDK levels, versionCode/Name, variants | `android-host/build.gradle.kts` |
| JDK toolchain | `jvmToolchain(17)` in every module build script |
| Contract conformance check | `tools/verify_project_identity.py` |

## Implementation notes recorded at freeze time

- AGP 9 supplies built-in Kotlin support and rejects the standalone
  `org.jetbrains.kotlin.android` plugin. `android-host` therefore applies
  `com.android.application` and `org.jetbrains.kotlin.plugin.compose` only. The
  frozen Kotlin `2.4.10` still governs the Android module: the resolved
  `kotlin-stdlib` and `kotlin-gradle-plugin` on the Android compile classpath are
  `2.4.10`.
- The installed Android platform package for `compileSdk = 37` is
  `platforms;android-37.0` with `build-tools;37.0.0`.
- Compose BOM `2026.06.00` is used as frozen even though later BOMs exist. The
  pin is an architect decision, not a currency decision.
