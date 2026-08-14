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

## L-0019

- Learning ID: L-0019
- Date: 2026-08-13
- Fact or lesson: Making reconciliation plan a canonical event sequence, rather than mutate state directly, is what makes it both replayable and resumable. Because the reconciliation is a list of events folded by the ordinary reducer, reduce equals replay holds for it automatically, and an interruption at any chunk resumes from a cursor to the identical sequence. A reconciliation that wrote state directly would leave nothing in the journal to replay and would be unrecoverable if the process died in the middle of one.
- Evidence location: core-continuity Reconciliation and ContinuityReducer; FX-SLICED-RESUME-01 and FX-REPLAY-EQUIVALENCE-01; slice equivalence asserted at one, two, seven, sixty-four and ten thousand chunks.
- Confidence: VERIFIED
- Scope: Every future canonical computation that must survive being interrupted.
- Supersedes learning: none

## L-0020

- Learning ID: L-0020
- Date: 2026-08-13
- Fact or lesson: Blind time must not advance the counter that gates its own replenishment. Blind decay credit is earned from verified elapsed time, so if consuming credit also advanced the verified counter, a reboot loop could manufacture the verified time needed to buy more credit. Separating chronological age from verified time is what closes that loop, and the same separation is why an anomalous or unverifiable interval can advance the clock a user sees without granting anything a user could farm.
- Evidence location: core-continuity ContinuityReducer BLIND_CREDIT_CONSUMED handler; FX-REBOOT-01 and FX-REPEATED-REBOOT-01; IMPL-0022.
- Confidence: VERIFIED
- Scope: Every future mechanism where a resource is earned from one clock and spent against another.
- Supersedes learning: none

## L-0021

- Learning ID: L-0021
- Date: 2026-08-13
- Fact or lesson: A structurally incomplete durable record and a structurally complete record that fails authentication mean opposite things and must be handled differently. The first is a write that never finished and is correctly skipped at the tail; the second is corruption, relocation or foreign data and must always be a storage fault. Treating them the same either makes an interrupted write unrecoverable or lets an acknowledged record be silently dropped, and the first implementation here did the latter.
- Evidence location: core-continuity EncryptedRecordStore readAll; the four relocation refusal tests in DurabilityAndJournalTest; IMPL-0026.
- Confidence: VERIFIED
- Scope: Every future durable reader in this repository.
- Supersedes learning: none

## L-0022

- Learning ID: L-0022
- Date: 2026-08-13
- Fact or lesson: Forgiving uncapped debt at accrual rather than retaining it is what makes the outstanding balance honest. A retained overflow is a liability the user cannot see and cannot discharge, so a one year absence would leave a permanent invisible burden even though only seventy two hours of it could ever be collected. Forgiving at accrual makes the outstanding number always equal to what could actually be collected, which is also what makes it safe to show a user.
- Evidence location: core-continuity Reconciliation prologue; FX-DEBT-CAP-01, which forgives 31,276,200,000 milliseconds of a one year gap at accrual; IMPL-0021.
- Confidence: VERIFIED
- Scope: Every future bounded liability in this system.
- Supersedes learning: none

## L-0023

- Learning ID: L-0023
- Date: 2026-08-13
- Fact or lesson: An accumulating learning law cannot represent reliability. If preference or habit is updated by adding a multiple of the outcome valence, every object with a net positive outcome eventually pins at the bound, so a source that succeeds eighty-six per cent of the time and one that succeeds thirty-three per cent of the time become indistinguishable, and no contingency reversal can be observed. An error-corrected update that moves the estimate toward the observed outcome discriminates correctly and cannot saturate.
- Evidence location: research/aliveness-spike/cohorts/Mechanisms.kt; AX-PREFERENCE-01 and AX-HABIT-01 before and after; IMPL-0031; DEC-0021.
- Confidence: VERIFIED
- Scope: Every future learned-value mechanism, including the R006 production equivalents if they are ever authorized.
- Supersedes learning: none

## L-0024

- Learning ID: L-0024
- Date: 2026-08-13
- Fact or lesson: An organism cannot learn to avoid something it innately avoids. The first habitat implementation depressed the utility of anything labelled unsafe and promoted withdrawal from it to the top priority tier, which meant the aversive object was never engaged, no punishment ever occurred, and the conditioned avoidance and extinction arms were both untestable. Danger has to be discoverable for danger learning to be measurable.
- Evidence location: research/aliveness-spike/cohorts/Controller.kt; AX-AVOIDANCE-01 peak fear 0.447 with 1,632 avoidance ticks; IMPL-0032.
- Confidence: VERIFIED
- Scope: Any future fixture that intends to measure acquisition of an aversive association.
- Supersedes learning: none

## L-0025

- Learning ID: L-0025
- Date: 2026-08-13
- Fact or lesson: An observational fixture measures the organism's choices, not the mechanism. Several A000 fixtures initially compared learned values for objects the organism had never engaged, or measured a habituation trace after the organism had stopped visiting the object and the trace had decayed. The readouts were real and the conclusions were meaningless. A mechanism claim needs a controlled protocol: forced exposure, or direct exercise of the update law, with the behavioural consequence measured as a separate finding.
- Evidence location: A000QualificationKernel preference, habituation and habit sections; the corrected fixtures against their originals in qualification/fixtures/A000/A000_REPORT.txt.
- Confidence: VERIFIED
- Scope: Every behavioural fixture in this programme.
- Supersedes learning: none

## L-0026

- Learning ID: L-0026
- Date: 2026-08-13
- Fact or lesson: Most spontaneous actions in the candidate architecture are overdetermined: several mechanisms independently support the same choice, so the dominant attribution class is MIXED_SUBSTANTIVE. A one-at-a-time knockout would have classified those actions as unexplained, which is precisely the misclassification exact coalition attribution was adopted to prevent. The canonical decision to move from single knockouts to Shapley allocation was load-bearing rather than ceremonial.
- Evidence location: research/aliveness-spike/cohorts/Attribution.kt; AX-ATTRIBUTION-01 over 1,363 scored spontaneous actions; the class breakdown in qualification/fixtures/A000/A000_REPORT.txt.
- Confidence: VERIFIED
- Scope: Any future causal attribution over this organism's action selection.
- Supersedes learning: none

## L-0027

- Learning ID: L-0027
- Date: 2026-08-13
- Fact or lesson: A tier promotion that fires on a schedule rather than on a need silently owns the whole schedule. Promoting rest to Tier 3 for the entire night, regardless of whether the organism was tired, meant Tier 3 outranked Tier 4 every night; one action took nearly half the tick budget and the successor of almost any action was that action. A circadian preference belongs in the utility term where it competes, not in the tier where it wins by construction.
- Evidence location: research/aliveness-spike/cohorts/Controller.kt tierForRest; occupancy 0.468 to 0.357 and cycle regularity 0.924 to 0.369; IMPL-0037.
- Confidence: VERIFIED
- Scope: Every future use of the canonical tier model, including the R004 production controller.
- Supersedes learning: none

## L-0028

- Learning ID: L-0028
- Date: 2026-08-13
- Fact or lesson: One suppression term cannot prevent behavioural lock-in, because under continuous engagement it saturates and a uniformly saturated term stops discriminating. Three small bounds acting on different things worked where one large one did not: satiation on the kind of activity, a refractory on the object, and a metabolic cost on the body. Each is individually gentle, and the organism still cannot spend its life on one thing.
- Evidence location: research/aliveness-spike/cohorts/Mechanisms.kt and Controller.kt; single-action occupancy from 0.593 through 0.468 to 0.357 across the three additions; IMPL-0041.
- Confidence: VERIFIED
- Scope: Any future anti-convergence or attention-allocation mechanism.
- Supersedes learning: none

## L-0029

- Learning ID: L-0029
- Date: 2026-08-13
- Fact or lesson: Directed exploration must be damped by fear or it becomes a mechanism for walking into known harm. Uncertainty about an avoided object keeps growing precisely because it is avoided, which made the aversive object the most information-rich thing in the habitat and drew the organism back into it dozens of times a day. Avoidance also has to cover attending, not only approaching.
- Evidence location: research/aliveness-spike/cohorts/Controller.kt habitExpectancy and eligible; threat count per thirty virtual days from 2,610 to 14; IMPL-0039.
- Confidence: VERIFIED
- Scope: Any future curiosity or information-seeking term that coexists with a threat model.
- Supersedes learning: none

## L-0030

- Learning ID: L-0030
- Date: 2026-08-13
- Fact or lesson: A single-organism comparison cannot tell a mechanism's contribution from a coin flip. Two of the D008 findings rested on one pair of organisms; pooled over a proper seed matrix, one reversed sign and the other turned out to be a tie. Specialization makes this worse rather than better: a healthy candidate produces individuals that ignore whole parts of the habitat, so one individual's omission says nothing about the learning law.
- Evidence location: A000QualificationKernel preference and episodic sections; AX-PREFERENCE-01 and AX-EPISODIC-02 before and after pooling; DEC-0024.
- Confidence: VERIFIED
- Scope: Every mechanism-contribution claim in this programme.
- Supersedes learning: none

## L-0031

- Learning ID: L-0031
- Date: 2026-08-13
- Fact or lesson: An information barrier that is described in a document is not a barrier. The variance-pilot seal became real only when the released type was reduced to two fields, the full analysis type was made private and unreturnable, and a test showed that two pilots with opposite outcomes release byte-identical output. The last of those is the only one that proves the property rather than the implementation.
- Evidence location: research/aliveness-spike/analysis/BlindVariancePilot.kt; BlindVariancePilotSealTest; IMPL-0047; INV-0043.
- Confidence: VERIFIED
- Scope: Every place in this programme where one party must not learn what another party knows.
- Supersedes learning: none

## L-0032

- Learning ID: L-0032
- Date: 2026-08-13
- Fact or lesson: Two constants frozen independently by the architect turned out to determine each other. The 1.25 pilot standard-deviation inflation factor is exactly the one-sided ninety-five percent upper confidence bound on a standard deviation at thirty-five degrees of freedom, which is thirty-six pairs. At thirty-five pairs the bound is 1.253 and the factor no longer covers it. That is why a short pilot now reports itself protocol-invalid rather than releasing a number the inflation factor cannot defend, and why the check is computed in a test rather than asserted from a table.
- Evidence location: BlindVariancePilotSealTest; research/aliveness-spike/study-protocol/A001FeasibilityBudgetV1.md.
- Confidence: VERIFIED
- Scope: The A001 feasibility calculation and any later re-powering.
- Supersedes learning: none

## L-0033

- Learning ID: L-0033
- Date: 2026-08-13
- Fact or lesson: A number quoted in prose and produced by an ad-hoc diagnostic will drift from the artifact it claims to describe. Several D009 figures came from an intermediate kernel run, and the cohort comparison table came from a diagnostic that nothing checked. The fix was structural in both cases: reconcile the prose against the committed evidence, and turn the diagnostic into a named generator whose output is a bundle constituent.
- Evidence location: DEC-0027; research/aliveness-spike/evidence/BASELINE_COVERAGE_MANIFEST.txt; qualification/longitudinal/A000/ACCELERATED_FINDINGS.md.
- Confidence: VERIFIED
- Scope: Every figure this programme reports to the architect.
- Supersedes learning: none

## L-0034

- Learning ID: L-0034
- Date: 2026-08-14
- Fact or lesson: A durability benchmark that lands on tmpfs measures nothing and looks excellent. The first backend evaluation ran in the system temporary directory, which is tmpfs on this machine, reported two-microsecond commits for an fsync-per-commit design, and would have selected a backend on numbers that never touched a device. Both harnesses now print the filesystem type and refuse to run on tmpfs or ramfs.
- Evidence location: benchmarks/persistence-bench/; qualification/evidence/R012/backend_benchmark.txt; IMPL-0064.
- Confidence: VERIFIED
- Scope: Every storage or durability measurement this programme makes.
- Supersedes learning: none

## L-0035

- Learning ID: L-0035
- Date: 2026-08-14
- Fact or lesson: Killing a process does not prove a durability claim. Runtime.halt skips shutdown hooks and finalizers and is a genuine abrupt death, but it leaves the operating system page cache intact, so bytes written without an fsync are still readable afterwards. Only power loss, or a device that lies about fsync, distinguishes forced bytes from unforced ones. The fault matrix therefore proves the recovery logic around the policy, and the policy itself is stated as the basis of the claim rather than implied to be tested.
- Evidence location: core-persistence/CrashHarness.kt; governance/release-gates/R012_SUBSTRATE_GATE.md; IMPL-0065.
- Confidence: VERIFIED
- Scope: Every durability claim in this programme.
- Supersedes learning: none

## L-0036

- Learning ID: L-0036
- Date: 2026-08-14
- Fact or lesson: The most dangerous storage bug found under D011 was a corrupt first frame being reported as an empty journal. The original scan tolerated a malformed frame whenever it sat at the end of the last good data, which is trivially true for the first frame of a file. A corrupt installation would have presented itself as a device with no organism on it, and the next step would have been a birth. The fix is a rule rather than a patch: a wrong magic or an impossible length is corruption wherever it appears, because a partial write cannot produce either.
- Evidence location: core-persistence/PersistenceBackend.kt; PersistenceBackendTest; IMPL-0055; INV-0048.
- Confidence: VERIFIED
- Scope: Every recovery path that has to distinguish an absent organism from an unreadable one.
- Supersedes learning: none

