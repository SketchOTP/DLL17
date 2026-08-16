"""Generate and verify the D016-M-R1 formal-input manifest.

This script is deliberately offline. It hashes already-generated observation
bundles and frozen source/instruction files; it never contacts a model.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
INPUT = ROOT / "research/aliveness-spike/evidence/a001-v2/formal-input"
OUT = INPUT / "A001_V2_FORMAL_INPUT_MANIFEST.json"
FORBIDDEN = (
    "FULL",
    "ScriptedPetBaselineV1",
    "DegradedScriptedControlV1",
    "baseline",
    "degraded",
    "learning",
    "scripted",
    "agentic",
    "candidate type",
    "source path",
    "repository path",
)


def sha256_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def sha256_file(path: Path) -> str:
    return sha256_bytes(path.read_bytes())


def rel(path: Path) -> str:
    return path.relative_to(ROOT).as_posix()


def bundle_records(directory: Path) -> list[dict[str, str]]:
    records = []
    for path in sorted(directory.glob("*.txt")):
        data = path.read_bytes()
        text = data.decode("utf-8")
        lowered = text.lower()
        leaked = [word for word in FORBIDDEN if word.lower() in lowered]
        if leaked:
            raise SystemExit(f"identity leakage in {rel(path)}: {leaked}")
        if "CREATURE_A\n" not in text or "CREATURE_B\n" not in text:
            raise SystemExit(f"invalid neutral bundle: {rel(path)}")
        records.append({"path": rel(path), "sha256": sha256_bytes(data)})
    if len(records) != 12:
        raise SystemExit(f"expected 12 bundles in {directory}, found {len(records)}")
    return records


def make_manifest() -> dict:
    source_files = [
        "research/aliveness-spike/agentic-review/src/main/kotlin/com/animusmachinae/dll17/research/aliveness/agentic/A001EvaluationContractV2.kt",
        "research/aliveness-spike/agentic-review/src/main/kotlin/com/animusmachinae/dll17/research/aliveness/agentic/AgentObservationHarnessV1.kt",
        "research/aliveness-spike/agentic-review/src/main/kotlin/com/animusmachinae/dll17/research/aliveness/agentic/A001V2Aggregation.kt",
        "research/aliveness-spike/agentic-review/src/main/kotlin/com/animusmachinae/dll17/research/aliveness/agentic/A001V2FormalExecutionRunner.kt",
        "research/aliveness-spike/realtime-viewer/src/main/kotlin/com/animusmachinae/dll17/research/aliveness/viewer/A001EvaluatorObservationGeneratorV1.kt",
        "research/aliveness-spike/study-protocol/A001ObservationProtocolV1.md",
        "research/aliveness-spike/evidence/a001-v2/formal-input/EVALUATOR_INSTRUCTIONS_V1.txt",
    ]
    files = [{"path": path, "sha256": sha256_file(ROOT / path)} for path in source_files]
    calibration = bundle_records(INPUT / "calibration")
    qualification = bundle_records(INPUT / "qualification")
    material = {
        "manifest_id": "A001_V2_FORMAL_INPUT_MANIFEST",
        "manifest_version": 1,
        "contract_id": "A001EvaluationContractV2",
        "contract_version": 2,
        "harness_id": "AgentObservationHarnessV1",
        "protocol_id": "A001ObservationProtocolV1",
        "generator_id": "A001EvaluatorObservationGeneratorV1",
        "generator_version": 1,
        "rubric": [
            "APPARENT_AUTONOMY",
            "BEHAVIORAL_COHERENCE",
            "ADAPTIVE_RESPONSIVENESS",
            "INDIVIDUALITY_AND_HISTORY",
            "SPONTANEOUS_SENSIBLE_ACTIVITY",
            "OVERALL_APPARENT_ALIVENESS",
        ],
        "panel_pairs": 12,
        "formal_executions_per_stage": 24,
        "panel_plan": "counterbalanced-A-B/B-A; fresh isolated requests",
        "output_schema": "A001V2EvaluatorResponseV1",
        "evaluator_instruction_sha256": sha256_file(INPUT / "EVALUATOR_INSTRUCTIONS_V1.txt"),
        "source_files": files,
        "calibration_bundles": calibration,
        "qualification_bundles": qualification,
    }
    canonical = json.dumps(material, ensure_ascii=False, separators=(",", ":"), sort_keys=True).encode("utf-8")
    material["formal_input_bundle_sha256"] = sha256_bytes(canonical)
    return material


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    manifest = make_manifest()
    rendered = json.dumps(manifest, ensure_ascii=False, indent=2) + "\n"
    if args.check:
        if not OUT.exists() or OUT.read_text(encoding="utf-8") != rendered:
            print("FORMAL_INPUT_MANIFEST=DIFF")
            return 1
        print(f"FORMAL_INPUT_MANIFEST=BYTE_MATCH sha256={manifest['formal_input_bundle_sha256']}")
        return 0
    OUT.write_text(rendered, encoding="utf-8", newline="\n")
    print(f"FORMAL_INPUT_MANIFEST=GENERATED sha256={manifest['formal_input_bundle_sha256']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
