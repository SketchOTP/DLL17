#!/usr/bin/env python3
"""Verify the build files against the frozen ProjectIdentityBuildContractV1.

The contract is prose; this script is the enforcement. It fails if a frozen
identity or toolchain value drifts, if a core module gains an Android plugin, or
if a dynamic dependency version appears anywhere in the version catalog.

Usage:
    python3 tools/verify_project_identity.py

Exit code 0 means every contract value still holds.
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

CONTRACT = ROOT / "docs/architecture/ProjectIdentityBuildContractV1.md"
CATALOG = ROOT / "gradle/libs.versions.toml"
WRAPPER = ROOT / "gradle/wrapper/gradle-wrapper.properties"
ANDROID_BUILD = ROOT / "android-host/build.gradle.kts"
SETTINGS = ROOT / "settings.gradle.kts"

CORE_MODULES = ("core-math", "core-crypto", "core-state")
JVM_MODULES = CORE_MODULES + ("desktop-runner",)
ALL_MODULES = JVM_MODULES + ("android-host",)

# The frozen values. These are architect-supplied and are duplicated here on
# purpose: the script must fail if either the contract document or a build file
# is edited alone.
FROZEN = {
    "project": "DLL17",
    "display_name": "Digital Living Lifeform",
    "application_id": "com.animusmachinae.dll17",
    "namespace": "com.animusmachinae.dll17",
    "kotlin": "2.4.10",
    "jdk": "17",
    "agp": "9.3.1",
    "gradle": "9.5.0",
    "compile_sdk": "37",
    "target_sdk": "37",
    "min_sdk": "29",
    "compose_bom": "2026.06.00",
    "version_code": "1",
    "version_name": "0.1.0-dev",
}

# Substrings that indicate a non-exact dependency version.
DYNAMIC_MARKERS = ("latest.release", "latest.integration", "+", "[", "]", "(,", ",)")


def read(path: Path) -> str:
    if not path.is_file():
        raise SystemExit(f"missing required file: {path.relative_to(ROOT)}")
    return path.read_text(encoding="utf-8")


def check(errors: list[str], condition: bool, message: str) -> None:
    if not condition:
        errors.append(message)


def check_contract_document(errors: list[str]) -> None:
    text = read(CONTRACT)
    for key in (
        "project",
        "display_name",
        "application_id",
        "kotlin",
        "agp",
        "gradle",
        "compile_sdk",
        "min_sdk",
        "compose_bom",
        "version_name",
    ):
        value = FROZEN[key]
        check(
            errors,
            f"`{value}`" in text,
            f"contract document does not state the frozen value `{value}` for {key}",
        )
    check(errors, "Status: `FROZEN`" in text, "contract document is not marked FROZEN")


def check_version_catalog(errors: list[str]) -> None:
    text = read(CATALOG)
    for key, version in (
        ("kotlin", FROZEN["kotlin"]),
        ("agp", FROZEN["agp"]),
        ("composeBom", FROZEN["compose_bom"]),
    ):
        check(
            errors,
            re.search(rf'^{key}\s*=\s*"{re.escape(version)}"$', text, re.MULTILINE) is not None,
            f"version catalog does not pin {key} to {version}",
        )

    for line_number, line in enumerate(text.splitlines(), start=1):
        stripped = line.strip()
        if not stripped or stripped.startswith("#") or stripped.startswith("["):
            continue
        for marker in DYNAMIC_MARKERS:
            if marker in stripped:
                errors.append(
                    f"gradle/libs.versions.toml:{line_number} uses a dynamic version "
                    f"marker {marker!r}: {stripped}"
                )


def check_wrapper(errors: list[str]) -> None:
    text = read(WRAPPER)
    expected = f"gradle-{FROZEN['gradle']}-bin.zip"
    check(
        errors,
        expected in text,
        f"gradle wrapper is not pinned to {expected}",
    )


def check_android_module(errors: list[str]) -> None:
    text = read(ANDROID_BUILD)
    expectations = (
        (rf'namespace\s*=\s*"{re.escape(FROZEN["namespace"])}"', "namespace"),
        (rf'applicationId\s*=\s*"{re.escape(FROZEN["application_id"])}"', "applicationId"),
        (rf'compileSdk\s*=\s*{FROZEN["compile_sdk"]}\b', "compileSdk"),
        (rf'targetSdk\s*=\s*{FROZEN["target_sdk"]}\b', "targetSdk"),
        (rf'minSdk\s*=\s*{FROZEN["min_sdk"]}\b', "minSdk"),
        (rf'versionCode\s*=\s*{FROZEN["version_code"]}\b', "versionCode"),
        (rf'versionName\s*=\s*"{re.escape(FROZEN["version_name"])}"', "versionName"),
    )
    for pattern, field in expectations:
        check(
            errors,
            re.search(pattern, text) is not None,
            f"android-host/build.gradle.kts does not set the frozen {field}",
        )

    check(errors, "debug {" in text, "android-host is missing the debug build type")
    check(errors, "release {" in text, "android-host is missing the release build type")
    check(
        errors,
        "productFlavors" not in text,
        "product flavors are prohibited by the contract",
    )
    check(
        errors,
        "signingConfigs" not in text,
        "production signing is deferred; no signing config may be declared yet",
    )


def check_core_boundary(errors: list[str]) -> None:
    for module in CORE_MODULES:
        text = read(ROOT / module / "build.gradle.kts")
        for forbidden in ("com.android", "android.application", "android.library", "kotlin.android"):
            check(
                errors,
                forbidden not in text,
                f"{module}/build.gradle.kts references {forbidden}; core modules must stay Android-free",
            )
        check(
            errors,
            "libs.plugins.kotlin.jvm" in text,
            f"{module}/build.gradle.kts does not apply the Kotlin JVM plugin",
        )

    desktop = read(ROOT / "desktop-runner/build.gradle.kts")
    check(
        errors,
        "com.android" not in desktop,
        "desktop-runner must stay Android-free",
    )


def check_toolchain(errors: list[str]) -> None:
    for module in ALL_MODULES:
        text = read(ROOT / module / "build.gradle.kts")
        check(
            errors,
            f"jvmToolchain({FROZEN['jdk']})" in text,
            f"{module}/build.gradle.kts does not pin jvmToolchain({FROZEN['jdk']})",
        )


def check_settings(errors: list[str]) -> None:
    text = read(SETTINGS)
    check(
        errors,
        f'rootProject.name = "{FROZEN["project"]}"' in text,
        f"settings.gradle.kts does not name the root project {FROZEN['project']}",
    )
    for module in ALL_MODULES:
        check(
            errors,
            f'include(":{module}")' in text,
            f"settings.gradle.kts does not include :{module}",
        )


def main() -> int:
    errors: list[str] = []
    check_contract_document(errors)
    check_version_catalog(errors)
    check_wrapper(errors)
    check_android_module(errors)
    check_core_boundary(errors)
    check_toolchain(errors)
    check_settings(errors)

    if errors:
        print("ProjectIdentityBuildContractV1 verification FAILED")
        for error in errors:
            print(f"- {error}")
        return 1

    print("ProjectIdentityBuildContractV1 verification PASSED")
    print(f"- checked {len(ALL_MODULES)} modules")
    print(f"- checked {len(FROZEN)} frozen contract values")
    return 0


if __name__ == "__main__":
    sys.exit(main())
