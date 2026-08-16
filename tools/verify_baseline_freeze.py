"""Verify the D016-K baseline freeze manifest and its file digests."""
from __future__ import annotations

import hashlib
import json
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "research/aliveness-spike/evidence/BASELINE_QUALIFICATION_FREEZE.json"
FREEZE_SOURCE = ROOT / "research/aliveness-spike/analysis/src/main/kotlin/com/animusmachinae/dll17/research/aliveness/analysis/BaselineQualificationFreezeV1.kt"


def canonical_text_bytes(path: Path) -> bytes:
    """Return the repository's cross-platform UTF-8/LF freeze representation."""
    text = path.read_bytes().decode("utf-8")
    return text.replace("\r\n", "\n").replace("\r", "\n").encode("utf-8")


def main() -> int:
    data = json.loads(MANIFEST.read_text(encoding="utf-8"))
    expected = {
        "freezeId": "BaselineQualificationFreezeV1",
        "comparator": "ScriptedPetBaselineV1-vs-DegradedScriptedControlV1",
        "protocol": "BaselineQualificationProtocolV1",
        "instrument": "BaselineCompetenceInstrumentV1",
        "presentation": "SpikeExpressionContractV1-blinded-counterbalanced",
        "exclusions": "BaselineQualificationExclusionsV1",
        "analysis": "A001AnalysisV1-paired-t-interval",
    }
    for key, value in expected.items():
        if data.get(key) != value:
            raise SystemExit(f"freeze manifest mismatch: {key}")
    if data.get("canonicalByteRule") != "UTF-8 text with CRLF and CR normalized to LF":
        raise SystemExit("freeze manifest canonical byte rule missing or incorrect")
    for item in data["files"]:
        path = ROOT / item["path"]
        actual = hashlib.sha256(canonical_text_bytes(path)).hexdigest()
        if actual != item["sha256"]:
            raise SystemExit(f"freeze digest mismatch: {item['path']}")
    manifest_hash = hashlib.sha256(canonical_text_bytes(MANIFEST)).hexdigest()
    source = FREEZE_SOURCE.read_text(encoding="utf-8")
    match = re.search(r'MANIFEST_SHA256:\s*String\s*=\s*"([0-9a-f]{64})"', source)
    if not match:
        raise SystemExit("gate manifest hash constant missing")
    if match.group(1) != manifest_hash:
        raise SystemExit("gate manifest hash does not match canonical manifest bytes")
    print(f"MANIFEST_SHA256={manifest_hash}")
    print(f"BASELINE_FREEZE=PASS files={len(data['files'])}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
