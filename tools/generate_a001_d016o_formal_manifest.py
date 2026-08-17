"""Generate and verify the D016-O A001 V2 formal-input freeze.

This verifier is deliberately offline. The observation bundles are produced by
the accepted A001EvaluatorObservationGeneratorV1 at the bound D016-N commit;
this script binds those bytes, the candidate source, and the unchanged
evaluation inputs into a new versioned manifest. It never contacts a model or
any network endpoint.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import subprocess
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
OLD_INPUT = ROOT / "research/aliveness-spike/evidence/a001-v2/formal-input"
INPUT = ROOT / "research/aliveness-spike/evidence/a001-v2/formal-input-d016n-v1"
OUT = INPUT / "A001_FULL_D016N_V1_FORMAL_INPUT_MANIFEST.json"

CANDIDATE_ID = "A001_FULL_D016N_V1"
CANDIDATE_SHA = "684579130bef5c820f3db9534ffb744654ebf3b4"
OLD_BUNDLE_SHA = "dc2e4b40735ba0ba1ca35758ac5f0ef9a034ec95d1d19b8e5b6f8d0f97f3e7ab"
OLD_MANIFEST_FILE_SHA = "2344ce5e1b371e8b10d529de4ae4c706ab63d43e535b19da99f1dd5baab2e97b"
EXPECTED_INSTRUCTION_SHA = "92147a2ade86db8d602b991b8bbd4099e16f008d1fab9b0f84e15652c6a568a4"

EVALUATION_SOURCE_FILES = [
    "research/aliveness-spike/agentic-review/src/main/kotlin/com/animusmachinae/dll17/research/aliveness/agentic/A001EvaluationContractV2.kt",
    "research/aliveness-spike/agentic-review/src/main/kotlin/com/animusmachinae/dll17/research/aliveness/agentic/AgentObservationHarnessV1.kt",
    "research/aliveness-spike/agentic-review/src/main/kotlin/com/animusmachinae/dll17/research/aliveness/agentic/A001V2Aggregation.kt",
    "research/aliveness-spike/agentic-review/src/main/kotlin/com/animusmachinae/dll17/research/aliveness/agentic/A001V2FormalExecutionRunner.kt",
    "research/aliveness-spike/realtime-viewer/src/main/kotlin/com/animusmachinae/dll17/research/aliveness/viewer/A001EvaluatorObservationGeneratorV1.kt",
    "research/aliveness-spike/study-protocol/A001ObservationProtocolV1.md",
    "research/aliveness-spike/evidence/a001-v2/formal-input-d016n-v1/EVALUATOR_INSTRUCTIONS_V1.txt",
]

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


def file_records(paths: list[Path]) -> list[dict[str, str]]:
    return [{"path": rel(path), "sha256": sha256_file(path)} for path in sorted(paths)]


def bundle_records(directory: Path, prefix: str) -> list[dict[str, str]]:
    records = []
    for path in sorted(directory.glob("*.txt")):
        data = path.read_bytes()
        text = data.decode("utf-8")
        lowered = text.lower()
        leaked = [word for word in FORBIDDEN if word.lower() in lowered]
        if leaked:
            raise SystemExit(f"identity leakage in {rel(path)}: {leaked}")
        if not text.startswith("A001_OBSERVATION_BUNDLE_V1\n"):
            raise SystemExit(f"invalid observation bundle: {rel(path)}")
        if "CREATURE_A\n" not in text or "CREATURE_B\n" not in text:
            raise SystemExit(f"neutral labels missing: {rel(path)}")
        if not text.endswith("END_BUNDLE\n"):
            raise SystemExit(f"bundle is incomplete: {rel(path)}")
        if not path.name.startswith(prefix):
            raise SystemExit(f"unexpected bundle name: {rel(path)}")
        records.append({"path": rel(path), "sha256": sha256_bytes(data)})
    if len(records) != 12:
        raise SystemExit(f"expected 12 bundles in {rel(directory)}, found {len(records)}")
    return records


def compare_records(old: list[dict[str, str]], new: list[dict[str, str]]) -> tuple[bool, bool]:
    old_map = {item["path"].split("/formal-input/", 1)[1]: item["sha256"] for item in old}
    new_map = {item["path"].split("/formal-input-d016n-v1/", 1)[1]: item["sha256"] for item in new}
    if set(old_map) != set(new_map):
        raise SystemExit("bundle comparison paths do not match")
    return all(old_map[name] == new_map[name] for name in old_map), any(
        old_map[name] != new_map[name] for name in old_map
    )


def candidate_source_paths() -> list[Path]:
    cohort_root = ROOT / "research/aliveness-spike/cohorts/src/main/kotlin"
    return [
        *cohort_root.rglob("*.kt"),
        ROOT / "research/aliveness-spike/cohorts/build.gradle.kts",
        ROOT / "research/aliveness-spike/realtime-viewer/src/main/kotlin/com/animusmachinae/dll17/research/aliveness/viewer/A001EvaluatorObservationGeneratorV1.kt",
        ROOT / "research/aliveness-spike/realtime-viewer/src/main/kotlin/com/animusmachinae/dll17/research/aliveness/viewer/ViewerSession.kt",
        ROOT / "research/aliveness-spike/realtime-viewer/build.gradle.kts",
    ]


def git_object_exists() -> bool:
    result = subprocess.run(
        ["git", "cat-file", "-e", f"{CANDIDATE_SHA}^{{commit}}"],
        cwd=ROOT,
        capture_output=True,
        text=True,
        check=False,
    )
    return result.returncode == 0


def make_manifest() -> dict:
    old_manifest = OLD_INPUT / "A001_V2_FORMAL_INPUT_MANIFEST.json"
    old_manifest_data = json.loads(old_manifest.read_text(encoding="utf-8"))
    old_calibration = bundle_records(OLD_INPUT / "calibration", "CAL-P")
    old_qualification = bundle_records(OLD_INPUT / "qualification", "FULL-P")
    calibration = bundle_records(INPUT / "calibration", "CAL-P")
    qualification = bundle_records(INPUT / "qualification", "FULL-P")
    calibration_same, _ = compare_records(old_calibration, calibration)
    qualification_same, qualification_differs = compare_records(old_qualification, qualification)

    if old_manifest_data.get("formal_input_bundle_sha256") != OLD_BUNDLE_SHA:
        raise SystemExit("D016-M bundle hash changed")
    if sha256_file(old_manifest) != OLD_MANIFEST_FILE_SHA:
        raise SystemExit("D016-M manifest file changed")

    instruction = INPUT / "EVALUATOR_INSTRUCTIONS_V1.txt"
    instruction_sha = sha256_file(instruction)
    if instruction_sha != EXPECTED_INSTRUCTION_SHA:
        raise SystemExit("evaluator instruction hash changed")
    if not calibration_same:
        raise SystemExit("BLOCKED_CALIBRATION_INPUT_DRIFT")
    if not qualification_differs or qualification_same:
        raise SystemExit("BLOCKED_MATERIAL_CANDIDATE_NOT_REFLECTED")

    material = {
        "manifest_id": "A001_FULL_D016N_V1_FORMAL_INPUT_MANIFEST",
        "manifest_version": 1,
        "candidate_identity": CANDIDATE_ID,
        "candidate_git_sha": CANDIDATE_SHA,
        "contract_id": "A001EvaluationContractV2",
        "contract_version": 2,
        "harness_id": "AgentObservationHarnessV1",
        "protocol_id": "A001ObservationProtocolV1",
        "generator_id": "A001EvaluatorObservationGeneratorV1",
        "generator_version": 1,
        "aggregator_id": "A001V2Aggregation",
        "provider": "OpenAI Responses API",
        "model": "gpt-5",
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
        "observation_case_count_per_stage": 12,
        "observation_ticks": 96,
        "tick_millis": 250,
        "seed_base": 20260816,
        "seed_step": 7919,
        "min_schema_valid_pairs": 11,
        "min_position_consistent_pairs": 10,
        "min_preference_pairs": 9,
        "min_median_overall_aliveness_delta": 10.0,
        "panel_plan": "counterbalanced-A-B/B-A; fresh isolated requests",
        "output_schema": "A001V2EvaluatorResponseV1",
        "evaluator_instruction_sha256": instruction_sha,
        "old_d016_m_manifest_bundle_sha256": OLD_BUNDLE_SHA,
        "old_d016_m_manifest_file_sha256": OLD_MANIFEST_FILE_SHA,
        "calibration_matches_d016_m": calibration_same,
        "revised_full_differs_from_d016_m": qualification_differs,
        "evaluation_source_files": file_records([ROOT / path for path in EVALUATION_SOURCE_FILES]),
        "candidate_source_files": file_records(candidate_source_paths()),
        "calibration_bundles": calibration,
        "qualification_bundles": qualification,
    }
    canonical = json.dumps(material, ensure_ascii=False, separators=(",", ":"), sort_keys=True).encode("utf-8")
    material["formal_input_bundle_sha256"] = sha256_bytes(canonical)
    return material


def render_preflight(manifest: dict) -> str:
    return (
        "D016_O_FORMAL_PREFLIGHT=PASS\n"
        f"FORMAL_CANDIDATE={manifest['candidate_identity']}\n"
        f"CANDIDATE_GIT_SHA={manifest['candidate_git_sha']}\n"
        "CALIBRATION_CASES=12\n"
        "QUALIFICATION_CASES=12\n"
        f"CALIBRATION_MATCHES_D016_M={str(manifest['calibration_matches_d016_m']).lower()}\n"
        f"REVISED_FULL_DIFFERS_FROM_D016_M={str(manifest['revised_full_differs_from_d016_m']).lower()}\n"
        f"FORMAL_INPUT_BUNDLE_SHA256={manifest['formal_input_bundle_sha256']}\n"
        f"EVALUATOR_INSTRUCTION_SHA256={manifest['evaluator_instruction_sha256']}\n"
        "OBSERVATION_PROTOCOL=A001ObservationProtocolV1\n"
        "PANEL_PAIRS=12\n"
        "FORMAL_EXECUTIONS_PER_STAGE=24\n"
        "AI_FORMAL_EXECUTIONS=0\n"
        "NETWORK_MODEL_CALLS=0\n"
        "OWNER_PIXEL_REVIEWS=0\n"
        "EXTERNAL_HUMAN_PARTICIPANTS=0\n"
        "R003_R009=BLOCKED\n"
        "FORMAL_RUNNER_READY=true\n"
    )


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    if not git_object_exists():
        raise SystemExit(f"candidate commit is not present: {CANDIDATE_SHA}")
    manifest = make_manifest()
    rendered = json.dumps(manifest, ensure_ascii=False, indent=2) + "\n"
    preflight = render_preflight(manifest)
    preflight_path = INPUT / "D016_O_FORMAL_PREFLIGHT.txt"
    if args.check:
        if not OUT.exists() or OUT.read_text(encoding="utf-8") != rendered:
            print("D016_O_FORMAL_MANIFEST=DIFF")
            return 1
        if not preflight_path.exists() or preflight_path.read_text(encoding="utf-8") != preflight:
            print("D016_O_FORMAL_PREFLIGHT=DIFF")
            return 1
        print(preflight, end="")
        return 0
    INPUT.mkdir(parents=True, exist_ok=True)
    OUT.write_text(rendered, encoding="utf-8", newline="\n")
    preflight_path.write_text(preflight, encoding="utf-8", newline="\n")
    print(preflight, end="")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
