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
        frozen_at_commit=None,
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
