#!/usr/bin/env python3
"""Generates the canonical lookup tables checked in under core-math.

`DeterminismContractV1` section 11 requires every lookup table to be produced by
a pure integer generator, checked in as generated source, and bound to a digest
so that a corrupted or hand-edited table fails loudly instead of drifting.

This script is also a deliberate second implementation. It computes the table's
canonical bytes and its SHA-256 digest using an independent Python encoder and
`hashlib`, never the Kotlin code. If the Kotlin canonical codec or the
hand-written Kotlin SHA-256 disagreed with this script, the generated digest
would not match at runtime and the table verification test would fail. That
makes the codec and the digest cross-checked against a foreign implementation
rather than only against themselves.

Usage:
    python3 tools/generate_lookup_tables.py           # write the generated source
    python3 tools/generate_lookup_tables.py --check   # fail if it would change
"""

from __future__ import annotations

import argparse
import hashlib
import pathlib
import sys

REPO_ROOT = pathlib.Path(__file__).resolve().parent.parent
OUTPUT = (
    REPO_ROOT
    / "core-math"
    / "src"
    / "main"
    / "kotlin"
    / "com"
    / "animusmachinae"
    / "dll17"
    / "core"
    / "math"
    / "GeneratedLookupTables.kt"
)

GENERATOR_ID = "LOOKUP_GENERATOR_V1"
GENERATOR_VERSION = 1

SCALE = 1_000_000

# Canonical envelope constants, DeterminismContractV1 section 10.
MAGIC = b"DL17"
ENVELOPE_FORMAT_VERSION = 1
CONTRACT_VERSION = 1
SCHEMA_ID_LOOKUP_TABLE = 101

HASH_DOMAIN_TAG = b"DLL17-STATE-HASH-V1"


# --------------------------------------------------------------- canonical codec


def u32(value: int) -> bytes:
    if value < 0 or value > 0x7FFFFFFF:
        raise ValueError(f"u32 out of range: {value}")
    return value.to_bytes(4, "big", signed=False)


def i32(value: int) -> bytes:
    return value.to_bytes(4, "big", signed=True)


def u16(value: int) -> bytes:
    return value.to_bytes(2, "big", signed=False)


def i64(value: int) -> bytes:
    return value.to_bytes(8, "big", signed=True)


def canonical_bytes(value: bytes) -> bytes:
    return u32(len(value)) + value


def canonical_identifier(value: str) -> bytes:
    if not 1 <= len(value) <= 64:
        raise ValueError(f"identifier length out of range: {value!r}")
    for ch in value:
        if not (ch.isascii() and (ch.isalnum() or ch in "._-")):
            raise ValueError(f"identifier character not canonical: {ch!r}")
    return canonical_bytes(value.encode("ascii"))


def wrap_envelope(schema_id: int, schema_version: int, payload: bytes) -> bytes:
    return (
        MAGIC
        + u16(ENVELOPE_FORMAT_VERSION)
        + i32(CONTRACT_VERSION)
        + i32(schema_id)
        + i32(schema_version)
        + u32(len(payload))
        + payload
    )


def canonical_state_hash(envelope: bytes) -> str:
    preimage = canonical_identifier(HASH_DOMAIN_TAG.decode("ascii")) + envelope
    return hashlib.sha256(preimage).hexdigest()


# ------------------------------------------------------------------- generators


def round_half_away_from_zero(numerator: int, denominator: int) -> int:
    """Matches FixedPoint's frozen rounding mode, on integers only."""
    if denominator <= 0:
        raise ValueError("denominator must be positive")
    sign = -1 if numerator < 0 else 1
    magnitude = abs(numerator)
    quotient, remainder = divmod(magnitude, denominator)
    if 2 * remainder >= denominator:
        quotient += 1
    return sign * quotient


def unit_ramp(steps: int) -> list[int]:
    """Fixed-point ramp from 0.0 to 1.0 inclusive, in `steps` equal intervals.

    This table carries no organism meaning. It exists so that the table
    descriptor, the generated-source path and the digest verification are proven
    by something real in R001. Tables with behavioural meaning belong to the
    phases that own the behaviour.
    """
    return [round_half_away_from_zero(index * SCALE, steps) for index in range(steps + 1)]


TABLES = [
    {
        "id": "LOOKUP_UNIT_RAMP_V1",
        "constant": "UNIT_RAMP",
        "version": 1,
        "parameters": "steps=256, scale=1_000_000, rounding=ROUND_HALF_AWAY_FROM_ZERO_V1",
        "doc": (
            "Fixed-point ramp from 0.000000 to 1.000000 across 256 equal intervals,\n"
            "     * 257 entries inclusive of both endpoints.\n"
            "     *\n"
            "     * Mechanism proof only: R001 defines no table with organism meaning."
        ),
        "values": unit_ramp(256),
    },
]


def table_envelope(table: dict) -> bytes:
    payload = canonical_identifier(table["id"])
    payload += u32(len(table["values"]))
    for value in table["values"]:
        payload += i64(value)
    return wrap_envelope(SCHEMA_ID_LOOKUP_TABLE, table["version"], payload)


def render() -> str:
    lines: list[str] = []
    lines.append("package com.animusmachinae.dll17.core.math")
    lines.append("")
    lines.append("// GENERATED FILE - DO NOT EDIT BY HAND.")
    lines.append("//")
    lines.append(f"// Generator ID:      {GENERATOR_ID}")
    lines.append(f"// Generator version: {GENERATOR_VERSION}")
    lines.append("// Generator source:  tools/generate_lookup_tables.py")
    lines.append("//")
    lines.append("// Regenerate with:   python3 tools/generate_lookup_tables.py")
    lines.append("// Verify with:       python3 tools/generate_lookup_tables.py --check")
    lines.append("//")
    lines.append("// Each table is bound to the digest of its canonical serialization. An edit")
    lines.append("// here that is not reproduced by the generator fails LookupTable.verify().")
    lines.append("")
    lines.append("/** Lookup tables generated by [GENERATOR_ID]. See DeterminismContractV1 section 11. */")
    lines.append("public object GeneratedLookupTables {")
    lines.append("")
    lines.append(f'    public const val GENERATOR_ID: String = "{GENERATOR_ID}"')
    lines.append(f"    public const val GENERATOR_VERSION: Int = {GENERATOR_VERSION}")

    for table in TABLES:
        envelope = table_envelope(table)
        digest = canonical_state_hash(envelope)
        values = table["values"]
        lines.append("")
        lines.append("    /**")
        lines.append(f"     * {table['doc']}")
        lines.append("     *")
        lines.append(f"     * Parameters: {table['parameters']}")
        lines.append(f"     * Entries: {len(values)}")
        lines.append("     */")
        lines.append(f"    public val {table['constant']}: LookupTable = LookupTable(")
        lines.append(f'        tableId = "{table["id"]}",')
        lines.append(f"        tableVersion = {table['version']},")
        lines.append(f'        expectedDigestHex = "{digest}",')
        lines.append("        values = longArrayOf(")
        for start in range(0, len(values), 8):
            chunk = values[start : start + 8]
            lines.append("            " + ", ".join(f"{value}L" for value in chunk) + ",")
        lines.append("        ),")
        lines.append("    )")

    lines.append("")
    lines.append("    /** Every generated table, for the unconditional verification sweep. */")
    lines.append("    public val ALL: List<LookupTable> = listOf(")
    for table in TABLES:
        lines.append(f"        {table['constant']},")
    lines.append("    )")
    lines.append("}")
    lines.append("")
    return "\n".join(lines)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--check",
        action="store_true",
        help="exit non-zero if the checked-in generated source is not what this generator produces",
    )
    args = parser.parse_args()

    rendered = render()

    if args.check:
        if not OUTPUT.exists():
            print(f"FAIL: {OUTPUT.relative_to(REPO_ROOT)} does not exist", file=sys.stderr)
            return 1
        current = OUTPUT.read_text(encoding="utf-8")
        if current != rendered:
            print(
                f"FAIL: {OUTPUT.relative_to(REPO_ROOT)} does not match the generator output.\n"
                "The table was edited by hand, or the generator changed without regenerating.",
                file=sys.stderr,
            )
            return 1
        print(f"PASS: {OUTPUT.relative_to(REPO_ROOT)} matches the generator output")
        for table in TABLES:
            print(f"  {table['id']}  {canonical_state_hash(table_envelope(table))}")
        return 0

    OUTPUT.write_text(rendered, encoding="utf-8")
    print(f"wrote {OUTPUT.relative_to(REPO_ROOT)}")
    for table in TABLES:
        print(f"  {table['id']}  {canonical_state_hash(table_envelope(table))}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
