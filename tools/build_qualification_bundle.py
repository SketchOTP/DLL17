#!/usr/bin/env python3
"""Build or verify the R000 qualification evidence bundle.

The bundle binds the R000 qualification claim to exact, immutable artifacts. It
records a SHA-256 for every constituent file plus a manifest hash over the whole
set, so a later architect can determine exactly what was qualified and detect
any drift.

Usage:
    tools/build_qualification_bundle.py            # regenerate the bundle
    tools/build_qualification_bundle.py --verify   # fail if anything drifted

`--verify` is what CI runs. It recomputes every hash from the working tree and
compares it to the committed manifest. It never rewrites the bundle, so a
tampered or stale bundle fails the build instead of silently healing.

Dependency-free by design: this must run on any machine with Python 3, with no
package installation, exactly like the governance validator.
"""

from __future__ import annotations

import argparse
import hashlib
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
BUNDLE = ROOT / "governance" / "qualification" / "R000_QUALIFICATION_BUNDLE.md"

BUNDLE_VERSION = "R000-QB-1"

# Every constituent of the R000 qualification claim, grouped by what it binds.
# A path listed here must exist; a missing constituent is a qualification
# failure, not a warning.
CONSTITUENTS: list[tuple[str, list[str]]] = [
    (
        "Project identity contract",
        ["docs/architecture/ProjectIdentityBuildContractV1.md"],
    ),
    (
        "Dependency and toolchain lock state",
        [
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
        ],
    ),
    (
        "Source and licence state",
        [
            "LICENSE",
            "governance/source-provenance/DEPENDENCY_LICENSE_INVENTORY.md",
            "governance/source-provenance/SOURCE_PROVENANCE_LEDGER.md",
        ],
    ),
    (
        "Mandatory R000 registry state",
        [
            "docs/architecture/registries/CanonicalStateCatalog.md",
            "docs/architecture/registries/EventCatalog.md",
            "docs/architecture/registries/ParameterRegistry.md",
            "docs/architecture/registries/RandomDomainRegistry.md",
            "docs/architecture/registries/StateTransitionMatrix.md",
            "docs/architecture/registries/ThreadAndDispatcherMap.md",
            "docs/architecture/registries/PresentationContractCatalog.md",
        ],
    ),
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
    (
        "Gate record",
        ["governance/release-gates/R000_EXIT_GATE.md"],
    ),
]

# Facts recorded in the bundle header. Values are read from the tree, never
# hard-coded, so the bundle cannot drift from the project it describes.
CI_WORKFLOW = ".github/workflows/ci.yml"


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(65536), b""):
            digest.update(chunk)
    return digest.hexdigest()


def git(*args: str) -> str:
    try:
        out = subprocess.run(
            ["git", *args], cwd=ROOT, capture_output=True, text=True, check=False
        )
        return out.stdout.strip()
    except OSError:
        return ""


def collect() -> tuple[list[tuple[str, str, str]], list[str]]:
    """Return (group, relative path, sha256) rows and a list of missing paths."""
    rows: list[tuple[str, str, str]] = []
    missing: list[str] = []
    for group, paths in CONSTITUENTS:
        for rel in paths:
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


def render(rows: list[tuple[str, str, str]]) -> str:
    mh = manifest_hash(rows)
    head = git("rev-parse", "HEAD") or "unavailable"
    branch = git("rev-parse", "--abbrev-ref", "HEAD") or "unavailable"

    out: list[str] = []
    out.append("# R000 qualification evidence bundle")
    out.append("")
    out.append(f"- Bundle version: `{BUNDLE_VERSION}`")
    out.append("- Phase: R000 — greenfield project initialization")
    out.append("- Produced under: architect directive D005")
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
    out.append(f"| Contract | `ProjectIdentityBuildContractV1` (FROZEN) |")
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
        "qualified commit SHA."
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
    out.append(
        "To reproduce the underlying evidence rather than just verify it, rerun\n"
        "the validation commands recorded in\n"
        "`qualification/evidence/R000/` and the Android harness\n"
        "`tools/qualify_r000_android.sh <serial>`. Device evidence will differ in\n"
        "timings and process IDs between runs; that is expected. The recorded\n"
        "artifacts are the observations that were actually made, not a claim that\n"
        "byte-identical logs will recur."
    )
    out.append("")
    return "\n".join(out)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--verify",
        action="store_true",
        help="verify the committed bundle against the working tree; do not write",
    )
    args = parser.parse_args()

    rows, missing = collect()

    if missing:
        print("R000 qualification bundle FAILED", file=sys.stderr)
        for rel in missing:
            print(f"  - missing constituent: {rel}", file=sys.stderr)
        return 1

    if not args.verify:
        BUNDLE.parent.mkdir(parents=True, exist_ok=True)
        BUNDLE.write_text(render(rows), encoding="utf-8")
        print("R000 qualification bundle WRITTEN")
        print(f"  - {BUNDLE.relative_to(ROOT)}")
        print(f"  - {len(rows)} constituents")
        print(f"  - manifest hash {manifest_hash(rows)}")
        return 0

    if not BUNDLE.is_file():
        print("R000 qualification bundle FAILED", file=sys.stderr)
        print(f"  - bundle not found at {BUNDLE.relative_to(ROOT)}", file=sys.stderr)
        return 1

    committed = BUNDLE.read_text(encoding="utf-8")
    errors: list[str] = []

    recorded = re.search(r"Manifest hash \(SHA-256\): `([0-9a-f]{64})`", committed)
    if not recorded:
        errors.append("bundle does not record a manifest hash")
    else:
        actual = manifest_hash(rows)
        if recorded.group(1) != actual:
            errors.append(
                f"manifest hash mismatch: bundle records {recorded.group(1)}, "
                f"working tree computes {actual}"
            )

    for _, rel, sha in rows:
        if f"| `{rel}` | `{sha}` |" not in committed:
            errors.append(f"constituent hash not recorded or changed: {rel} ({sha})")

    if errors:
        print("R000 qualification bundle verification FAILED", file=sys.stderr)
        for error in errors:
            print(f"  - {error}", file=sys.stderr)
        print(
            "\nRegenerate with: python3 tools/build_qualification_bundle.py",
            file=sys.stderr,
        )
        return 1

    print("R000 qualification bundle verification PASSED")
    print(f"  - verified {len(rows)} constituents")
    print(f"  - manifest hash {manifest_hash(rows)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
