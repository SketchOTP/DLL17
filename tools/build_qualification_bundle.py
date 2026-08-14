#!/usr/bin/env python3
"""Builds and verifies the phase qualification evidence bundles.

A bundle binds a phase's qualified artifacts by SHA-256 so that a later reader
can determine exactly what was qualified, and so that a silent edit to a
constituent fails CI instead of passing unnoticed.

Two kinds of phase exist here:

* **Frozen.** The phase's gate is closed. Its constituents are read from the
  commit that was qualified, never from the working tree. This matters: R000's
  bundle originally verified against the working tree, which meant any later
  phase editing a build file or a registry would break a gate that had already
  passed. Evidence of a past qualification must not depend on the present.

* **Live.** The phase currently being qualified. Its constituents are read from
  the working tree, because that is what is being qualified.

Usage:
    python3 tools/build_qualification_bundle.py --phase R001   # write R001
    python3 tools/build_qualification_bundle.py --verify       # verify every phase
"""

from __future__ import annotations

import argparse
import hashlib
import pathlib
import re
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
CI_WORKFLOW = ".github/workflows/ci.yml"

BUILD_FILES = [
    "gradle/libs.versions.toml",
    "gradle/wrapper/gradle-wrapper.properties",
    "settings.gradle.kts",
    "build.gradle.kts",
    "gradle.properties",
    "core-math/build.gradle.kts",
    "core-crypto/build.gradle.kts",
    "core-state/build.gradle.kts",
    "desktop-runner/build.gradle.kts",
    "android-host/build.gradle.kts",
]

REGISTRIES = [
    "docs/architecture/registries/CanonicalStateCatalog.md",
    "docs/architecture/registries/EventCatalog.md",
    "docs/architecture/registries/ParameterRegistry.md",
    "docs/architecture/registries/RandomDomainRegistry.md",
    "docs/architecture/registries/StateTransitionMatrix.md",
    "docs/architecture/registries/ThreadAndDispatcherMap.md",
    "docs/architecture/registries/PresentationContractCatalog.md",
]

R000_CONSTITUENTS: list[tuple[str, list[str]]] = [
    ("Project identity contract", ["docs/architecture/ProjectIdentityBuildContractV1.md"]),
    ("Dependency and toolchain lock state", BUILD_FILES),
    (
        "Source and licence state",
        [
            "LICENSE",
            "governance/source-provenance/DEPENDENCY_LICENSE_INVENTORY.md",
            "governance/source-provenance/SOURCE_PROVENANCE_LEDGER.md",
        ],
    ),
    ("Mandatory R000 registry state", REGISTRIES),
    (
        "Validation evidence",
        [
            "qualification/evidence/R000/governance_validation.txt",
            "qualification/evidence/R000/governance_selftest.txt",
            "qualification/evidence/R000/project_identity.txt",
            "qualification/evidence/R000/gradle_build.txt",
            "qualification/evidence/R000/desktop_runner.txt",
            "qualification/evidence/R000/android_assemble.txt",
            "qualification/evidence/R000/toolchain_environment.txt",
        ],
    ),
    (
        "Android install, launch and relaunch evidence",
        [
            "qualification/device-matrix/R000/qualification_run.log",
            "qualification/device-matrix/R000/ui_hierarchy_launch1.xml",
            "qualification/device-matrix/R000/ui_hierarchy_launch2.xml",
            "qualification/device-matrix/R000/shell_launch1.png",
            "qualification/device-matrix/R000/shell_launch2.png",
            "qualification/device-matrix/R000/meminfo_launch1.txt",
        ],
    ),
    (
        "Measured resource baseline and device matrix",
        [
            "docs/release/DEVICE_AND_RESOURCE_BUDGETS.md",
            "qualification/device-matrix/R000/DEVICE_MATRIX.md",
        ],
    ),
    ("Gate record", ["governance/release-gates/R000_EXIT_GATE.md"]),
]

CANONICAL_SOURCES = [
    "core-crypto/src/main/kotlin/com/animusmachinae/dll17/core/crypto/CanonicalEncoding.kt",
    "core-crypto/src/main/kotlin/com/animusmachinae/dll17/core/crypto/CanonicalHash.kt",
    "core-crypto/src/main/kotlin/com/animusmachinae/dll17/core/crypto/Sha256.kt",
    "core-crypto/src/main/kotlin/com/animusmachinae/dll17/core/crypto/DeterministicRandom.kt",
    "core-math/src/main/kotlin/com/animusmachinae/dll17/core/math/FixedPoint.kt",
    "core-math/src/main/kotlin/com/animusmachinae/dll17/core/math/SaturationDiagnostics.kt",
    "core-math/src/main/kotlin/com/animusmachinae/dll17/core/math/LookupTable.kt",
    "core-math/src/main/kotlin/com/animusmachinae/dll17/core/math/GeneratedLookupTables.kt",
    "core-state/src/main/kotlin/com/animusmachinae/dll17/core/state/CanonicalState.kt",
    "core-state/src/main/kotlin/com/animusmachinae/dll17/core/state/CanonicalReducer.kt",
    "core-state/src/main/kotlin/com/animusmachinae/dll17/core/state/Durability.kt",
    "core-state/src/main/kotlin/com/animusmachinae/dll17/core/state/Journal.kt",
    "core-state/src/main/kotlin/com/animusmachinae/dll17/core/state/WitnessedInteraction.kt",
    "core-state/src/main/kotlin/com/animusmachinae/dll17/core/state/PlatformPanicWitness.kt",
    "core-state/src/main/kotlin/com/animusmachinae/dll17/core/state/PresentationPayload.kt",
    "core-state/src/main/kotlin/com/animusmachinae/dll17/core/state/Migration.kt",
    "core-state/src/main/kotlin/com/animusmachinae/dll17/core/state/R001QualificationKernel.kt",
]

R001_CONSTITUENTS: list[tuple[str, list[str]]] = [
    ("Determinism contract", ["docs/architecture/DeterminismContractV1.md"]),
    ("Project identity contract", ["docs/architecture/ProjectIdentityBuildContractV1.md"]),
    ("Dependency and toolchain lock state", BUILD_FILES),
    (
        "Source and licence state",
        [
            "LICENSE",
            "governance/source-provenance/DEPENDENCY_LICENSE_INVENTORY.md",
            "governance/source-provenance/SOURCE_PROVENANCE_LEDGER.md",
        ],
    ),
    ("Canonical implementation source", CANONICAL_SOURCES),
    ("Lookup table generator", ["tools/generate_lookup_tables.py"]),
    ("Mandatory registry state", REGISTRIES + ["docs/invariants/INVARIANT_REGISTRY.md"]),
    (
        "Golden vectors and replay fixtures",
        [
            "qualification/fixtures/R001/GOLDEN_VECTORS.md",
            "qualification/fixtures/R001/desktop_jvm_report.txt",
            "qualification/replay/R001/REPLAY_EVIDENCE.md",
        ],
    ),
    (
        "Validation evidence",
        [
            "qualification/evidence/R001/governance_validation.txt",
            "qualification/evidence/R001/governance_selftest.txt",
            "qualification/evidence/R001/project_identity.txt",
            "qualification/evidence/R001/gradle_build.txt",
            "qualification/evidence/R001/desktop_runner.txt",
            "qualification/evidence/R001/lookup_table_check.txt",
            "qualification/evidence/R001/toolchain_environment.txt",
        ],
    ),
    (
        "Cross-target determinism matrix",
        [
            "qualification/device-matrix/R001/DETERMINISM_MATRIX.md",
            "qualification/device-matrix/R001/desktop_jvm.txt",
            "qualification/device-matrix/R001/x86_emulator.txt",
            "qualification/device-matrix/R001/tensor_device.txt",
        ],
    ),
    ("Gate record", ["governance/release-gates/R001_EXIT_GATE.md"]),
]


CONTINUITY_SOURCES = [
    "core-crypto/src/main/kotlin/com/animusmachinae/dll17/core/crypto/ChaCha20Poly1305.kt",
    "core-continuity/src/main/kotlin/com/animusmachinae/dll17/core/continuity/ContinuityContract.kt",
    "core-continuity/src/main/kotlin/com/animusmachinae/dll17/core/continuity/ContinuityState.kt",
    "core-continuity/src/main/kotlin/com/animusmachinae/dll17/core/continuity/ContinuityEvent.kt",
    "core-continuity/src/main/kotlin/com/animusmachinae/dll17/core/continuity/ContinuityReducer.kt",
    "core-continuity/src/main/kotlin/com/animusmachinae/dll17/core/continuity/TrustedTime.kt",
    "core-continuity/src/main/kotlin/com/animusmachinae/dll17/core/continuity/OfflineReconciliation.kt",
    "core-continuity/src/main/kotlin/com/animusmachinae/dll17/core/continuity/DurableStore.kt",
    "core-continuity/src/main/kotlin/com/animusmachinae/dll17/core/continuity/ContinuityJournal.kt",
    "core-continuity/src/main/kotlin/com/animusmachinae/dll17/core/continuity/DurabilityAdmission.kt",
    "core-continuity/src/main/kotlin/com/animusmachinae/dll17/core/continuity/PlatformProtection.kt",
    "core-continuity/src/main/kotlin/com/animusmachinae/dll17/core/continuity/IdentityBinding.kt",
    "core-continuity/src/main/kotlin/com/animusmachinae/dll17/core/continuity/ContinuityMigration.kt",
    "core-continuity/src/main/kotlin/com/animusmachinae/dll17/core/continuity/R002QualificationKernel.kt",
    "core-continuity/src/main/kotlin/com/animusmachinae/dll17/core/continuity/CoreContinuityModule.kt",
]

R002_CONSTITUENTS: list[tuple[str, list[str]]] = [
    (
        "Frozen contracts",
        [
            "docs/architecture/ContinuityDurabilityContractV1.md",
            "docs/architecture/DeterminismContractV1.md",
            "docs/architecture/ProjectIdentityBuildContractV1.md",
        ],
    ),
    ("Dependency and toolchain lock state", BUILD_FILES + ["core-continuity/build.gradle.kts"]),
    (
        "Source and licence state",
        [
            "LICENSE",
            "governance/source-provenance/DEPENDENCY_LICENSE_INVENTORY.md",
            "governance/source-provenance/SOURCE_PROVENANCE_LEDGER.md",
        ],
    ),
    ("Continuity implementation source", CONTINUITY_SOURCES),
    (
        "Android backup and transfer exclusion",
        [
            "android-host/src/main/AndroidManifest.xml",
            "android-host/src/main/res/xml/backup_rules.xml",
            "android-host/src/main/res/xml/data_extraction_rules.xml",
        ],
    ),
    ("Mandatory registry state", REGISTRIES + ["docs/invariants/INVARIANT_REGISTRY.md"]),
    (
        "Golden vectors, replay and exploit evidence",
        [
            "qualification/fixtures/R002/GOLDEN_VECTORS.md",
            "qualification/fixtures/R002/desktop_jvm_report.txt",
            "qualification/replay/R002/REPLAY_EVIDENCE.md",
            "qualification/fault-injection/R002/EXPLOIT_MATRIX.md",
        ],
    ),
    (
        "Validation evidence",
        [
            "qualification/evidence/R002/governance_validation.txt",
            "qualification/evidence/R002/governance_selftest.txt",
            "qualification/evidence/R002/project_identity.txt",
            "qualification/evidence/R002/gradle_build.txt",
            "qualification/evidence/R002/desktop_runner.txt",
            "qualification/evidence/R002/toolchain_environment.txt",
        ],
    ),
    (
        "Cross-target continuity matrix",
        [
            "qualification/device-matrix/R002/CONTINUITY_MATRIX.md",
            "qualification/device-matrix/R002/desktop_jvm.txt",
            "qualification/device-matrix/R002/x86_emulator.txt",
            "qualification/device-matrix/R002/tensor_device.txt",
        ],
    ),
    ("Gate record", ["governance/release-gates/R002_EXIT_GATE.md"]),
]


SPIKE_SOURCES = [
    "research/aliveness-spike/cohorts/src/main/kotlin/com/animusmachinae/dll17/research/aliveness/SpikeContract.kt",
    "research/aliveness-spike/cohorts/src/main/kotlin/com/animusmachinae/dll17/research/aliveness/SpikeNumerics.kt",
    "research/aliveness-spike/cohorts/src/main/kotlin/com/animusmachinae/dll17/research/aliveness/Habitat.kt",
    "research/aliveness-spike/cohorts/src/main/kotlin/com/animusmachinae/dll17/research/aliveness/Organism.kt",
    "research/aliveness-spike/cohorts/src/main/kotlin/com/animusmachinae/dll17/research/aliveness/Mechanisms.kt",
    "research/aliveness-spike/cohorts/src/main/kotlin/com/animusmachinae/dll17/research/aliveness/Controller.kt",
    "research/aliveness-spike/cohorts/src/main/kotlin/com/animusmachinae/dll17/research/aliveness/Outcomes.kt",
    "research/aliveness-spike/cohorts/src/main/kotlin/com/animusmachinae/dll17/research/aliveness/Attribution.kt",
    "research/aliveness-spike/cohorts/src/main/kotlin/com/animusmachinae/dll17/research/aliveness/DecisionTrace.kt",
    "research/aliveness-spike/cohorts/src/main/kotlin/com/animusmachinae/dll17/research/aliveness/Expression.kt",
    "research/aliveness-spike/cohorts/src/main/kotlin/com/animusmachinae/dll17/research/aliveness/Agents.kt",
    "research/aliveness-spike/cohorts/src/main/kotlin/com/animusmachinae/dll17/research/aliveness/Runtime.kt",
    "research/aliveness-spike/accelerated-sim/src/main/kotlin/com/animusmachinae/dll17/research/aliveness/sim/Measures.kt",
    "research/aliveness-spike/accelerated-sim/src/main/kotlin/com/animusmachinae/dll17/research/aliveness/sim/AcceleratedSimulator.kt",
    "research/aliveness-spike/accelerated-sim/src/main/kotlin/com/animusmachinae/dll17/research/aliveness/sim/A000QualificationKernel.kt",
    "research/aliveness-spike/analysis/src/main/kotlin/com/animusmachinae/dll17/research/aliveness/analysis/CuriosityEnvelopeSearch.kt",
    "research/aliveness-spike/analysis/src/main/kotlin/com/animusmachinae/dll17/research/aliveness/analysis/AlivenessGovernanceAudit.kt",
    "research/aliveness-spike/realtime-viewer/src/main/kotlin/com/animusmachinae/dll17/research/aliveness/viewer/ViewerSession.kt",
    "research/aliveness-spike/realtime-viewer/src/main/kotlin/com/animusmachinae/dll17/research/aliveness/viewer/SwingViewer.kt",
]

SPIKE_CONTRACTS = [
    "research/aliveness-spike/study-protocol/README.md",
    "research/aliveness-spike/study-protocol/SpikeExpressionContractV1.md",
    "research/aliveness-spike/study-protocol/SpikeDecisionTraceV1.md",
    "research/aliveness-spike/study-protocol/MechanismCoalitionSetV2.md",
    "research/aliveness-spike/study-protocol/MechanismRevisionD009.md",
    "research/aliveness-spike/study-protocol/CoalitionValueFunctionV1.md",
    "research/aliveness-spike/study-protocol/SpontaneousActionAttributionV1.md",
    "research/aliveness-spike/study-protocol/CuriosityBalanceEnvelopeV1.md",
    "research/aliveness-spike/study-protocol/ScriptedPetBaselineV1.md",
    "research/aliveness-spike/study-protocol/MaterialChangeEligibilityV1.md",
    "research/aliveness-spike/study-protocol/AlivenessGovernanceAuditV1.md",
    "research/aliveness-spike/study-protocol/AlivenessProgramGateV1.md",
    "research/aliveness-spike/study-protocol/BaselineQualificationProtocolV1.md",
    "research/aliveness-spike/study-protocol/BlindVariancePilotV1.md",
    "research/aliveness-spike/study-protocol/A001FeasibilityBudgetV1.md",
    "research/aliveness-spike/study-protocol/IndependentReviewRosterV1.md",
]

A000_CONSTITUENTS: list[tuple[str, list[str]]] = [
    ("Research contracts", SPIKE_CONTRACTS),
    ("Research implementation", SPIKE_SOURCES),
    (
        "Research isolation boundary",
        [
            "research/aliveness-spike/cohorts/build.gradle.kts",
            "research/aliveness-spike/accelerated-sim/build.gradle.kts",
            "research/aliveness-spike/analysis/build.gradle.kts",
            "research/aliveness-spike/realtime-viewer/build.gradle.kts",
            "settings.gradle.kts",
            "research/aliveness-spike/cohorts/src/test/kotlin/com/animusmachinae/dll17/research/aliveness/SpikeIsolationTest.kt",
            CI_WORKFLOW,
        ],
    ),
    (
        "Accelerated evidence",
        [
            "qualification/fixtures/A000/GOLDEN_VECTORS.md",
            "qualification/fixtures/A000/A000_REPORT.txt",
            "qualification/longitudinal/A000/ACCELERATED_FINDINGS.md",
        ],
    ),
    (
        "Preserved negative evidence",
        [
            "research/aliveness-spike/evidence/negative/D008/NEGATIVE_EVIDENCE.md",
            "research/aliveness-spike/evidence/negative/D008/A000_REPORT_D008.txt",
            "research/aliveness-spike/evidence/negative/D008/CURIOSITY_ENVELOPE_SEARCH_D008.txt",
        ],
    ),
    (
        "Envelope and governance evidence",
        [
            "research/aliveness-spike/evidence/CURIOSITY_ENVELOPE_SEARCH.txt",
            "research/aliveness-spike/evidence/GOVERNANCE_AUDIT.txt",
        ],
    ),
    (
        "Validation evidence",
        [
            "qualification/evidence/A000/governance_validation.txt",
            "qualification/evidence/A000/project_identity.txt",
            "qualification/evidence/A000/gradle_build.txt",
            "qualification/evidence/A000/a000_kernel.txt",
            "qualification/evidence/A000/toolchain_environment.txt",
        ],
    ),
    ("Gate record", ["governance/release-gates/A000_EXIT_GATE.md"]),
]


A001PRE_CONSTITUENTS: list[tuple[str, list[str]]] = [
    (
        "Study protocol and instrument",
        [
            "research/aliveness-spike/study-protocol/AlivenessStudyProtocolV1.md",
            "research/aliveness-spike/study-protocol/GradedAlivenessInstrumentV1.md",
            "research/aliveness-spike/study-protocol/AlivenessProgramGateV1.md",
        ],
    ),
    (
        "Comparator qualification",
        [
            "research/aliveness-spike/study-protocol/BaselineQualificationProtocolV1.md",
            "research/aliveness-spike/study-protocol/ScriptedPetBaselineV1.md",
            "research/aliveness-spike/evidence/BASELINE_COVERAGE_MANIFEST.txt",
            "research/aliveness-spike/accelerated-sim/src/main/kotlin/com/animusmachinae/dll17/research/aliveness/sim/BaselineCoverageManifest.kt",
        ],
    ),
    (
        "Sealed variance pilot and feasibility",
        [
            "research/aliveness-spike/study-protocol/BlindVariancePilotV1.md",
            "research/aliveness-spike/study-protocol/A001FeasibilityBudgetV1.md",
            "research/aliveness-spike/analysis/src/main/kotlin/com/animusmachinae/dll17/research/aliveness/analysis/BlindVariancePilot.kt",
            "research/aliveness-spike/analysis/src/main/kotlin/com/animusmachinae/dll17/research/aliveness/analysis/A001FeasibilityBudget.kt",
        ],
    ),
    (
        "Preregistered analysis",
        [
            "research/aliveness-spike/analysis/src/main/kotlin/com/animusmachinae/dll17/research/aliveness/analysis/A001StudyContract.kt",
            "research/aliveness-spike/analysis/src/main/kotlin/com/animusmachinae/dll17/research/aliveness/analysis/A001Analysis.kt",
            "research/aliveness-spike/analysis/src/main/kotlin/com/animusmachinae/dll17/research/aliveness/analysis/Statistics.kt",
            "research/aliveness-spike/analysis/src/main/kotlin/com/animusmachinae/dll17/research/aliveness/analysis/A001DryRun.kt",
            "research/aliveness-spike/analysis/src/test/kotlin/com/animusmachinae/dll17/research/aliveness/analysis/StatisticsTest.kt",
            "research/aliveness-spike/analysis/src/test/kotlin/com/animusmachinae/dll17/research/aliveness/analysis/A001AnalysisTest.kt",
            "research/aliveness-spike/analysis/src/test/kotlin/com/animusmachinae/dll17/research/aliveness/analysis/A001StudyContractTest.kt",
            "research/aliveness-spike/analysis/src/test/kotlin/com/animusmachinae/dll17/research/aliveness/analysis/A001FeasibilityBudgetTest.kt",
            "research/aliveness-spike/analysis/src/test/kotlin/com/animusmachinae/dll17/research/aliveness/analysis/BlindVariancePilotSealTest.kt",
        ],
    ),
    (
        "Participants and independent review",
        [
            "research/aliveness-spike/study-protocol/ParticipantInformationAndConsentV1.md",
            "research/aliveness-spike/study-protocol/DataHandlingAndPrivacyV1.md",
            "research/aliveness-spike/study-protocol/IndependentReviewOnboardingV1.md",
            "research/aliveness-spike/study-protocol/IndependentReviewRosterV1.md",
            "research/aliveness-spike/study-protocol/MaterialChangeEligibilityV1.md",
        ],
    ),
    (
        "Activation audit",
        [
            "research/aliveness-spike/study-protocol/AlivenessGovernanceAuditV1.md",
            "research/aliveness-spike/analysis/src/main/kotlin/com/animusmachinae/dll17/research/aliveness/analysis/AlivenessGovernanceAudit.kt",
            "research/aliveness-spike/analysis/src/test/kotlin/com/animusmachinae/dll17/research/aliveness/analysis/GovernanceAuditTest.kt",
            "research/aliveness-spike/evidence/GOVERNANCE_AUDIT.txt",
        ],
    ),
    (
        "Synthetic dry-run evidence",
        ["research/aliveness-spike/evidence/A001_ACTIVATION_DRY_RUN.txt"],
    ),
    (
        "Validation evidence",
        [
            "qualification/evidence/A001PRE/governance_validation.txt",
            "qualification/evidence/A001PRE/project_identity.txt",
            "qualification/evidence/A001PRE/gradle_build.txt",
            "qualification/evidence/A001PRE/a000_kernel.txt",
            "qualification/evidence/A001PRE/toolchain_environment.txt",
        ],
    ),
    (
        "Isolation and CI",
        [
            "settings.gradle.kts",
            "research/aliveness-spike/analysis/build.gradle.kts",
            "research/aliveness-spike/accelerated-sim/build.gradle.kts",
            "research/aliveness-spike/cohorts/src/test/kotlin/com/animusmachinae/dll17/research/aliveness/SpikeIsolationTest.kt",
            CI_WORKFLOW,
        ],
    ),
    (
        "Reconciled A000 carry-forward",
        [
            "research/aliveness-spike/cohorts/src/main/kotlin/com/animusmachinae/dll17/research/aliveness/DecisionTrace.kt",
            "research/aliveness-spike/accelerated-sim/src/main/kotlin/com/animusmachinae/dll17/research/aliveness/sim/A000QualificationKernel.kt",
            "qualification/fixtures/A000/GOLDEN_VECTORS.md",
            "qualification/fixtures/A000/A000_REPORT.txt",
            "qualification/longitudinal/A000/ACCELERATED_FINDINGS.md",
            "governance/release-gates/A000_EXIT_GATE.md",
        ],
    ),
    ("Gate record", ["governance/release-gates/A001_ACTIVATION_GATE.md"]),
]


R012_CONSTITUENTS: list[tuple[str, list[str]]] = [
    (
        "Frozen contracts",
        [
            "docs/architecture/PersistenceBackendContractV1.md",
            "docs/architecture/LocalStorageCryptographyContractV1.md",
            "docs/architecture/RecoveryCryptographyContractV1.md",
            "docs/architecture/IdentityAuthorityProtocolV1.md",
            "docs/architecture/RecoveryPackageStoreContractV1.md",
        ],
    ),
    (
        "Persistence implementation",
        [
            "core-persistence/build.gradle.kts",
            "core-persistence/src/main/kotlin/com/animusmachinae/dll17/core/persistence/PersistenceBackend.kt",
            "core-persistence/src/main/kotlin/com/animusmachinae/dll17/core/persistence/LocalStorageCryptography.kt",
            "core-persistence/src/main/kotlin/com/animusmachinae/dll17/core/persistence/ColdRecoveryActivation.kt",
            "core-persistence/src/main/kotlin/com/animusmachinae/dll17/core/persistence/CrashHarness.kt",
            "core-persistence/src/main/kotlin/com/animusmachinae/dll17/core/persistence/R012QualificationKernel.kt",
            "core-persistence/src/main/kotlin/com/animusmachinae/dll17/core/persistence/R012PerformanceHarness.kt",
        ],
    ),
    (
        "Recovery implementation",
        [
            "core-recovery/build.gradle.kts",
            "core-recovery/src/main/kotlin/com/animusmachinae/dll17/core/recovery/RecoveryCryptography.kt",
            "core-recovery/src/main/kotlin/com/animusmachinae/dll17/core/recovery/RecoveryPackageStore.kt",
            "core-recovery/src/main/kotlin/com/animusmachinae/dll17/core/recovery/IdentityAuthorityProtocol.kt",
            "core-crypto/src/main/kotlin/com/animusmachinae/dll17/core/crypto/KeyDerivation.kt",
        ],
    ),
    (
        "Identity authority service",
        [
            "services/identity-authority/build.gradle.kts",
            "services/identity-authority/src/main/kotlin/com/animusmachinae/dll17/services/identity/IdentityAuthorityService.kt",
        ],
    ),
    (
        "Qualification suites",
        [
            "core-persistence/src/test/kotlin/com/animusmachinae/dll17/core/persistence/PersistenceBackendTest.kt",
            "core-persistence/src/test/kotlin/com/animusmachinae/dll17/core/persistence/LocalStorageCryptographyTest.kt",
            "core-persistence/src/test/kotlin/com/animusmachinae/dll17/core/persistence/ModuleBoundaryTest.kt",
            "core-recovery/src/test/kotlin/com/animusmachinae/dll17/core/recovery/RecoveryCryptographyTest.kt",
            "core-recovery/src/test/kotlin/com/animusmachinae/dll17/core/recovery/RecoveryPackageStoreConformanceTest.kt",
            "services/identity-authority/src/test/kotlin/com/animusmachinae/dll17/services/identity/IdentityAuthorityServiceTest.kt",
            "core-crypto/src/test/kotlin/com/animusmachinae/dll17/core/crypto/KeyDerivationTest.kt",
        ],
    ),
    (
        "Backend selection and measured evidence",
        [
            "benchmarks/persistence-bench/build.gradle.kts",
            "benchmarks/persistence-bench/src/main/kotlin/com/animusmachinae/dll17/bench/PersistenceBackendBenchmark.kt",
            "qualification/evidence/R012/backend_benchmark.txt",
            "qualification/evidence/R012/performance.txt",
        ],
    ),
    (
        "Qualification evidence",
        [
            "qualification/fixtures/R012/R012_REPORT.txt",
            "qualification/evidence/R012/governance_validation.txt",
            "qualification/evidence/R012/project_identity.txt",
            "qualification/evidence/R012/gradle_build.txt",
            "qualification/evidence/R012/toolchain_environment.txt",
        ],
    ),
    (
        "Build and toolchain state",
        [
            "settings.gradle.kts",
            "gradle/libs.versions.toml",
            "desktop-runner/build.gradle.kts",
            CI_WORKFLOW,
        ],
    ),
    ("Gate record", ["governance/release-gates/R012_SUBSTRATE_GATE.md"]),
]


# D012: the Android device half of the R012 substrate. Deliberately a separate
# bundle rather than an extension of R012-QB-1. The substrate bundle is closed
# and pinned; this one covers the Android adapter, the device qualification
# suite and the evidence a target produced, and it can close on its own schedule
# — which matters, because it cannot close at all until a physical device is
# available.
R012DEV_CONSTITUENTS: list[tuple[str, list[str]]] = [
    (
        "Android production adapter",
        [
            "android-host/build.gradle.kts",
            "android-host/src/main/AndroidManifest.xml",
            "android-host/src/main/kotlin/com/animusmachinae/dll17/android/persistence/AndroidKeystoreDeviceKeyContainer.kt",
            "android-host/src/main/kotlin/com/animusmachinae/dll17/android/persistence/AndroidPersistenceLocations.kt",
            "android-host/src/main/kotlin/com/animusmachinae/dll17/android/persistence/AndroidLocalKeyBootstrap.kt",
        ],
    ),
    (
        "Backup and device-transfer exclusion",
        [
            "android-host/src/main/res/xml/data_extraction_rules.xml",
            "android-host/src/main/res/xml/backup_rules.xml",
            "android-host/src/test/kotlin/com/animusmachinae/dll17/android/BackupExclusionTest.kt",
            "tools/verify_backup_exclusion.py",
        ],
    ),
    (
        "Device qualification suite",
        [
            "android-host/src/debug/AndroidManifest.xml",
            "android-host/src/debug/kotlin/com/animusmachinae/dll17/android/persistence/DeviceCrashService.kt",
            "android-host/src/androidTest/kotlin/com/animusmachinae/dll17/android/persistence/R012DeviceQualificationKernel.kt",
            "android-host/src/androidTest/kotlin/com/animusmachinae/dll17/android/persistence/R012DeviceQualificationInstrumentedTest.kt",
            "android-host/src/androidTest/kotlin/com/animusmachinae/dll17/android/persistence/R012DeviceMeasurementsInstrumentedTest.kt",
            "android-host/src/test/kotlin/com/animusmachinae/dll17/android/AndroidLocalKeyBootstrapTest.kt",
        ],
    ),
    (
        "Target evidence",
        [
            "qualification/device-matrix/R012/DEVICE_MATRIX.md",
            "qualification/device-matrix/R012/x86_emulator_qualification.txt",
            "qualification/device-matrix/R012/x86_emulator_performance.txt",
            "qualification/evidence/R012/backup_exclusion.txt",
        ],
    ),
    ("Gate record", ["governance/release-gates/R012_DEVICE_GATE.md"]),
]


# D013: the corrected substrate. R012-QB-1 stays pinned to the commit it was
# qualified at and is not touched — the epoch-separation correction is a real
# change to a constituent of a closed gate, and the honest way to record that is
# a successor bundle rather than a quiet edit to the old one. This bundle adds
# the V2 contract, the amended record boundary in `core-continuity`, and the
# regression evidence that the correction is qualified.
R012V2_CONSTITUENTS: list[tuple[str, list[str]]] = [
    (
        "Frozen contracts",
        [
            "docs/architecture/PersistenceBackendContractV1.md",
            "docs/architecture/LocalStorageCryptographyContractV2.md",
            "docs/architecture/LocalStorageCryptographyContractV1.md",
            "docs/architecture/RecoveryCryptographyContractV1.md",
            "docs/architecture/IdentityAuthorityProtocolV1.md",
            "docs/architecture/RecoveryPackageStoreContractV1.md",
        ],
    ),
    (
        "Amended record boundary",
        [
            "core-continuity/src/main/kotlin/com/animusmachinae/dll17/core/continuity/DurableStore.kt",
            "core-continuity/src/test/kotlin/com/animusmachinae/dll17/core/continuity/DurabilityAndJournalTest.kt",
        ],
    ),
    (
        "Persistence implementation",
        [
            "core-persistence/build.gradle.kts",
            "core-persistence/src/main/kotlin/com/animusmachinae/dll17/core/persistence/PersistenceBackend.kt",
            "core-persistence/src/main/kotlin/com/animusmachinae/dll17/core/persistence/LocalStorageCryptography.kt",
            "core-persistence/src/main/kotlin/com/animusmachinae/dll17/core/persistence/ColdRecoveryActivation.kt",
            "core-persistence/src/main/kotlin/com/animusmachinae/dll17/core/persistence/CrashHarness.kt",
            "core-persistence/src/main/kotlin/com/animusmachinae/dll17/core/persistence/R012QualificationKernel.kt",
            "core-persistence/src/main/kotlin/com/animusmachinae/dll17/core/persistence/R012PerformanceHarness.kt",
        ],
    ),
    (
        "Qualification suites",
        [
            "core-persistence/src/test/kotlin/com/animusmachinae/dll17/core/persistence/PersistenceBackendTest.kt",
            "core-persistence/src/test/kotlin/com/animusmachinae/dll17/core/persistence/LocalStorageCryptographyTest.kt",
            "core-persistence/src/test/kotlin/com/animusmachinae/dll17/core/persistence/ModuleBoundaryTest.kt",
        ],
    ),
    (
        "Qualification evidence",
        [
            "qualification/fixtures/R012/R012_REPORT.txt",
            "qualification/evidence/R012V2/governance_validation.txt",
            "qualification/evidence/R012V2/project_identity.txt",
            "qualification/evidence/R012V2/gradle_build.txt",
            "qualification/evidence/R012V2/toolchain_environment.txt",
        ],
    ),
    ("Prior art", ["docs/decisions/EXTERNAL_PRIOR_ART.md"]),
    ("Gate record", ["governance/release-gates/R012_SUBSTRATE_GATE_V2.md"]),
]


class PhaseSpec:
    def __init__(
        self,
        phase: str,
        title: str,
        directive: str,
        bundle_version: str,
        bundle_path: str,
        constituents: list[tuple[str, list[str]]],
        frozen_at_commit: str | None,
    ) -> None:
        self.phase = phase
        self.title = title
        self.directive = directive
        self.bundle_version = bundle_version
        self.bundle_path = ROOT / bundle_path
        self.constituents = constituents
        self.frozen_at_commit = frozen_at_commit

    @property
    def is_frozen(self) -> bool:
        return self.frozen_at_commit is not None


PHASES: dict[str, PhaseSpec] = {
    "R000": PhaseSpec(
        phase="R000",
        title="greenfield project initialization",
        directive="D005",
        bundle_version="R000-QB-1",
        bundle_path="governance/qualification/R000_QUALIFICATION_BUNDLE.md",
        constituents=R000_CONSTITUENTS,
        # The commit whose blobs these hashes were computed from. R000 is closed;
        # its evidence is pinned here so that later phases cannot disturb it.
        frozen_at_commit="43054d0a2a210bc48563cc81016d6083bff2a182",
    ),
    "R001": PhaseSpec(
        phase="R001",
        title="deterministic fixed-point spike",
        directive="D006",
        bundle_version="R001-QB-1",
        bundle_path="governance/qualification/R001_QUALIFICATION_BUNDLE.md",
        constituents=R001_CONSTITUENTS,
        # R001 closed under D006 at this commit. Its evidence is pinned so that
        # R002 and later phases cannot disturb a gate that already passed.
        frozen_at_commit="e442e1478deed9e70f5f2b547c92071ba8bce6ff",
    ),
    "R002": PhaseSpec(
        phase="R002",
        title="lifecycle, durability, trusted time and reconciliation",
        directive="D007",
        bundle_version="R002-QB-1",
        bundle_path="governance/qualification/R002_QUALIFICATION_BUNDLE.md",
        constituents=R002_CONSTITUENTS,
        # R002 closed under D007 at this commit and the architect accepted it.
        # Pinned for the same reason R000 and R001 are: A000 adds a Gradle module
        # and therefore edits `settings.gradle.kts`, which is an R002 constituent.
        # A closed gate must not break because a later track exists.
        frozen_at_commit="7f6f37fabba6a5ad4af2fd517e62cb4c08dbfeb2",
    ),
    "A000": PhaseSpec(
        phase="A000",
        title="disposable aliveness spike research track",
        directive="D008, D009",
        bundle_version="A000-QB-2",
        bundle_path="governance/qualification/A000_QUALIFICATION_BUNDLE.md",
        constituents=A000_CONSTITUENTS,
        # A000 closed under D009 at this commit and the architect accepted it.
        # Pinned for the same reason R000, R001 and R002 are: D010 legitimately
        # edits A000 artifacts — the kernel gains a preregistered ablation cohort
        # and the gate record gains a figure reconciliation — and a gate that has
        # already passed must not depend on what a later directive does.
        frozen_at_commit="4a2b1e4c7ce1326b5c8d5b08d873df7f581186d7",
    ),
    "R012": PhaseSpec(
        phase="R012",
        title="persistence, recovery and identity substrate",
        directive="D011",
        bundle_version="R012-QB-1",
        bundle_path="governance/qualification/R012_SUBSTRATE_BUNDLE.md",
        constituents=R012_CONSTITUENTS,
        # Closed under D011 and accepted by the architect at this commit. Pinned
        # for the fifth time for the reason IMPL-0014 identified: D012 runs the
        # performance harness on Android, where `Files.getFileStore` is refused
        # by the platform, and fixing that portability defect edits a constituent
        # of a gate that has already passed. The pin means the R012 substrate
        # bundle is still verified against exactly what was qualified.
        frozen_at_commit="afd0ecdb21bd20a00d4f3b6ae69d31e61890707c",
    ),
    "R012V2": PhaseSpec(
        phase="R012V2",
        title="local-storage epoch separation",
        directive="D013",
        bundle_version="R012-QB-2",
        bundle_path="governance/qualification/R012_SUBSTRATE_BUNDLE_V2.md",
        constituents=R012V2_CONSTITUENTS,
        frozen_at_commit=None,
    ),
    "R012DEV": PhaseSpec(
        phase="R012DEV",
        title="R012 substrate on Android hardware",
        directive="D012",
        bundle_version="R012DEV-QB-1",
        bundle_path="governance/qualification/R012_DEVICE_BUNDLE.md",
        constituents=R012DEV_CONSTITUENTS,
        # Pinned to the commit D012 filed, even though its gate is BLOCKED rather
        # than passed. D013 re-ran the device suite and the fixture that reported
        # NOT HELD now holds — which would silently erase the negative evidence
        # if this bundle followed the working tree. The D012 record stays exactly
        # as it was written, verifiable, and the D013 run is a separate bundle.
        frozen_at_commit="4700b0762cad3b1bb63a69be4f7eca9caea3b819",
    ),
    "R012DEV2": PhaseSpec(
        phase="R012DEV2",
        title="R012 substrate on Android hardware, after epoch separation",
        directive="D013",
        bundle_version="R012DEV-QB-2",
        bundle_path="governance/qualification/R012_DEVICE_BUNDLE_V2.md",
        constituents=R012DEV_CONSTITUENTS,
        frozen_at_commit=None,
    ),
    "A001PRE": PhaseSpec(
        phase="A001PRE",
        title="A001 activation package, prepared without human data",
        directive="D010",
        bundle_version="A001PRE-QB-1",
        bundle_path="governance/qualification/A001_ACTIVATION_PACKAGE_BUNDLE.md",
        constituents=A001PRE_CONSTITUENTS,
        # Closed under D010 and accepted by the architect at this commit. Pinned
        # for the fourth time for the reason IMPL-0014 identified: D011 adds
        # Gradle modules and a CI step, and both `settings.gradle.kts` and the
        # workflow are A001PRE constituents. A gate that has already passed must
        # not break because a later directive exists.
        frozen_at_commit="3065c073aac271d0b99d8af40e0b89c852a0b255",
    ),
}


def git(*args: str) -> str:
    try:
        out = subprocess.run(
            ["git", *args], cwd=ROOT, capture_output=True, text=True, check=False
        )
        return out.stdout.strip()
    except OSError:
        return ""


def commit_present(commit: str) -> bool:
    """True if `commit` is a real object in this clone.

    Worth checking separately. A shallow clone does not contain the commit, and
    without this the per-file lookup fails for every constituent and reports
    them all as "absent from <commit>", which reads as evidence tampering rather
    than as a clone-depth problem.
    """
    try:
        out = subprocess.run(
            ["git", "cat-file", "-e", f"{commit}^{{commit}}"],
            cwd=ROOT,
            capture_output=True,
            check=False,
        )
    except OSError:
        return False
    return out.returncode == 0


def git_blob(commit: str, rel: str) -> bytes | None:
    try:
        out = subprocess.run(
            ["git", "show", f"{commit}:{rel}"],
            cwd=ROOT,
            capture_output=True,
            check=False,
        )
    except OSError:
        return None
    if out.returncode != 0:
        return None
    return out.stdout


def sha256_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def sha256_file(path: pathlib.Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(65536), b""):
            digest.update(chunk)
    return digest.hexdigest()


def collect(spec: PhaseSpec) -> tuple[list[tuple[str, str, str]], list[str]]:
    """Return (group, relative path, sha256) rows and a list of missing paths."""
    rows: list[tuple[str, str, str]] = []
    missing: list[str] = []

    if spec.is_frozen and not commit_present(spec.frozen_at_commit or ""):
        return [], [
            f"commit {spec.frozen_at_commit} is not present in this clone, so the "
            f"{spec.phase} bundle cannot be verified. This is almost always a shallow "
            "checkout: fetch full history (actions/checkout with fetch-depth: 0). It is "
            "not evidence that a constituent changed."
        ]

    for group, paths in spec.constituents:
        for rel in paths:
            if spec.is_frozen:
                blob = git_blob(spec.frozen_at_commit or "", rel)
                if blob is None:
                    missing.append(f"{rel} (absent from {spec.frozen_at_commit})")
                    continue
                rows.append((group, rel, sha256_bytes(blob)))
            else:
                target = ROOT / rel
                if not target.is_file():
                    missing.append(rel)
                    continue
                rows.append((group, rel, sha256_file(target)))
    return rows, missing


def manifest_hash(rows: list[tuple[str, str, str]]) -> str:
    """Hash over the sorted `sha256  path` lines. Order-independent by
    construction, so it does not depend on how this script groups things."""
    lines = sorted(f"{sha}  {rel}" for _, rel, sha in rows)
    return hashlib.sha256("\n".join(lines).encode("utf-8") + b"\n").hexdigest()


def read_pin(name: str) -> str:
    catalog = (ROOT / "gradle" / "libs.versions.toml").read_text(encoding="utf-8")
    match = re.search(rf'^{name}\s*=\s*"([^"]+)"', catalog, re.MULTILINE)
    return match.group(1) if match else "unknown"


def gradle_pin() -> str:
    props = (ROOT / "gradle" / "wrapper" / "gradle-wrapper.properties").read_text(
        encoding="utf-8"
    )
    match = re.search(r"gradle-([0-9.]+)-bin\.zip", props)
    return match.group(1) if match else "unknown"


def render(spec: PhaseSpec, rows: list[tuple[str, str, str]]) -> str:
    mh = manifest_hash(rows)
    head = git("rev-parse", "HEAD") or "unavailable"
    branch = git("rev-parse", "--abbrev-ref", "HEAD") or "unavailable"

    out: list[str] = []
    out.append(f"# {spec.phase} qualification evidence bundle")
    out.append("")
    out.append(f"- Bundle version: `{spec.bundle_version}`")
    out.append(f"- Phase: {spec.phase} — {spec.title}")
    out.append(f"- Produced under: architect directive {spec.directive}")
    out.append(f"- Manifest hash (SHA-256): `{mh}`")
    out.append(f"- Constituent count: {len(rows)}")
    out.append("")
    out.append(
        "This bundle is generated by `tools/build_qualification_bundle.py` and\n"
        "verified by the same script with `--verify`, which CI runs on every push\n"
        "and pull request. Any change to a constituent without regenerating the\n"
        "bundle fails the build."
    )
    out.append("")

    out.append("## What this bundle qualifies")
    out.append("")
    out.append("| Binding | Value |")
    out.append("|---|---|")
    out.append(f"| Generated from commit | `{head}` |")
    out.append(f"| Branch | `{branch}` |")
    out.append("| Determinism contract | `DeterminismContractV1` (FROZEN, version 1) |")
    out.append("| Identity contract | `ProjectIdentityBuildContractV1` (FROZEN) |")
    out.append(f"| Kotlin | `{read_pin('kotlin')}` |")
    out.append(f"| Android Gradle Plugin | `{read_pin('agp')}` |")
    out.append(f"| Gradle | `{gradle_pin()}` |")
    out.append(f"| Compose BOM | `{read_pin('composeBom')}` |")
    out.append(f"| CI workflow | `{CI_WORKFLOW}` |")
    out.append("")
    out.append(
        "`Generated from commit` is the commit whose working tree produced these\n"
        "hashes. The qualified commit is its immediate descendant, the one that\n"
        "contains this file: a bundle cannot contain the hash of the commit that\n"
        "contains it. The manifest hash is the durable identifier, because it is\n"
        "computed from file content only and is therefore independent of commit\n"
        "identity, rebases and merges. `.agent/OUTCOMES.md` records the final\n"
        "qualified commit SHA.\n"
        "\n"
        "Once this phase's gate closes, the phase is pinned in\n"
        "`tools/build_qualification_bundle.py` to its qualified commit, and\n"
        "verification reads that commit's blobs instead of the working tree. A\n"
        "closed gate must not be breakable by a later phase editing a shared file."
    )
    out.append("")

    out.append("## Constituent manifest")
    out.append("")
    out.append("| Binding | Artifact | SHA-256 |")
    out.append("|---|---|---|")
    for group, rel, sha in rows:
        out.append(f"| {group} | `{rel}` | `{sha}` |")
    out.append("")

    out.append("## Reproducing this bundle")
    out.append("")
    out.append("```")
    out.append("python3 tools/build_qualification_bundle.py --verify")
    out.append("```")
    out.append("")
    if spec.phase == "R000":
        out.append(
            "To reproduce the underlying evidence rather than just verify it, rerun\n"
            "the validation commands recorded in\n"
            "`qualification/evidence/R000/` and the Android harness\n"
            "`tools/qualify_r000_android.sh` with a device serial. Device evidence\n"
            "will differ in timings and process IDs between runs; that is expected.\n"
            "The recorded artifacts are the observations that were actually made,\n"
            "not a claim that byte-identical logs will recur."
        )
    else:
        out.append(
            "To reproduce the underlying evidence rather than just verify it, run\n"
            "`./gradlew build`, `./gradlew :desktop-runner:run`,\n"
            "`python3 tools/generate_lookup_tables.py --check`, and the cross-target\n"
            "harness `tools/qualify_r001_determinism.sh` against each target.\n"
            "\n"
            "Unlike the R000 device evidence, the determinism evidence **is**\n"
            "expected to be byte-identical on every rerun and on every target. That\n"
            "is the entire claim: if `R001_EVIDENCE_DIGEST` differs between two runs\n"
            "or two targets, R001 has failed."
        )
    out.append("")
    return "\n".join(out)


def verify_phase(spec: PhaseSpec) -> tuple[bool, list[str]]:
    rows, missing = collect(spec)
    errors: list[str] = [f"missing constituent: {rel}" for rel in missing]

    if not spec.bundle_path.is_file():
        errors.append(f"bundle not found at {spec.bundle_path.relative_to(ROOT)}")
        return False, errors

    committed = spec.bundle_path.read_text(encoding="utf-8")

    recorded = re.search(r"Manifest hash \(SHA-256\): `([0-9a-f]{64})`", committed)
    if not recorded:
        errors.append("bundle does not record a manifest hash")
    elif not missing:
        actual = manifest_hash(rows)
        if recorded.group(1) != actual:
            source = (
                f"commit {spec.frozen_at_commit[:12]}"
                if spec.is_frozen and spec.frozen_at_commit
                else "working tree"
            )
            errors.append(
                f"manifest hash mismatch: bundle records {recorded.group(1)}, "
                f"{source} computes {actual}"
            )

    for _, rel, sha in rows:
        if f"| `{rel}` | `{sha}` |" not in committed:
            errors.append(f"constituent hash not recorded or changed: {rel} ({sha})")

    return not errors, errors


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--verify",
        action="store_true",
        help="verify committed bundles; do not write",
    )
    parser.add_argument(
        "--phase",
        choices=sorted(PHASES),
        help="phase to write (required unless --verify)",
    )
    args = parser.parse_args()

    if args.verify:
        failed = False
        for name in sorted(PHASES):
            spec = PHASES[name]
            ok, errors = verify_phase(spec)
            source = (
                f"commit {spec.frozen_at_commit[:12]}"
                if spec.is_frozen and spec.frozen_at_commit
                else "working tree"
            )
            if ok:
                rows, _ = collect(spec)
                print(f"{name} qualification bundle verification PASSED ({source})")
                print(f"  - verified {len(rows)} constituents")
                print(f"  - manifest hash {manifest_hash(rows)}")
            else:
                failed = True
                print(
                    f"{name} qualification bundle verification FAILED ({source})",
                    file=sys.stderr,
                )
                for error in errors:
                    print(f"  - {error}", file=sys.stderr)
        if failed:
            print(
                "\nRegenerate the live phase with: "
                "python3 tools/build_qualification_bundle.py --phase R001",
                file=sys.stderr,
            )
            return 1
        return 0

    if not args.phase:
        parser.error("--phase is required unless --verify is given")

    spec = PHASES[args.phase]
    if spec.is_frozen:
        print(
            f"refusing to rewrite {spec.phase}: its gate is closed and it is pinned to "
            f"commit {spec.frozen_at_commit}",
            file=sys.stderr,
        )
        return 1

    rows, missing = collect(spec)
    if missing:
        print(f"{spec.phase} qualification bundle FAILED", file=sys.stderr)
        for rel in missing:
            print(f"  - missing constituent: {rel}", file=sys.stderr)
        return 1

    spec.bundle_path.parent.mkdir(parents=True, exist_ok=True)
    spec.bundle_path.write_text(render(spec, rows), encoding="utf-8")
    print(f"{spec.phase} qualification bundle WRITTEN")
    print(f"  - {spec.bundle_path.relative_to(ROOT)}")
    print(f"  - {len(rows)} constituents")
    print(f"  - manifest hash {manifest_hash(rows)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
