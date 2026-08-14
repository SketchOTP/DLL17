#!/usr/bin/env python3
"""Verify Android backup and device-transfer exclusion from the built package.

Why this exists
---------------

`BackupExclusionTest` checks the source files, and an instrumented fixture checks
what the installed package reports about itself. Neither covers the step in
between: the manifest merger, the resource compiler and the packager. A rule
file that is written correctly, compiled correctly and then not referenced by the
merged manifest is silently permissive, and Auto Backup is opt-out by default —
so the failure mode is a package that quietly backs up a live organism.

This reads the *built* APK. It decodes the binary manifest and the compiled XML
resources with `aapt2` and asserts on what the packager actually produced.

Usage:
    tools/verify_backup_exclusion.py [--apk PATH ...] [--sdk ANDROID_HOME]

Exit code 0 on success, 1 on any failure, 2 if the tooling or the APK is absent
(which is reported as BLOCKED, never as a pass).
"""

from __future__ import annotations

import argparse
import pathlib
import re
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent

# The canonical directories the R012 Android adapter writes to. Kept in step with
# `AndroidPersistenceLocations.REQUIRED_EXCLUSION_PREFIXES`; a mismatch here is a
# real defect, because it means the code writes somewhere the rules do not cover.
REQUIRED_PATHS = [
    "canonical/",
    "journal/",
    "checkpoints/",
    "identity/",
    "keys/",
    "recovery/",
]

DEFAULT_APKS = [
    "android-host/build/outputs/apk/debug/android-host-debug.apk",
    "android-host/build/outputs/apk/release/android-host-release-unsigned.apk",
]


def find_aapt2(sdk: pathlib.Path | None) -> pathlib.Path | None:
    roots = [sdk] if sdk else []
    import os

    for env in ("ANDROID_HOME", "ANDROID_SDK_ROOT"):
        value = os.environ.get(env)
        if value:
            roots.append(pathlib.Path(value))
    for root in roots:
        build_tools = root / "build-tools"
        if not build_tools.is_dir():
            continue
        for version in sorted(build_tools.iterdir(), reverse=True):
            candidate = version / "aapt2"
            if candidate.is_file():
                return candidate
    return None


def dump(aapt2: pathlib.Path, apk: pathlib.Path, resource: str) -> str:
    result = subprocess.run(
        [str(aapt2), "dump", "xmltree", str(apk), "--file", resource],
        capture_output=True,
        text=True,
        check=False,
    )
    if result.returncode != 0:
        raise RuntimeError(f"aapt2 could not read {resource} from {apk.name}: {result.stderr.strip()}")
    return result.stdout


def excluded_paths(tree: str, section: str | None) -> list[str]:
    """Every `path` attribute inside `section`, from an aapt2 xmltree dump."""
    paths: list[str] = []
    depth_of_section: int | None = None
    for line in tree.splitlines():
        indent = len(line) - len(line.lstrip())
        stripped = line.strip()
        if stripped.startswith("E: "):
            name = stripped[3:].split(" ")[0]
            if depth_of_section is not None and indent <= depth_of_section and name != "exclude":
                depth_of_section = None
            if section is not None and name == section:
                depth_of_section = indent
            elif section is None and name in ("full-backup-content",):
                depth_of_section = indent
        elif stripped.startswith("A: ") and (section is None or depth_of_section is not None):
            match = re.search(r'\bpath\([^)]*\)="([^"]*)"', stripped) or re.search(
                r'\bpath="([^"]*)"', stripped
            )
            if match:
                paths.append(match.group(1))
    return paths


def resource_files(aapt2: pathlib.Path, apk: pathlib.Path) -> dict[str, str]:
    """Map every resource id to the file it resolves to inside the package.

    Needed because the release variant renames compiled resources — `res/4j.xml`
    rather than `res/xml/data_extraction_rules.xml` — so following the manifest's
    own reference is the only way to check the rules the platform would load.
    """
    result = subprocess.run(
        [str(aapt2), "dump", "resources", str(apk)], capture_output=True, text=True, check=False
    )
    mapping: dict[str, str] = {}
    current: str | None = None
    for line in result.stdout.splitlines():
        stripped = line.strip()
        entry = re.match(r"resource (0x[0-9a-f]+) ", stripped)
        if entry:
            current = entry.group(1)
            continue
        file_ref = re.search(r"\(file\) (\S+) type=", stripped)
        if file_ref and current:
            mapping[current] = file_ref.group(1)
            current = None
    return mapping


def check(apk: pathlib.Path, aapt2: pathlib.Path) -> list[str]:
    failures: list[str] = []
    manifest = dump(aapt2, apk, "AndroidManifest.xml")

    allow = re.search(r"allowBackup\([^)]*\)=(\S+)", manifest)
    if allow is None:
        failures.append(f"{apk.name}: the merged manifest declares no allowBackup at all")
    elif allow.group(1) not in ("false", "(type 0x12)0x0"):
        failures.append(f"{apk.name}: the merged manifest sets allowBackup to {allow.group(1)}")

    # The crash process is a debug-variant component whose entire purpose is to
    # die abruptly while holding canonical storage open. Checking that it is
    # absent from the release package belongs here, because "it is in a debug
    # source set" is a claim about the build files and this is the artifact.
    if "release" in apk.name and "DeviceCrashService" in manifest:
        failures.append(f"{apk.name}: the release package declares the crash process")

    # Follow the manifest's own references rather than assuming a path.
    ids = resource_files(aapt2, apk)
    resolved: dict[str, str | None] = {}
    for attribute in ("dataExtractionRules", "fullBackupContent"):
        reference = re.search(rf"{attribute}\([^)]*\)=@(0x[0-9a-f]+)", manifest)
        if reference is None:
            failures.append(f"{apk.name}: the merged manifest does not reference {attribute}")
            resolved[attribute] = None
            continue
        target = ids.get(reference.group(1))
        if target is None:
            failures.append(
                f"{apk.name}: {attribute} points at {reference.group(1)}, "
                "which resolves to no file in this package"
            )
        resolved[attribute] = target

    sections = [
        (resolved.get("dataExtractionRules"), "cloud-backup"),
        (resolved.get("dataExtractionRules"), "device-transfer"),
        (resolved.get("fullBackupContent"), None),
    ]
    for resource, section in sections:
        if resource is None:
            continue
        try:
            tree = dump(aapt2, apk, resource)
        except RuntimeError as failure:
            failures.append(f"{apk.name}: {failure}")
            continue
        found = excluded_paths(tree, section)
        label = section or "full-backup-content"
        for required in REQUIRED_PATHS:
            if required not in found:
                failures.append(f"{apk.name}: {label} does not exclude {required}")
        if "." not in found:
            failures.append(f"{apk.name}: {label} does not exclude the database domain")
        print(f"  {apk.name} {label}: {len(found)} exclusions -> {', '.join(found)}")
    return failures


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apk", action="append", default=None)
    parser.add_argument("--sdk", default=None)
    args = parser.parse_args()

    aapt2 = find_aapt2(pathlib.Path(args.sdk) if args.sdk else None)
    if aapt2 is None:
        print("BLOCKED: aapt2 was not found; set ANDROID_HOME or pass --sdk")
        return 2

    apks = [ROOT / rel for rel in (args.apk or DEFAULT_APKS)]
    present = [apk for apk in apks if apk.is_file()]
    if not present:
        print("BLOCKED: no built package found; run `gradlew :android-host:assemble` first")
        for apk in apks:
            print(f"  missing: {apk.relative_to(ROOT)}")
        return 2

    print(f"aapt2: {aapt2}")
    failures: list[str] = []
    for apk in present:
        print(f"package: {apk.relative_to(ROOT)}")
        failures.extend(check(apk, aapt2))

    missing = [apk for apk in apks if not apk.is_file()]
    for apk in missing:
        print(f"NOTE: {apk.relative_to(ROOT)} was not built and was not checked")

    if failures:
        print("\nFAILED")
        for failure in failures:
            print(f"  {failure}")
        return 1
    print("\nPASSED: every built package excludes canonical state from backup and device transfer")
    return 0


if __name__ == "__main__":
    sys.exit(main())
