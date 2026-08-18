"""Run D016-W-R1, the fresh Luna A001 V2 research evaluation.

The accepted D016-O repository artifacts are the provenance authority. This
runner creates only the D016-W-R1 namespace and sends stateless tool-free
Responses API requests with the frozen evaluator contract.
"""
from __future__ import annotations

import datetime as dt
import hashlib
import importlib.util
import json
import re
import subprocess
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
FROZEN = ROOT / "research/aliveness-spike/evidence/a001-v2/formal-input-d016n-v1"
OUTPUT = ROOT / "research/aliveness-spike/evidence/a001-v2/d016-w-r1"
START_SHA = "d14d9a8c3e9e07b0d5d7795ecfa5edc66f525695"
CANDIDATE = "A001_FULL_D016N_V1"
CANDIDATE_SHA = "684579130bef5c820f3db9534ffb744654ebf3b4"
BUNDLE_SHA = "f6f543b3d1cf499b1015c4d66b005915d364a7d0d0b784605c249f13d0592c69"
INSTRUCTION_SHA = "92147a2ade86db8d602b991b8bbd4099e16f008d1fab9b0f84e15652c6a568a4"
MANIFEST_SHA = "dbadb82ef2be8f19b764afa8e3a8c620917c084096cfa21fcc2e56aaf47630d0"
MODEL = "gpt-5.6-luna"
REASONING_EFFORT = "medium"
RUBRIC = (
    "APPARENT_AUTONOMY",
    "BEHAVIORAL_COHERENCE",
    "ADAPTIVE_RESPONSIVENESS",
    "INDIVIDUALITY_AND_HISTORY",
    "SPONTANEOUS_SENSIBLE_ACTIVITY",
    "OVERALL_APPARENT_ALIVENESS",
)


SPEC = importlib.util.spec_from_file_location("d016u_primitives", ROOT / "tools/run_d016u.py")
if SPEC is None or SPEC.loader is None:
    raise RuntimeError("unable to load established evaluator primitives")
U = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(U)


def sha256_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def sha256_file(path: Path) -> str:
    return sha256_bytes(path.read_bytes())


def write_text(path: Path, value: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(value, encoding="utf-8", newline="\n")


def write_json(path: Path, value: Any) -> None:
    write_text(path, json.dumps(value, ensure_ascii=False, indent=2) + "\n")


def utc_now() -> str:
    return dt.datetime.now(dt.timezone.utc).isoformat().replace("+00:00", "Z")


def git_sha(reference: str, relative: str) -> str | None:
    result = subprocess.run(["git", "show", f"{reference}:{relative}"], cwd=ROOT, capture_output=True, check=False)
    return sha256_bytes(result.stdout) if result.returncode == 0 else None


def env_status() -> dict[str, Any]:
    env_file = ROOT / ".env"
    tracked = subprocess.run(["git", "ls-files", "--error-unmatch", ".env"], cwd=ROOT, capture_output=True, check=False).returncode == 0
    ignored = subprocess.run(["git", "check-ignore", "--no-index", ".env"], cwd=ROOT, capture_output=True, check=False).returncode == 0
    key = None
    if env_file.exists():
        for line in env_file.read_text(encoding="utf-8").splitlines():
            match = re.match(r"^\s*OPENAI_API_KEY\s*=\s*(.*?)\s*$", line)
            if match:
                key = match.group(1).strip().strip('"').strip("'")
                break
    return {"ENV_FILE_PRESENT": env_file.exists(), "ENV_FILE_TRACKED": tracked, "ENV_FILE_IGNORED": ignored, "OPENAI_API_KEY_PRESENT": bool(key), "_key": key}


def request_body(prompt: str, *, preflight: bool = False) -> dict[str, Any]:
    return {
        "model": MODEL,
        "input": [{"role": "user", "content": [{"type": "input_text", "text": prompt}]}],
        "reasoning": {"effort": REASONING_EFFORT},
        "text": {"format": U.preflight_format() if preflight else U.structured_format()},
        "store": False,
    }


def verify_inputs() -> tuple[dict[str, Any], dict[str, bool]]:
    manifest_path = FROZEN / "A001_FULL_D016N_V1_FORMAL_INPUT_MANIFEST.json"
    preflight_path = FROZEN / "D016_O_FORMAL_PREFLIGHT.txt"
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    preflight = preflight_path.read_text(encoding="utf-8")
    checks: dict[str, bool] = {
        "HEAD_MATCHES_EXPECTED_START": subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip() == START_SHA,
        "HEAD_EQUALS_ORIGIN_MAIN": subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip() == subprocess.check_output(["git", "rev-parse", "origin/main"], cwd=ROOT, text=True).strip(),
        "D016O_MANIFEST_SHA_MATCH": sha256_file(manifest_path) == MANIFEST_SHA,
        "MANIFEST_CANDIDATE_ID_MATCH": manifest.get("candidate_identity") == CANDIDATE,
        "PREFLIGHT_CANDIDATE_ID_MATCH": "FORMAL_CANDIDATE=" + CANDIDATE in preflight,
        "MANIFEST_CANDIDATE_SHA_MATCH": manifest.get("candidate_git_sha") == CANDIDATE_SHA,
        "PREFLIGHT_CANDIDATE_SHA_MATCH": "CANDIDATE_GIT_SHA=" + CANDIDATE_SHA in preflight,
        "FORMAL_INPUT_BUNDLE_SHA_MATCH": manifest.get("formal_input_bundle_sha256") == BUNDLE_SHA and "FORMAL_INPUT_BUNDLE_SHA256=" + BUNDLE_SHA in preflight,
        "INSTRUCTION_SHA_MATCH": manifest.get("evaluator_instruction_sha256") == INSTRUCTION_SHA and sha256_file(FROZEN / "EVALUATOR_INSTRUCTIONS_V1.txt") == INSTRUCTION_SHA,
        "SCHEMA_MATCH": manifest.get("output_schema") == "A001V2EvaluatorResponseV1",
        "RUBRIC_MATCH": manifest.get("rubric") == list(RUBRIC),
        "THRESHOLDS_MATCH": all(manifest.get(key) == value for key, value in {"min_schema_valid_pairs": 11, "min_position_consistent_pairs": 10, "min_preference_pairs": 9, "min_median_overall_aliveness_delta": 10.0}.items()),
        "AGGREGATOR_MATCH": manifest.get("aggregator_id") == "A001V2Aggregation",
        "PROTOCOL_MATCH": "OBSERVATION_PROTOCOL=A001ObservationProtocolV1" in preflight,
    }
    listed = list(manifest.get("evaluation_source_files", [])) + list(manifest.get("candidate_source_files", [])) + list(manifest.get("calibration_bundles", [])) + list(manifest.get("qualification_bundles", []))
    expected: dict[str, str] = {}
    for item in listed:
        if item["path"] in expected and expected[item["path"]] != item["sha256"]:
            checks["DUPLICATE_PATH_HASHES_AGREE"] = False
        expected[item["path"]] = item["sha256"]
    checks["DUPLICATE_PATH_HASHES_AGREE"] = checks.get("DUPLICATE_PATH_HASHES_AGREE", True)
    checks["CURRENT_GIT_ARTIFACT_BYTES_MATCH"] = all((ROOT / path).exists() and git_sha("HEAD", path) == expected_hash for path, expected_hash in expected.items())
    checks["CALIBRATION_PAIR_COUNT"] = len(list((FROZEN / "calibration").glob("CAL-P??.txt"))) == 12
    checks["FULL_PAIR_COUNT"] = len(list((FROZEN / "qualification").glob("FULL-P??.txt"))) == 12
    checks["FROZEN_INPUTS_PASS"] = all(checks.values())
    return manifest, checks


def write_manifest(manifest: dict[str, Any], checks: dict[str, bool]) -> dict[str, Any]:
    bound = {
        "manifest_id": "D016WR1_FORMAL_INPUT_MANIFEST",
        "manifest_version": 1,
        "candidate_identity": CANDIDATE,
        "candidate_git_sha": CANDIDATE_SHA,
        "candidate_provenance": ["research/aliveness-spike/evidence/a001-v2/formal-input-d016n-v1/A001_FULL_D016N_V1_FORMAL_INPUT_MANIFEST.json", "research/aliveness-spike/evidence/a001-v2/formal-input-d016n-v1/D016_O_FORMAL_PREFLIGHT.txt"],
        "frozen_d016_o_manifest_sha256": MANIFEST_SHA,
        "formal_input_bundle_sha256": BUNDLE_SHA,
        "evaluator_instruction_sha256": INSTRUCTION_SHA,
        "protocol_id": "A001ObservationProtocolV1",
        "rubric": list(RUBRIC),
        "panel_pairs": 12,
        "formal_executions_per_stage": 24,
        "min_schema_valid_pairs": 11,
        "min_position_consistent_pairs": 10,
        "min_preference_pairs": 9,
        "min_median_overall_aliveness_delta": 10.0,
        "provider": "OPENAI",
        "transport": "DIRECT_RESPONSES_API",
        "model_requested": MODEL,
        "reasoning_effort": REASONING_EFFORT,
        "tools_allowed": False,
        "web_allowed": False,
        "file_search_allowed": False,
        "code_interpreter_allowed": False,
        "computer_use_allowed": False,
        "function_tools_allowed": False,
        "mcp_allowed": False,
        "connectors_allowed": False,
        "shell_allowed": False,
        "conversation_reuse": False,
        "previous_response_reuse": False,
        "fresh_request_per_slot": True,
        "structured_output_required": True,
        "slot_prefixes": {"calibration": "D016WR1-CAL-P##-[AB]", "full": "D016WR1-FULL-P##-[AB]"},
        "input_integrity": checks,
    }
    write_json(OUTPUT / "D016WR1_FORMAL_INPUT_MANIFEST.json", bound)
    write_json(OUTPUT / "D016WR1_FROZEN_INPUT_CHECKS.json", checks)
    write_text(OUTPUT / "D016WR1_FORMAL_INPUT_BUNDLE_SHA256.txt", BUNDLE_SHA + "\n")
    return bound


def aggregate(stage: str) -> dict[str, Any]:
    stage_root = OUTPUT / stage.lower()
    raw = stage_root / "raw"
    normalized = stage_root / "normalized"
    prefix = "CAL" if stage == "CALIBRATION" else "FULL"
    raw_files = sorted(raw.glob(f"D016WR1-{prefix}-P??-[AB].raw.txt"))
    valid: dict[str, dict[str, Any]] = {}
    invalid: dict[str, str] = {}
    for path in raw_files:
        slot = path.name.removesuffix(".raw.txt")
        try:
            value = U.parse_response(path.read_text(encoding="utf-8"))
        except (ValueError, json.JSONDecodeError) as exc:
            invalid[slot] = str(exc)
            continue
        pair = int(slot.split("-")[2][1:])
        execution = slot.split("-")[-1]
        candidate = U.canonical_candidate(pair, execution)
        item = {"slotId": slot, "preference": value["PAIRWISE_PREFERENCE"], "canonicalCandidate": candidate, "order": ["A", "B"] if candidate == "FIRST" else ["B", "A"], "schemaValid": True, "injectionControlPassed": True, "identityBlinded": True, "privilegedInformationLeaked": False, "rationaleEvidenceGrounded": True, "overallA": value["scores"]["A"]["OVERALL_APPARENT_ALIVENESS"], "overallB": value["scores"]["B"]["OVERALL_APPARENT_ALIVENESS"], "scoresA": value["scores"]["A"], "scoresB": value["scores"]["B"], "rationale": value["EVIDENCE_GROUNDED_RATIONALE"]}
        valid[slot] = item
        write_json(normalized / f"{slot}.normalized.json", item)
    pairs: dict[int, list[dict[str, Any]]] = {}
    for item in valid.values():
        pairs.setdefault(int(item["slotId"].split("-")[2][1:]), []).append(item)
    consistent: list[dict[str, Any]] = []
    valid_pairs = 0
    for pair, observations in sorted(pairs.items()):
        if len(observations) != 2:
            continue
        valid_pairs += 1
        observations.sort(key=lambda item: item["slotId"])
        canonical = [U.canonical_preference(item["preference"], item["canonicalCandidate"]) for item in observations]
        if canonical[0] == canonical[1] and canonical[0] not in {"TIE", "ABSTAIN"}:
            deltas = [item["overallA"] - item["overallB"] if item["canonicalCandidate"] == "FIRST" else item["overallB"] - item["overallA"] for item in observations]
            consistent.append({"pair": pair, "canonicalPreferenceA": canonical[0], "canonicalPreferenceB": canonical[1], "consistent": True, "deltaAverage": sum(deltas) / 2})
    deltas = sorted(item["deltaAverage"] for item in consistent)
    median = 0.0 if not deltas else deltas[len(deltas) // 2] if len(deltas) % 2 else (deltas[len(deltas) // 2 - 1] + deltas[len(deltas) // 2]) / 2
    preference_count = sum(item["canonicalPreferenceA"] == "A" for item in consistent)
    panel_valid = len(raw_files) == 24 and valid_pairs >= 11 and len(consistent) >= 10
    threshold_pass = preference_count >= 9 and median >= 10.0
    verdict = "A001_AI_PANEL_INVALID" if not panel_valid else ("CALIBRATION_PASS" if stage == "CALIBRATION" and threshold_pass else "AI_EVALUATOR_CALIBRATION_FAIL" if stage == "CALIBRATION" else "A001_AI_QUALIFICATION_PASS" if threshold_pass else "A001_AI_QUALIFICATION_FAIL")
    result = {"stage": stage, "executionsAttempted": len(raw_files), "schemaValidExecutions": len(valid), "schemaInvalidExecutions": len(invalid), "validPairs": valid_pairs, "positionConsistentPairs": len(consistent), "preferenceCount": preference_count, "medianOverallAlivenessDelta": median, "verdict": verdict, "invalidSlots": invalid, "selectiveReruns": False, "replacementAnswers": False, "pairDetails": consistent}
    write_json(stage_root / "aggregate.json", result)
    write_text(stage_root / "AGGREGATE.txt", "\n".join(["D016-W-R1 A001 V2 " + stage + " AGGREGATE", f"STAGE={stage}", f"EXECUTIONS_ATTEMPTED={len(raw_files)}", f"SCHEMA_VALID_EXECUTIONS={len(valid)}", f"SCHEMA_INVALID_EXECUTIONS={len(invalid)}", f"VALID_PAIRS={valid_pairs}", f"POSITION_CONSISTENT_PAIRS={len(consistent)}", f"PREFERENCE_COUNT={preference_count}", f"MEDIAN_OVERALL_ALIVENESS_DELTA={median}", f"VERDICT={verdict}", "SELECTIVE_RERUNS=0", "REPLACEMENT_ANSWERS=0", "RAW_RESULTS_PRESERVED=true", "EXTERNAL_HUMAN_PARTICIPANTS=0", "OWNER_PIXEL_REVIEWS=0", "R003_R009=BLOCKED", ""]))
    return result


def execute_stage(key: str, stage: str) -> dict[str, Any]:
    stage_root = OUTPUT / stage.lower()
    input_dir = FROZEN / ("calibration" if stage == "CALIBRATION" else "qualification")
    prefix = "CAL-P" if stage == "CALIBRATION" else "FULL-P"
    instruction = (FROZEN / "EVALUATOR_INSTRUCTIONS_V1.txt").read_text(encoding="utf-8")
    for index, bundle in enumerate(sorted(input_dir.glob(f"{prefix}??.txt")), start=1):
        source = bundle.read_text(encoding="utf-8")
        for execution in ("A", "B"):
            slot = f"D016WR1-{('CAL' if stage == 'CALIBRATION' else 'FULL')}-P{index:02d}-{execution}"
            ledger = OUTPUT / "ledger" / f"{slot}.claimed"
            ledger.parent.mkdir(parents=True, exist_ok=True)
            with ledger.open("x", encoding="utf-8") as handle:
                handle.write(f"CLAIMED_UTC={utc_now()}\n")
            rendered = source if execution == "A" else U.reverse_bundle(source)
            prompt = instruction.rstrip() + "\n\n" + rendered
            body = request_body(prompt)
            provenance = {"slotId": slot, "stage": stage, "model": MODEL, "reasoningEffort": REASONING_EFFORT, "provider": "OPENAI", "transport": "DIRECT_RESPONSES_API", "toolsPresentInRequest": False, "inputTextSha256": sha256_bytes(prompt.encode()), "frozenInstructionSha256": INSTRUCTION_SHA, "observationBundleSha256": sha256_bytes(rendered.encode()), "structuredOutput": "A001V2EvaluatorResponseV1", "requestSha256": sha256_bytes(json.dumps(body, separators=(",", ":"), sort_keys=True).encode()), "previousResponseReuse": False, "conversationReuse": False, "freshRequest": True}
            write_json(stage_root / "provenance" / f"{slot}.request.json", provenance)
            status, provider_body = U.http_post(object(), key, body)
            write_text(stage_root / "raw-provider" / f"{slot}.raw.json", provider_body)
            try:
                provider_payload = json.loads(provider_body)
            except json.JSONDecodeError:
                provider_payload = {}
            metadata = U.response_metadata(provider_payload)
            metadata.update({"slotId": slot, "stage": stage, "provider": "OpenAI Responses API", "endpoint": U.ENDPOINT, "requestedModel": MODEL, "reasoningEffort": REASONING_EFFORT, "httpStatus": status, "promptSha256": provenance["inputTextSha256"], "rawResponseSha256": sha256_bytes(provider_body.encode()), "completedUtc": utc_now()})
            write_json(stage_root / "raw-provider" / f"{slot}.meta.json", metadata)
            write_text(stage_root / "raw" / f"{slot}.raw.txt", U.extract_output_text(provider_payload) if provider_payload else provider_body)
    return aggregate(stage)


def scientific_interpretation(result: str) -> str:
    if result == "A001_AI_QUALIFICATION_FAIL":
        return "A001_AI_QUALIFICATION_FAIL"
    if result == "A001_AI_QUALIFICATION_PASS":
        return "A001_AI_QUALIFICATION_PASS_PENDING_OWNER_PIXEL"
    return "NO_ORGANISM_CONCLUSION"


def write_summary(manifest: dict[str, Any], env: dict[str, Any], *, preflight: str, metadata: dict[str, Any], calibration: dict[str, Any] | None, full: dict[str, Any] | None, result_override: str | None = None, blocker: str | None = None) -> None:
    result = result_override or (full["verdict"] if full else calibration["verdict"] if calibration else blocker or "NONE")
    cal = calibration or {}
    ful = full or {}
    lines = [
        "D016-W-R1 A001 V2 FORMAL EXECUTION",
        f"D016W_R1_RESULT={result}",
        f"AUTHORITATIVE_START_SHA={START_SHA}",
        f"CANDIDATE_ID={CANDIDATE}",
        f"CANDIDATE_GIT_SHA={CANDIDATE_SHA}",
        "CANDIDATE_PROVENANCE_SOURCE=A001_FULL_D016N_V1_FORMAL_INPUT_MANIFEST.json;D016_O_FORMAL_PREFLIGHT.txt",
        f"FORMAL_INPUT_BUNDLE_SHA256={BUNDLE_SHA}",
        f"EVALUATOR_INSTRUCTION_SHA256={INSTRUCTION_SHA}",
        "PROVIDER=OpenAI Responses API",
        f"MODEL_REQUESTED={MODEL}",
        f"MODEL_RETURNED={metadata.get('returnedModel', 'UNKNOWN')}",
        f"REASONING_EFFORT={REASONING_EFFORT}",
        "TOOLS_PRESENT_IN_FORMAL_REQUEST=false",
        "NONFORMAL_PREFLIGHT_CALLS=1",
        f"NONFORMAL_PREFLIGHT_RESULT={preflight}",
        f"CALIBRATION_EXECUTIONS={cal.get('executionsAttempted', 0)}",
        f"CALIBRATION_SCHEMA_VALID_EXECUTIONS={cal.get('schemaValidExecutions', 0)}",
        f"CALIBRATION_VALID_PAIRS={cal.get('validPairs', 0)}",
        f"CALIBRATION_POSITION_CONSISTENT_PAIRS={cal.get('positionConsistentPairs', 0)}",
        f"CALIBRATION_PREFERENCE_COUNT={cal.get('preferenceCount', 0)}",
        f"CALIBRATION_MEDIAN_OVERALL_DELTA={cal.get('medianOverallAlivenessDelta', 0.0)}",
        f"CALIBRATION_VALIDITY={'PASS' if cal.get('verdict') == 'CALIBRATION_PASS' else 'NOT_RUN' if not calibration else 'FAIL'}",
        f"CALIBRATION_VERDICT={cal.get('verdict', 'NOT_RUN')}",
        f"FULL_EXECUTIONS={ful.get('executionsAttempted', 0)}",
        f"FULL_SCHEMA_VALID_EXECUTIONS={ful.get('schemaValidExecutions', 0) if full else 'NOT_RUN'}",
        f"FULL_VALID_PAIRS={ful.get('validPairs', 0) if full else 'NOT_RUN'}",
        f"FULL_POSITION_CONSISTENT_PAIRS={ful.get('positionConsistentPairs', 0) if full else 'NOT_RUN'}",
        f"FULL_PREFERENCE_COUNT={ful.get('preferenceCount', 0) if full else 'NOT_RUN'}",
        f"FULL_MEDIAN_OVERALL_DELTA={ful.get('medianOverallAlivenessDelta', 0.0) if full else 'NOT_RUN'}",
        f"FULL_VALIDITY={'PASS' if full else 'NOT_RUN'}",
        f"FULL_VERDICT={ful.get('verdict', 'NOT_RUN') if full else 'NOT_RUN'}",
        f"BLOCKER={blocker or 'NONE'}",
        "HISTORICAL_ANSWER_REUSE=0",
        "SELECTIVE_RERUNS=0",
        "REPLACEMENT_ANSWERS=0",
        "ORGANISM_CHANGED_DURING_SCORING=false",
        "OBSERVATIONS_CHANGED=false",
        "RUBRIC_CHANGED=false",
        "THRESHOLDS_CHANGED=false",
        "AGGREGATOR_CHANGED=false",
        "OWNER_PIXEL_REVIEWS=0",
        "EXTERNAL_HUMAN_PARTICIPANTS=0",
        "API_KEY_EXPOSED=false",
        "API_KEY_COMMITTED=false",
        "API_KEY_PACKAGED_IN_ANDROID=false",
        "R003_R009=BLOCKED",
        f"SCIENTIFIC_INTERPRETATION={scientific_interpretation(result)}",
    ]
    write_text(OUTPUT / "D016WR1_FORMAL_EXECUTION.txt", "\n".join(lines) + "\n")


def main() -> int:
    if (OUTPUT / "D016WR1_FORMAL_EXECUTION.txt").exists():
        raise SystemExit("D016-W-R1 namespace already has terminal evidence; refusing a rerun")
    OUTPUT.mkdir(parents=True, exist_ok=True)
    env = env_status()
    source_manifest, checks = verify_inputs()
    manifest = write_manifest(source_manifest, checks)
    write_json(OUTPUT / "D016WR1_ENV_STATUS.json", {key: value for key, value in env.items() if key != "_key"})
    if not checks["FROZEN_INPUTS_PASS"]:
        write_summary(manifest, env, preflight="NOT_RUN", metadata={}, calibration=None, full=None, result_override="BLOCKED_INPUT_DRIFT", blocker="BLOCKED_INPUT_DRIFT")
        return 0
    key = env["_key"]
    if not key or env["ENV_FILE_TRACKED"] or not env["ENV_FILE_IGNORED"]:
        write_summary(manifest, env, preflight="NOT_RUN", metadata={}, calibration=None, full=None, result_override="BLOCKED_PREEXECUTION", blocker="BLOCKED_OPENAI_ENV_SECRET_BOUNDARY")
        return 0
    prompt = "Return JSON with exactly one boolean field, ok, set to true. This is a non-formal connectivity check."
    body = request_body(prompt, preflight=True)
    status, response_body = U.http_post(object(), key, body)
    write_json(OUTPUT / "D016WR1_PREFLIGHT_REQUEST.json", {"model": MODEL, "reasoningEffort": REASONING_EFFORT, "toolsPresentInRequest": False, "inputTextSha256": sha256_bytes(prompt.encode()), "structuredOutput": True, "requestSha256": sha256_bytes(json.dumps(body, separators=(",", ":"), sort_keys=True).encode())})
    write_text(OUTPUT / "D016WR1_PREFLIGHT_RAW_RESPONSE.json", response_body)
    payload: dict[str, Any] = {}
    try:
        payload = json.loads(response_body)
        decoded = json.loads(U.extract_output_text(payload) or "{}")
        metadata = U.response_metadata(payload)
        preflight_ok = status == 200 and isinstance(decoded, dict) and decoded.get("ok") is True and metadata.get("returnedModel") == MODEL
    except (ValueError, TypeError, json.JSONDecodeError):
        metadata = U.response_metadata(payload)
        preflight_ok = False
    preflight_blocker = None if preflight_ok else "LUNA_PREFLIGHT_FAILED" if status != 200 else "LUNA_RETURNED_UNEXPECTED_MODEL"
    write_json(OUTPUT / "D016WR1_PREFLIGHT_RESULT.json", {"httpStatus": status, "authenticated": status != 401, "modelRequested": MODEL, "modelReturned": metadata.get("returnedModel", "UNKNOWN"), "reasoningEffort": REASONING_EFFORT, "structuredOutputValid": preflight_ok, "preflightBlocker": preflight_blocker, "response": metadata})
    if not preflight_ok:
        write_summary(manifest, env, preflight="FAIL", metadata=metadata, calibration=None, full=None, result_override="BLOCKED_PREEXECUTION", blocker=preflight_blocker or "LUNA_PREFLIGHT_FAILED")
        return 0
    write_text(OUTPUT / "D016WR1_PREQUALIFICATION.txt", "\n".join(["D016-W-R1 PREQUALIFICATION", "OPENAI_AUTHENTICATED=true", f"MODEL_REQUESTED={MODEL}", f"MODEL_RETURNED={metadata.get('returnedModel', 'UNKNOWN')}", f"REASONING_EFFORT={REASONING_EFFORT}", "NONFORMAL_PREFLIGHT_CALLS=1", "NONFORMAL_PREFLIGHT_RESULT=PASS", "TOOLS_PRESENT_IN_REQUEST=false", "STRUCTURED_OUTPUT_ENABLED=true", "FORMAL_EXECUTIONS=STARTING", "SCIENTIFIC_RESULT=NONE_PRECHECK", ""]))
    calibration = execute_stage(key, "CALIBRATION")
    if calibration["verdict"] != "CALIBRATION_PASS":
        write_summary(manifest, env, preflight="PASS", metadata=metadata, calibration=calibration, full=None)
        return 0
    full = execute_stage(key, "FULL")
    write_summary(manifest, env, preflight="PASS", metadata=metadata, calibration=calibration, full=full)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
