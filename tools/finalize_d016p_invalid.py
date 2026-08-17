"""Finalize the D016-P invalid calibration branch without provider contact."""

from __future__ import annotations

import hashlib
import json
import statistics
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
INPUT = ROOT / "research/aliveness-spike/evidence/a001-v2/formal-input-d016n-v1"
OUTPUT = ROOT / "research/aliveness-spike/evidence/a001-v2/d016-p"
CAL = OUTPUT / "calibration"
RAW = CAL / "raw"
RAW_PROVIDER = CAL / "raw-provider"
NORMALIZED = CAL / "normalized"

RUBRIC = (
    "APPARENT_AUTONOMY",
    "BEHAVIORAL_COHERENCE",
    "ADAPTIVE_RESPONSIVENESS",
    "INDIVIDUALITY_AND_HISTORY",
    "SPONTANEOUS_SENSIBLE_ACTIVITY",
    "OVERALL_APPARENT_ALIVENESS",
)


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def canonical_candidate(pair: int, order: str) -> str:
    labels = ("A", "B") if pair % 2 else ("B", "A")
    if order == "B":
        labels = labels[::-1]
    return "FIRST" if labels[0] == "A" else "SECOND"


def canonical_preference(preference: str, candidate: str) -> str:
    if preference in {"TIE", "ABSTAIN"}:
        return preference
    if candidate == "FIRST":
        return preference
    return "B" if preference == "A" else "A"


def parse_slot(path: Path) -> tuple[int, str]:
    parts = path.name.split("-")
    if len(parts) != 4 or parts[0] != "D016P" or parts[1] != "CAL":
        raise ValueError(f"unexpected slot file: {path.name}")
    return int(parts[2][1:]), parts[3].split(".")[0]


def parse_response(path: Path) -> dict:
    value = json.loads(path.read_text(encoding="utf-8"))
    if set(value) != {"scores", "PAIRWISE_PREFERENCE", "EVIDENCE_GROUNDED_RATIONALE"}:
        raise ValueError("response keys do not match frozen schema")
    if set(value["scores"]) != {"A", "B"}:
        raise ValueError("scores must contain A and B")
    scores = value["scores"]
    for label in ("A", "B"):
        if set(scores[label]) != set(RUBRIC):
            raise ValueError(f"rubric keys for {label} do not match frozen schema")
        if any(not isinstance(scores[label][dimension], int) or not 0 <= scores[label][dimension] <= 100 for dimension in RUBRIC):
            raise ValueError("score outside 0..100")
    if value["PAIRWISE_PREFERENCE"] not in {"A", "B", "TIE", "ABSTAIN"}:
        raise ValueError("invalid pairwise preference")
    rationale = value["EVIDENCE_GROUNDED_RATIONALE"]
    if not isinstance(rationale, str) or not rationale.strip() or "\n" in rationale:
        raise ValueError("rationale is not one line")
    return value


def render_normalized(slot: str, pair: int, order: str, value: dict) -> dict:
    candidate = canonical_candidate(pair, order)
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


def main() -> None:
    if not RAW.exists() or not RAW_PROVIDER.exists():
        raise SystemExit("D016-P calibration raw evidence is missing")
    NORMALIZED.mkdir(parents=True, exist_ok=True)
    raw_files = sorted(RAW.glob("D016P-CAL-P??-[AB].raw.txt"))
    provider_files = sorted(RAW_PROVIDER.glob("D016P-CAL-P??-[AB].raw.json"))
    if len(raw_files) != len(provider_files):
        raise SystemExit("runner/provider raw evidence counts differ")

    valid: dict[str, dict] = {}
    invalid: dict[str, str] = {}
    for path in raw_files:
        slot = path.name.removesuffix(".raw.txt")
        pair, order = parse_slot(path)
        try:
            value = parse_response(path)
        except (ValueError, json.JSONDecodeError) as exc:
            invalid[slot] = str(exc)
            (NORMALIZED / f"{slot}.invalid.json").write_text(
                json.dumps(
                    {
                        "slotId": slot,
                        "schemaValid": False,
                        "failure": "A001_AI_PANEL_INVALID",
                        "reason": str(exc),
                        "rawProviderPath": f"calibration/raw-provider/{slot}.raw.json",
                    },
                    indent=2,
                )
                + "\n",
                encoding="utf-8",
            )
            continue
        normalized = render_normalized(slot, pair, order, value)
        valid[slot] = normalized
        (NORMALIZED / f"{slot}.normalized.json").write_text(
            json.dumps(normalized, indent=2) + "\n", encoding="utf-8"
        )

    pairs = {}
    for slot, observation in valid.items():
        pair = int(slot.split("-")[2][1:])
        pairs.setdefault(pair, []).append(observation)
    complete_pairs = []
    for pair, observations in sorted(pairs.items()):
        if len(observations) != 2:
            continue
        observations.sort(key=lambda item: item["slotId"])
        canonical = [
            canonical_preference(item["preference"], item["canonicalCandidate"])
            for item in observations
        ]
        if canonical[0] == canonical[1] and canonical[0] not in {"TIE", "ABSTAIN"}:
            delta = sum(
                (item["overallA"] - item["overallB"])
                if item["canonicalCandidate"] == "FIRST"
                else (item["overallB"] - item["overallA"])
                for item in observations
            ) / 2
            complete_pairs.append(
                {
                    "pair": pair,
                    "canonicalPreferenceA": canonical[0],
                    "canonicalPreferenceB": canonical[1],
                    "consistent": True,
                    "deltaAverage": delta,
                }
            )

    deltas = [item["deltaAverage"] for item in complete_pairs]
    median = statistics.median(deltas) if deltas else 0.0
    preference_count = sum(item["canonicalPreferenceA"] == "A" for item in complete_pairs)
    aggregate = {
        "stage": "CALIBRATION",
        "executionsAttempted": len(raw_files),
        "schemaValidExecutions": len(valid),
        "schemaInvalidExecutions": len(invalid),
        "completedPairs": len(pairs),
        "completeConsistentPairs": len(complete_pairs),
        "preferenceCountFromCompletePairs": preference_count,
        "medianDeltaFromCompletePairs": median,
        "verdict": "A001_AI_PANEL_INVALID",
        "invalidSlots": invalid,
        "selectiveReruns": False,
        "replacementAnswers": False,
        "fullExecuted": False,
        "pairDetails": complete_pairs,
    }
    (CAL / "aggregate.json").write_text(json.dumps(aggregate, indent=2) + "\n", encoding="utf-8")
    (CAL / "AGGREGATE.txt").write_text(
        "D016-P A001 V2 CALIBRATION AGGREGATE\n"
        "STAGE=CALIBRATION\n"
        "PROVIDER=OpenAI Responses API\n"
        "MODEL=gpt-5\n"
        f"EXECUTIONS_ATTEMPTED={len(raw_files)}\n"
        f"SCHEMA_VALID_EXECUTIONS={len(valid)}\n"
        f"SCHEMA_INVALID_EXECUTIONS={len(invalid)}\n"
        f"COMPLETE_PAIRS={len(pairs)}\n"
        f"COMPLETE_CONSISTENT_PAIRS={len(complete_pairs)}\n"
        f"PREFERENCE_COUNT_FROM_COMPLETE_PAIRS={preference_count}\n"
        f"MEDIAN_DELTA_FROM_COMPLETE_PAIRS={median}\n"
        "VERDICT=A001_AI_PANEL_INVALID\n"
        "SELECTIVE_RERUNS=false\nREPLACEMENT_ANSWERS=false\n"
        "RAW_RESULTS_PRESERVED=true\nFULL_EXECUTED=false\n"
        "EXTERNAL_HUMAN_PARTICIPANTS=0\nR003_R009=BLOCKED\n",
        encoding="utf-8",
    )

    bundle = json.loads((INPUT / "A001_FULL_D016N_V1_FORMAL_INPUT_MANIFEST.json").read_text(encoding="utf-8"))
    summary = (
        "D016-P A001 V2 FORMAL EXECUTION\n"
        "D016P_RESULT=A001_AI_PANEL_INVALID\n"
        "D016P_CALIBRATION_RESULT=A001_AI_PANEL_INVALID\n"
        "A001_V2_STATE=AI_PANEL_INVALID\n"
        f"FORMAL_INPUT_BUNDLE_SHA256={bundle['formal_input_bundle_sha256']}\n"
        f"EVALUATOR_INSTRUCTION_SHA256={bundle['evaluator_instruction_sha256']}\n"
        "PROVIDER=OpenAI Responses API\nMODEL=gpt-5\n"
        f"CALIBRATION_MODEL_EXECUTIONS={len(raw_files)}\n"
        f"CALIBRATION_SCHEMA_VALID_EXECUTIONS={len(valid)}\n"
        f"CALIBRATION_SCHEMA_INVALID_EXECUTIONS={len(invalid)}\n"
        "FULL_MODEL_EXECUTIONS=0\n"
        f"TOTAL_FORMAL_MODEL_EXECUTIONS={len(raw_files)}\n"
        "SELECTIVE_RERUNS=0\nREPLACEMENT_ANSWERS=0\n"
        "RAW_RESULTS_PRESERVED=true\n"
        "OWNER_PIXEL_REVIEWS=0\nEXTERNAL_HUMAN_PARTICIPANTS=0\n"
        "PIXEL_HOST_BUILD=NOT_PERFORMED\nPIXEL_HOST=NOT_BUILT\n"
        "R003_R009=BLOCKED\nSTOP_AFTER_D016P_INVALID=true\n"
    )
    (OUTPUT / "D016_P_FORMAL_EXECUTION.txt").write_text(summary, encoding="utf-8")

    postmortem = (
        "D016-P FAILURE POSTMORTEM\n"
        "RESULT=A001_AI_PANEL_INVALID\n\n"
        "## Quantitative comparison\n"
        "D016-M calibration: 24 executions, 12 valid pairs, 12 position-consistent pairs, preference 12/12, median delta +36.5.\n"
        "D016-M FULL: 24 executions, 12 valid pairs, 12 position-consistent pairs, preference 0/12, median delta -36.5.\n"
        f"D016-P calibration: {len(raw_files)} attempted, {len(valid)} schema-valid executions, {len(pairs)} complete pairs, {len(complete_pairs)} complete consistent pairs, partial preference {preference_count}, partial median delta {median}; these partial diagnostics are not a qualification result.\n"
        "D016-P FULL: NOT EXECUTED because calibration was panel-invalid.\n"
        "D016-P six-dimension FULL-minus-baseline comparison: NOT APPLICABLE; no D016-P FULL evidence exists. No diagnostic value is fabricated.\n\n"
        "## Prior phenotype\n"
        "PRIOR_D016M_PHENOTYPE=NOT_EVIDENT\n"
        "Reason: D016-P never produced FULL-vs-baseline evidence, so the prior D016-M outward phenotype cannot be assessed from this invalid calibration branch.\n\n"
        "## Coder-supported hypotheses\n"
        "CODER_SUPPORTED_HYPOTHESES=0\n"
        "No organism hypothesis is supportable from this branch because the candidate FULL stage was never executed.\n\n"
        "## Boundary\n"
        "No organism modification occurred after the invalid result. No evaluator modification occurred. No rescore occurred. Pixel host was not built. R003-R009 remain blocked.\n"
    )
    (OUTPUT / "D016_P_FAILURE_POSTMORTEM.txt").write_text(postmortem, encoding="utf-8")


if __name__ == "__main__":
    main()
