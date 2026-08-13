# Project Learnings

Use this append-only file for durable, verified project knowledge only.

## Entry guidance after adoption

Each live entry should include:

- Learning ID.
- Date.
- Fact or lesson.
- Evidence location.
- Confidence: `VERIFIED` or `PROVISIONAL`.
- Scope.
- Supersedes or superseded-by reference when applicable.

Do not add live entries to this template. Exclude temporary narration, raw logs, full source files, secrets, unsupported guesses, and facts already obvious from stable project documentation.

## L-0001

- Learning ID: L-0001
- Date: 2026-08-12
- Fact or lesson: The repository validator constrains governance vocabulary. Directive headings must match the identifier form beginning with the letter D and a hyphen, and outcome states are restricted to COMPLETE, PARTIAL, BLOCKED, FAILED, CANCELLED and SUPERSEDED, so an externally requested state such as NONCONFORMING must be expressed as the nearest allowed state plus an explicit narrative in the outcome summary.
- Evidence location: scripts/validate_governance.py, DIRECTIVE_HEADING and OUTCOME_STATUSES definitions near the top of the file.
- Confidence: VERIFIED
- Scope: All future governance ledger entries in this repository.
- Supersedes learning: none

## L-0002

- Learning ID: L-0002
- Date: 2026-08-12
- Fact or lesson: The shipped validator self-test copies this repository and asserts that the copy satisfies the clean unadopted fixture state, so it raises an assertion error permanently once the repository is adopted. Adoption and that self-test cannot both succeed without changing the script, which is outside governance-baseline scope.
- Evidence location: scripts/test_validate_governance.py, the clean fixture assertion in main; observed by executing the script on 2026-08-12.
- Confidence: VERIFIED
- Scope: Repository validation strategy and any future directive that touches scripts/.
- Supersedes learning: none

## L-0003

- Learning ID: L-0003
- Date: 2026-08-12
- Fact or lesson: This repository is not under version control. No Git repository, remote, branch or commit identifier exists at the repository root, so change history for governance work is available only through the local append-only ledgers.
- Evidence location: git status --short and git rev-parse HEAD executed at the repository root on 2026-08-12, both reporting that this is not a Git repository.
- Confidence: VERIFIED
- Scope: All change-evidence reporting for this repository until version control is introduced.
- Supersedes learning: none

## L-0004

- Learning ID: L-0004
- Date: 2026-08-12
- Fact or lesson: A validator self-test must construct its states from an isolated pristine fixture rather than from the live project directory. Once the live governance files were legitimately adopted, any test that assumed they were unadopted became permanently wrong, and the correct repair was to isolate the fixture rather than to relax the validator.
- Evidence location: scripts/test_validate_governance.py, the template fixture helpers near the top of the file; scripts/fixtures/governance_template/.
- Confidence: VERIFIED
- Scope: Governance tooling and any future test that inspects repository state.
- Supersedes learning: L-0002

## L-0005

- Learning ID: L-0005
- Date: 2026-08-12
- Fact or lesson: This repository is now a local Git repository on branch main, with provenance beginning at baseline commit f82e1b2f7c138a7c4238f109b45a6562b8b18a21. No remote is configured, so commit identifiers are durable locally but are not yet replicated anywhere.
- Evidence location: git log and git rev-parse HEAD at the repository root on 2026-08-12; DEC-0004 in .agent/RECORD.md.
- Confidence: VERIFIED
- Scope: All change-evidence reporting for this repository.
- Supersedes learning: L-0003

## L-0006

- Learning ID: L-0006
- Date: 2026-08-12
- Fact or lesson: The self-test summary previously reported a hardcoded count of eighty-two adopted rejection cases while the suite actually held seventy-nine. Counts printed as evidence are now derived from the case lists so the reported figure cannot drift from what ran.
- Evidence location: scripts/test_validate_governance.py, the summary print statements at the end of main.
- Confidence: VERIFIED
- Scope: Any future evidence printed by repository tooling.
- Supersedes learning: none

## L-0007

- Learning ID: L-0007
- Date: 2026-08-12
- Fact or lesson: Android Gradle Plugin 9 supplies built-in Kotlin support and fails the build if the standalone org.jetbrains.kotlin.android plugin is applied. The Android module therefore applies only com.android.application and org.jetbrains.kotlin.plugin.compose, and the frozen Kotlin 2.4.10 still governs it because the Compose plugin forces that version onto the Android compile classpath.
- Evidence location: The build failure text from Gradle 9.5.0 naming the plugin rejection, and the resolved kotlin-stdlib 2.4.10 and kotlin-gradle-plugin 2.4.10 in the android-host debugCompileClasspath and buildEnvironment reports on 2026-08-12.
- Confidence: VERIFIED
- Scope: Any future change to the Android module build script or to the Kotlin version pin.
- Supersedes learning: none

## L-0008

- Learning ID: L-0008
- Date: 2026-08-12
- Fact or lesson: The governance self-test copies the whole repository once per rejection case. Once build outputs existed this became prohibitively slow, so the copy now ignores build, .gradle, .kotlin and local.properties in addition to the previous exclusions. None of those paths is tracked or validated, so no coverage was lost.
- Evidence location: scripts/test_validate_governance.py copy_repo, and the suite completing in about seventeen seconds after the change on 2026-08-12.
- Confidence: VERIFIED
- Scope: Any future addition of large untracked directories to the repository root.
- Supersedes learning: none

## L-0009

- Learning ID: L-0009
- Date: 2026-08-12
- Fact or lesson: The build host had no Java runtime, no Gradle and no Android SDK before D-004. The toolchain was installed without administrator rights into the user home: Temurin JDK 17.0.20+8 and Gradle 9.5.0 under ~/.local/toolchains, and the Android SDK with platforms android-37.0 and build-tools 37.0.0 under ~/Android/Sdk. Builds require JAVA_HOME and ANDROID_HOME to point at those locations.
- Evidence location: java -version and gradle --version reporting command not found before installation, and the successful gradle build afterwards, on 2026-08-12.
- Confidence: VERIFIED
- Scope: Any later session that needs to build this project on this machine, and any assumption that a toolchain is present.
- Supersedes learning: none

## L-0010

- Learning ID: L-0010
- Date: 2026-08-12
- Fact or lesson: The android-37.0 google_apis x86_64 emulator image crashes surfaceflinger with SIGSEGV inside RegionSamplingThread::threadMain on this host, which takes down system_server and produces broken-pipe failures from the activity service. The fault reproduced under the swiftshader_indirect, guest and swangle_indirect rendering backends. No fatal exception was ever attributed to com.animusmachinae.dll17 during those runs.
- Evidence location: qualification/device-matrix/R000/DEVICE_MATRIX.md, and the emulator crash buffer captured on 2026-08-12.
- Confidence: VERIFIED
- Scope: R001 determinism qualification, which requires the x86 Android emulator as a cross-architecture target in the canonical determinism matrix. It must be resolved before R001 closes.
- Supersedes learning: none

## L-0011

- Learning ID: L-0011
- Date: 2026-08-12
- Fact or lesson: A crash check that greps the whole logcat buffer for FATAL EXCEPTION attributes unrelated system crashes to the application under test. The first qualification run reported eight failures that were caused by a uiautomator crash and by system_server instability, not by the shell. Crash detection must be scoped to the package under test by matching the Process line inside the crash block.
- Evidence location: tools/qualify_r000_android.sh, crash_inspect function, and the first two recorded harness runs on 2026-08-12.
- Confidence: VERIFIED
- Scope: Any device qualification harness added in later phases.
- Supersedes learning: none

## L-0012

- Learning ID: L-0012
- Date: 2026-08-12
- Fact or lesson: The debug APK is byte-identical across a clean rebuild on this toolchain. The same SHA-256 was produced for the artifact installed during device qualification and for the artifact rebuilt after gradlew clean build.
- Evidence location: qualification/evidence/R000/toolchain_environment.txt and qualification/device-matrix/R000/qualification_run.log, both recording 8bc93994407648e72211da89c002421c03a4e9503ced49966c9e869e9f7c7784.
- Confidence: VERIFIED
- Scope: The release gate requirement that a release APK match the tested artifact hash. R000 evidence suggests that requirement is achievable on this toolchain, but it was observed for debug builds only.
- Supersedes learning: none

## L-0013

- Learning ID: L-0013
- Date: 2026-08-12
- Fact or lesson: A target can report sys.boot_completed as 1 while system_server is still settling, and PackageManager can take time after a streamed install before the launcher activity resolves. Both produce failures that look like application defects: broken pipe from the activity service, and Activity class does not exist. Device harnesses must wait for the activity and package services and for launcher-activity resolution before recording any result.
- Evidence location: tools/qualify_r000_android.sh, the wait_for_services function and the launcher resolution loop, added on 2026-08-12.
- Confidence: VERIFIED
- Scope: Any later automated device qualification.
- Supersedes learning: none


## L-0014

- Learning ID: L-0014
- Date: 2026-08-13
- Fact or lesson: When an Android emulator image is unusable because of its graphics stack, an AOSP ATD image is the first alternative to try rather than the last. The android-37.0 google_apis x86_64 image crashed surfaceflinger under all three rendering backends, while system-images android-36 aosp_atd x86_64 booted headless in fifty-seven seconds on the same host with zero fatal exceptions. ATD images strip SystemUI and much of the graphics path because they are built for automated testing, so a workload that needs ART but not a rendered surface loses nothing by moving to one.
- Evidence location: qualification/device-matrix/R001/x86_emulator.txt; contrast with the blocked-target section of qualification/device-matrix/R000/DEVICE_MATRIX.md.
- Confidence: VERIFIED
- Scope: Any Android qualification on this build host that needs execution rather than presentation.
- Supersedes learning: none

## L-0015

- Learning ID: L-0015
- Date: 2026-08-13
- Fact or lesson: java.lang.Math.multiplyHigh is the obvious way to obtain a 128-bit intermediate and compiles cleanly against compileSdk 37, but it reached Android only at API 31 while the frozen minSdk is 29. The failure mode is a NoSuchMethodError on a supported device that no desktop test can reach, so a fully green host test suite proves nothing about it. Determinism-critical arithmetic uses explicit 32-bit limbs instead, and the prohibition is written into the contract rather than left to reviewer memory.
- Evidence location: docs/architecture/DeterminismContractV1.md section 9.3; core-math FixedPoint.multiplyThenDivideRounded; the structural assertion in FixedPointOracleTest that FixedPoint does not reference java.lang.Math.
- Confidence: VERIFIED
- Scope: Any JDK method used in shared core code whose introducing API level is above minSdk.
- Supersedes learning: none

## L-0016

- Learning ID: L-0016
- Date: 2026-08-13
- Fact or lesson: Where a primitive is owned rather than taken from the platform, a second independent implementation is worth more than another test against the first. The lookup-table generator computes the embedded digest in Python with hashlib and an independently written canonical encoder, and the Kotlin runtime recomputes it with the project's own SHA-256 and codec. Making the build's own code-generation step the second implementation turns a routine generator into a continuous cross-implementation check of both the codec and the digest at no extra maintenance cost.
- Evidence location: tools/generate_lookup_tables.py; core-math LookupTable.verify; the CI step that runs the generator with --check.
- Confidence: VERIFIED
- Scope: Any owned primitive in this repository whose correctness cannot be delegated to a platform library.
- Supersedes learning: none

## L-0017

- Learning ID: L-0017
- Date: 2026-08-13
- Fact or lesson: A qualification bundle is a claim about a past commit and must be verified against that commit, not against the working tree. The R000 bundle originally hashed the working tree, so R001's legitimate edits to shared build files and registries would have failed a gate that had already closed, for reasons unrelated to R000. Reading blobs through git show makes a closed gate immune to later phases and costs nothing, while working-tree verification quietly turns every shared file into a tripwire that later work is guaranteed to hit.
- Evidence location: tools/build_qualification_bundle.py, the PhaseSpec frozen_at_commit mechanism; DEC-0016.
- Confidence: VERIFIED
- Scope: Every future phase gate in this repository.
- Supersedes learning: none

## L-0018

- Learning ID: L-0018
- Date: 2026-08-13
- Fact or lesson: Filtering device evidence to the package or logging tag under test is a correctness control as well as a privacy control. An unfiltered logcat buffer mixes unrelated system and third-party failures into the evidence and invites the same false attribution that made the first R000 harness run report eight failures the shell did not have, while also inventorying the device owner's applications and account activity into a public repository.
- Evidence location: tools/qualify_r001_determinism.sh, the DLL17-R001 tag filter; qualification/device-matrix/R001/, which contains zero matches for account, token or telephony patterns while carrying the complete kernel output.
- Confidence: VERIFIED
- Scope: Every harness in this repository that reads device logs into committed evidence.
- Supersedes learning: none
