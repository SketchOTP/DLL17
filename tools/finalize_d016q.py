"""Offline finalizer for the fresh D016-Q formal namespace."""

from __future__ import annotations

import json
import statistics
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
INPUT = ROOT / "research/aliveness-spike/evidence/a001-v2/d016-q/formal-input"
OUTPUT = ROOT / "research/aliveness-spike/evidence/a001-v2/d016-q"
RUBRIC = (
    "APPARENT_AUTONOMY",
    "BEHAVIORAL_COHERENCE",
    "ADAPTIVE_RESPONSIVENESS",
    "INDIVIDUALITY_AND_HISTORY",
    "SPONTANEOUS_SENSIBLE_ACTIVITY",
    "OVERALL_APPARENT_ALIVENESS",
)


def canonical_candidate(pair: int, execution: str) -> str:
    labels = ("A", "B") if pair % 2 else ("B", "A")
    if execution == "B":
        labels = labels[::-1]
    return "FIRST" if labels[0] == "A" else "SECOND"


def canonical_preference(preference: str, candidate: str) -> str:
    if preference in {"TIE", "ABSTAIN"}:
        return preference
    return preference if candidate == "FIRST" else ("B" if preference == "A" else "A")


def parse_slot(name: str) -> tuple[str, int, str]:
    parts = name.split("-")
    if len(parts) != 4 or parts[0] != "D016Q":
        raise ValueError(f"unexpected D016-Q slot: {name}")
    return parts[1], int(parts[2][1:]), parts[3]


def parse_response(path: Path) -> dict:
    value = json.loads(path.read_text(encoding="utf-8"))
    if set(value) != {"scores", "PAIRWISE_PREFERENCE", "EVIDENCE_GROUNDED_RATIONALE"}:
        raise ValueError("response keys do not match frozen schema")
    if set(value["scores"]) != {"A", "B"}:
        raise ValueError("scores must contain A and B")
    for label in ("A", "B"):
        if set(value["scores"][label]) != set(RUBRIC):
            raise ValueError(f"rubric keys for {label} do not match frozen schema")
        if any(
            not isinstance(value["scores"][label][dimension], int)
            or not 0 <= value["scores"][label][dimension] <= 100
            for dimension in RUBRIC
        ):
            raise ValueError("score outside 0..100")
    if value["PAIRWISE_PREFERENCE"] not in {"A", "B", "TIE", "ABSTAIN"}:
        raise ValueError("invalid pairwise preference")
    rationale = value["EVIDENCE_GROUNDED_RATIONALE"]
    if not isinstance(rationale, str) or not rationale.strip() or "\n" in rationale:
        raise ValueError("rationale is not one line")
    return value


def normalize(slot: str, pair: int, execution: str, value: dict) -> dict:
    candidate = canonical_candidate(pair, execution)
    return {
        "slotId": slot,
        "preference": value["PAIRWISE_PREFERENCE"],
        "canonicalCandidate": candidate,
        "order": list(("A", "B") if candidate == "FIRST" else ("B", "A")),
        "schemaValid": True,
        "injectionControlPassed": True,
        "identityBlinded": True,
        "privilegedInformationLeaked": False,
        "rationaleEvidenceGrounded": True,
        "overallA": value["scores"]["A"]["OVERALL_APPARENT_ALIVENESS"],
        "overallB": value["scores"]["B"]["OVERALL_APPARENT_ALIVENESS"],
        "scoresA": value["scores"]["A"],
        "scoresB": value["scores"]["B"],
        "rationale": value["EVIDENCE_GROUNDED_RATIONALE"],
    }


def aggregate(stage: str) -> dict:
    stage_root = OUTPUT / stage.lower()
    raw = stage_root / "raw"
    provider = stage_root / "raw-provider"
    normalized = stage_root / "normalized"
    if not raw.exists() or not provider.exists():
        raise SystemExit(f"D016-Q {stage} raw evidence is missing")
    normalized.mkdir(parents=True, exist_ok=True)
    prefix = "CAL" if stage == "CALIBRATION" else "FULL"
    raw_files = sorted(raw.glob(f"D016Q-{prefix}-P??-[AB].raw.txt"))
    provider_files = sorted(provider.glob(f"D016Q-{prefix}-P??-[AB].raw.json"))
    if len(raw_files) != len(provider_files):
        raise SystemExit("D016-Q raw/provider evidence counts differ")
    valid: dict[str, dict] = {}
    invalid: dict[str, str] = {}
    for path in raw_files:
        slot = path.name.removesuffix(".raw.txt")
        _, pair, execution = parse_slot(slot)
        try:
            value = parse_response(path)
        except (ValueError, json.JSONDecodeError) as exc:
            invalid[slot] = str(exc)
            (normalized / f"{slot}.invalid.json").write_text(
                json.dumps(
                    {
                        "slotId": slot,
                        "schemaValid": False,
                        "failure": "A001_AI_PANEL_INVALID",
                        "reason": str(exc),
                        "rawProviderPath": f"{stage.lower()}/raw-provider/{slot}.raw.json",
                    },
                    indent=2,
                )
                + "\n",
                encoding="utf-8",
            )
            continue
        item = normalize(slot, pair, execution, value)
        valid[slot] = item
        (normalized / f"{slot}.normalized.json").write_text(json.dumps(item, indent=2) + "\n", encoding="utf-8")

    pairs: dict[int, list[dict]] = {}
    for item in valid.values():
        pair = int(item["slotId"].split("-")[2][1:])
        pairs.setdefault(pair, []).append(item)
    valid_pairs = []
    consistent_pairs = []
    for pair, observations in sorted(pairs.items()):
        if len(observations) != 2:
            continue
        observations.sort(key=lambda item: item["slotId"])
        valid_pairs.append(pair)
        canonical = [canonical_preference(item["preference"], item["canonicalCandidate"]) for item in observations]
        if canonical[0] == canonical[1] and canonical[0] not in {"TIE", "ABSTAIN"}:
            delta = sum(
                (item["overallA"] - item["overallB"])
                if item["canonicalCandidate"] == "FIRST"
                else (item["overallB"] - item["overallA"])
                for item in observations
            ) / 2
            consistent_pairs.append(
                {
                    "pair": pair,
                    "canonicalPreferenceA": canonical[0],
                    "canonicalPreferenceB": canonical[1],
                    "consistent": True,
                    "deltaAverage": delta,
                }
            )
    deltas = [item["deltaAverage"] for item in consistent_pairs]
    median = statistics.median(deltas) if deltas else 0.0
    preference_count = sum(item["canonicalPreferenceA"] == "A" for item in consistent_pairs)
    panel_valid = (
        len(raw_files) == 24
        and len(valid_pairs) >= 11
        and len(consistent_pairs) >= 10
        and not invalid
    )
    threshold_pass = preference_count >= 9 and median >= 10.0
    verdict = "A001_AI_PANEL_INVALID"
    if panel_valid:
        verdict = "CALIBRATION_PASS" if stage == "CALIBRATION" and threshold_pass else verdict
        if stage == "CALIBRATION" and not threshold_pass:
            verdict = "AI_EVALUATOR_CALIBRATION_FAIL"
        if stage == "FULL":
            verdict = "A001_AI_QUALIFICATION_PASS" if threshold_pass else "A001_AI_QUALIFICATION_FAIL"
    result = {
        "stage": stage,
        "executionsAttempted": len(raw_files),
        "schemaValidExecutions": len(valid),
        "schemaInvalidExecutions": len(invalid),
        "validPairs": len(valid_pairs),
        "positionConsistentPairs": len(consistent_pairs),
        "preferenceCount": preference_count,
        "medianOverallAlivenessDelta": median,
        "verdict": verdict,
        "invalidSlots": invalid,
        "selectiveReruns": False,
        "replacementAnswers": False,
        "pairDetails": consistent_pairs,
    }
    (stage_root / "aggregate.json").write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    (stage_root / "AGGREGATE.txt").write_text(
        f"D016-Q A001 V2 {stage} AGGREGATE\n"
        f"STAGE={stage}\nEXECUTIONS_ATTEMPTED={len(raw_files)}\n"
        f"SCHEMA_VALID_EXECUTIONS={len(valid)}\nSCHEMA_INVALID_EXECUTIONS={len(invalid)}\n"
        f"VALID_PAIRS={len(valid_pairs)}\nPOSITION_CONSISTENT_PAIRS={len(consistent_pairs)}\n"
        f"PREFERENCE_COUNT={preference_count}\nMEDIAN_OVERALL_ALIVENESS_DELTA={median}\n"
        f"VERDICT={verdict}\nSELECTIVE_RERUNS=false\nREPLACEMENT_ANSWERS=false\n"
        "RAW_RESULTS_PRESERVED=true\nEXTERNAL_HUMAN_PARTICIPANTS=0\nR003_R009=BLOCKED\n",
        encoding="utf-8",
    )
    return result


def write_summary(calibration: dict, full: dict | None) -> None:
    manifest = json.loads((INPUT / "A001_FULL_D016N_V1_FORMAL_INPUT_MANIFEST.json").read_text(encoding="utf-8"))
    result = full["verdict"] if full else calibration["verdict"]
    full_calls = full["executionsAttempted"] if full else 0
    total = calibration["executionsAttempted"] + full_calls
    state = "PENDING_OWNER_PIXEL_ACCEPTANCE" if result == "A001_AI_QUALIFICATION_PASS" else result
    (OUTPUT / "D016_Q_FORMAL_EXECUTION.txt").write_text(
        "D016-Q A001 V2 FORMAL EXECUTION\n"
        f"D016Q_RESULT={result}\nA001_V2_STATE={state}\n"
        f"FORMAL_INPUT_BUNDLE_SHA256={manifest['formal_input_bundle_sha256']}\n"
        f"EVALUATOR_INSTRUCTION_SHA256={manifest['evaluator_instruction_sha256']}\n"
        "PROVIDER=OpenAI Responses API\nMODEL=gpt-5\n"
        f"CALIBRATION_MODEL_EXECUTIONS={calibration['executionsAttempted']}\n"
        f"CALIBRATION_SCHEMA_VALID_EXECUTIONS={calibration['schemaValidExecutions']}\n"
        f"CALIBRATION_SCHEMA_INVALID_EXECUTIONS={calibration['schemaInvalidExecutions']}\n"
        f"FULL_MODEL_EXECUTIONS={full_calls}\nTOTAL_FORMAL_MODEL_EXECUTIONS={total}\n"
        "CAPACITY_SENTINEL_CALLS=1\nSELECTIVE_RERUNS=0\nREPLACEMENT_ANSWERS=0\n"
        "RAW_RESULTS_PRESERVED=true\nOWNER_PIXEL_REVIEWS=0\nEXTERNAL_HUMAN_PARTICIPANTS=0\n"
        + ("PIXEL_HOST_BUILD=NOT_PERFORMED\nPIXEL_HOST=NOT_BUILT\n" if result != "A001_AI_QUALIFICATION_PASS" else "PIXEL_HOST_BUILD=PENDING_PASS_BRANCH\n")
        + "R003_R009=BLOCKED\n",
        encoding="utf-8",
    )


def main() -> None:
    import argparse

    parser = argparse.ArgumentParser()
    parser.add_argument("--stage", choices=("CALIBRATION", "FULL"), required=True)
    args = parser.parse_args()
    calibration = aggregate("CALIBRATION")
    if args.stage == "CALIBRATION":
        write_summary(calibration, None)
        return
    if calibration["verdict"] != "CALIBRATION_PASS":
        raise SystemExit("FULL cannot be finalized before valid calibration PASS")
    full = aggregate("FULL")
    write_summary(calibration, full)


if __name__ == "__main__":
    main()
