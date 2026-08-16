"""Verify the D016-K baseline freeze manifest and its file digests."""
from __future__ import annotations

import hashlib
import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "research/aliveness-spike/evidence/BASELINE_QUALIFICATION_FREEZE.json"


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
    for item in data["files"]:
        path = ROOT / item["path"]
        actual = hashlib.sha256(path.read_bytes()).hexdigest()
        if actual != item["sha256"]:
            raise SystemExit(f"freeze digest mismatch: {item['path']}")
    print(f"BASELINE_FREEZE=PASS files={len(data['files'])}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
