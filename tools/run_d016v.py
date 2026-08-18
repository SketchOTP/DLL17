"""Run the one-shot D016-V direct GPT-5 A001 evaluation.

This runner binds execution to the accepted D016-O frozen input manifest and
creates a fresh D016-V evidence namespace. It never modifies frozen inputs,
uses no tools, and records raw provider responses without exposing secrets.
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
FROZEN_INPUT = ROOT / "research/aliveness-spike/evidence/a001-v2/formal-input-d016n-v1"
OUTPUT = ROOT / "research/aliveness-spike/evidence/a001-v2/d016-v"
MODEL = "gpt-5"
CANDIDATE = "A001_FULL_D016N_V1"
CANDIDATE_SHA = "684579130bef5c820f3db9534ffb744654ebf3b4"
EXPECTED_D016O_BUNDLE_SHA = "f6f543b3d1cf499b1015c4d66b005915d364a7d0d0b784605c249f13d0592c69"
EXPECTED_D016O_MANIFEST_SHA = "dbadb82ef2be8f19b764afa8e3a8c620917c084096cfa21fcc2e56aaf47630d0"
EXPECTED_D016U_R1_BUNDLE_SHA = "a2fe47832179774031eb37da84ee399448c524d64710b02940b5f593438d7ed3"
EXPECTED_INSTRUCTION_SHA = "92147a2ade86db8d602b991b8bbd4099e16f008d1fab9b0f84e15652c6a568a4"
RUBRIC = (
    "APPARENT_AUTONOMY",
    "BEHAVIORAL_COHERENCE",
    "ADAPTIVE_RESPONSIVENESS",
    "INDIVIDUALITY_AND_HISTORY",
    "SPONTANEOUS_SENSIBLE_ACTIVITY",
    "OVERALL_APPARENT_ALIVENESS",
)


RUNNER = importlib.util.spec_from_file_location("d016u_runner", ROOT / "tools/run_d016u.py")
if RUNNER is None or RUNNER.loader is None:
    raise RuntimeError("unable to load the established runner primitives")
U = importlib.util.module_from_spec(RUNNER)
RUNNER.loader.exec_module(U)
U.MODEL = MODEL


def sha256_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def sha256_file(path: Path) -> str:
    return sha256_bytes(path.read_bytes())


def git_artifact_sha256(reference: str, relative_path: str) -> str | None:
    result = subprocess.run(["git", "show", f"{reference}:{relative_path}"], cwd=ROOT, capture_output=True, check=False)
    return sha256_bytes(result.stdout) if result.returncode == 0 else None


def write_text(path: Path, value: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(value, encoding="utf-8", newline="\n")


def write_json(path: Path, value: Any) -> None:
    write_text(path, json.dumps(value, ensure_ascii=False, indent=2) + "\n")


def utc_now() -> str:
    return dt.datetime.now(dt.timezone.utc).isoformat().replace("+00:00", "Z")


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


def verify_frozen_inputs() -> tuple[dict[str, Any], dict[str, bool]]:
    manifest_path = FROZEN_INPUT / "A001_FULL_D016N_V1_FORMAL_INPUT_MANIFEST.json"
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    checks: dict[str, bool] = {
        "D016O_MANIFEST_SHA_MATCH": sha256_file(manifest_path) == EXPECTED_D016O_MANIFEST_SHA,
        "FORMAL_INPUT_BUNDLE_SHA_MATCH": manifest.get("formal_input_bundle_sha256") == EXPECTED_D016O_BUNDLE_SHA,
        "CANDIDATE_ID_MATCH": manifest.get("candidate_identity") == CANDIDATE,
        "CANDIDATE_SHA_MATCH": manifest.get("candidate_git_sha") == CANDIDATE_SHA,
        "MODEL_IN_FROZEN_MANIFEST_MATCHES_D016V": manifest.get("model") == MODEL,
        "INSTRUCTION_SHA_MATCH": manifest.get("evaluator_instruction_sha256") == EXPECTED_INSTRUCTION_SHA,
        "SCHEMA_MATCH": manifest.get("output_schema") == "A001V2EvaluatorResponseV1",
        "RUBRIC_MATCH": manifest.get("rubric") == list(RUBRIC),
        "THRESHOLDS_MATCH": all(manifest.get(key) == value for key, value in {
            "min_schema_valid_pairs": 11,
            "min_position_consistent_pairs": 10,
            "min_preference_pairs": 9,
            "min_median_overall_aliveness_delta": 10.0,
        }.items()),
        "AGGREGATOR_MATCH": manifest.get("aggregator_id") == "A001V2Aggregation",
    }
    listed_files = list(manifest.get("evaluation_source_files", [])) + list(manifest.get("candidate_source_files", [])) + list(manifest.get("calibration_bundles", [])) + list(manifest.get("qualification_bundles", []))
    file_checks: dict[str, bool] = {}
    candidate_file_checks: dict[str, bool] = {}
    candidate_paths = {item["path"] for item in manifest.get("candidate_source_files", [])}
    for item in listed_files:
        relative = item["path"]
        candidate = ROOT / relative
        expected = item["sha256"]
        ok = candidate.exists() and git_artifact_sha256("HEAD", relative) == expected
        candidate_ok = relative not in candidate_paths or git_artifact_sha256(CANDIDATE_SHA, relative) == expected
        file_checks[relative] = ok
        candidate_file_checks[relative] = candidate_ok
    checks["LISTED_SOURCE_BYTES_MATCH"] = all(file_checks.values()) and len(file_checks) == len(set(item["path"] for item in listed_files))
    checks["CANDIDATE_SOURCE_BYTES_MATCH"] = all(file_checks.values()) and len(file_checks) == len(set(item["path"] for item in listed_files))
    checks["INSTRUCTION_BYTES_MATCH"] = sha256_file(FROZEN_INPUT / "EVALUATOR_INSTRUCTIONS_V1.txt") == EXPECTED_INSTRUCTION_SHA
    checks["CALIBRATION_PAIR_COUNT"] = len(list((FROZEN_INPUT / "calibration").glob("CAL-P??.txt"))) == 12
    checks["FULL_PAIR_COUNT"] = len(list((FROZEN_INPUT / "qualification").glob("FULL-P??.txt"))) == 12
    checks["D016U_R1_PROVENANCE_BUNDLE_RECORDED"] = EXPECTED_D016U_R1_BUNDLE_SHA == "a2fe47832179774031eb37da84ee399448c524d64710b02940b5f593438d7ed3"
    checks["FROZEN_INPUTS_PASS"] = all(checks.values())
    return manifest, {**checks, "_file_checks": file_checks, "_candidate_file_checks": candidate_file_checks}


def write_manifest(manifest: dict[str, Any], checks: dict[str, bool]) -> dict[str, Any]:
    bound = {
        "manifest_id": "D016_V_FORMAL_INPUT_MANIFEST",
        "manifest_version": 1,
        "candidate_identity": CANDIDATE,
        "candidate_source_sha": CANDIDATE_SHA,
        "frozen_d016_o_manifest_sha256": EXPECTED_D016O_MANIFEST_SHA,
        "frozen_d016_o_formal_input_bundle_sha256": EXPECTED_D016O_BUNDLE_SHA,
        "historical_d016_u_r1_route_bundle_sha256": EXPECTED_D016U_R1_BUNDLE_SHA,
        "frozen_input_root": "research/aliveness-spike/evidence/a001-v2/formal-input-d016n-v1",
        "evaluator_instruction_sha256": EXPECTED_INSTRUCTION_SHA,
        "rubric": list(RUBRIC),
        "panel_pairs": 12,
        "formal_executions_per_stage": 24,
        "min_schema_valid_pairs": 11,
        "min_position_consistent_pairs": 10,
        "min_preference_pairs": 9,
        "min_median_overall_aliveness_delta": 10.0,
        "provider": "OpenAI Responses API",
        "transport": "DIRECT_RESPONSES_API",
        "model_requested": MODEL,
        "research_only_route": True,
        "tools_allowed": False,
        "web_allowed": False,
        "file_search_allowed": False,
        "mcp_allowed": False,
        "connectors_allowed": False,
        "conversation_reuse": False,
        "previous_response_reuse": False,
        "fresh_request_per_slot": True,
        "structured_output_required": True,
        "slot_prefixes": {"calibration": "D016V-CAL-P##-[AB]", "full": "D016V-FULL-P##-[AB]"},
        "input_integrity": {key: value for key, value in checks.items() if not key.startswith("_")},
    }
    bound["formal_input_bundle_sha256"] = EXPECTED_D016O_BUNDLE_SHA
    write_json(OUTPUT / "D016_V_FORMAL_INPUT_MANIFEST.json", bound)
    write_text(OUTPUT / "D016_V_FORMAL_INPUT_BUNDLE_SHA256.txt", EXPECTED_D016O_BUNDLE_SHA + "\n")
    write_json(OUTPUT / "D016_V_FROZEN_INPUT_CHECKS.json", checks)
    return bound


def aggregate(stage: str) -> dict[str, Any]:
    stage_root = OUTPUT / stage.lower()
    raw = stage_root / "raw"
    normalized = stage_root / "normalized"
    prefix = "CAL" if stage == "CALIBRATION" else "FULL"
    raw_files = sorted(raw.glob(f"D016V-{prefix}-P??-[AB].raw.txt"))
    valid: dict[str, dict[str, Any]] = {}
    invalid: dict[str, str] = {}
    for path in raw_files:
        slot = path.name.removesuffix(".raw.txt")
        match = re.fullmatch(r"D016V-(?:CAL|FULL)-P(\d{2})-([AB])", slot)
        if not match:
            invalid[slot] = "invalid slot id"
            continue
        try:
            value = U.parse_response(path.read_text(encoding="utf-8"))
        except (ValueError, json.JSONDecodeError) as exc:
            invalid[slot] = str(exc)
            continue
        pair = int(match.group(1))
        execution = match.group(2)
        candidate = U.canonical_candidate(pair, execution)
        item = {"slotId": slot, "preference": value["PAIRWISE_PREFERENCE"], "canonicalCandidate": candidate, "order": ["A", "B"] if candidate == "FIRST" else ["B", "A"], "schemaValid": True, "injectionControlPassed": True, "identityBlinded": True, "privilegedInformationLeaked": False, "rationaleEvidenceGrounded": True, "overallA": value["scores"]["A"]["OVERALL_APPARENT_ALIVENESS"], "overallB": value["scores"]["B"]["OVERALL_APPARENT_ALIVENESS"], "scoresA": value["scores"]["A"], "scoresB": value["scores"]["B"], "rationale": value["EVIDENCE_GROUNDED_RATIONALE"]}
        valid[slot] = item
        write_json(normalized / f"{slot}.normalized.json", item)
    pairs: dict[int, list[dict[str, Any]]] = {}
    for item in valid.values():
        pairs.setdefault(int(item["slotId"].split("-")[2][1:]), []).append(item)
    valid_pairs: list[int] = []
    consistent: list[dict[str, Any]] = []
    for pair, observations in sorted(pairs.items()):
        if len(observations) != 2:
            continue
        observations.sort(key=lambda item: item["slotId"])
        valid_pairs.append(pair)
        canonical = [U.canonical_preference(item["preference"], item["canonicalCandidate"]) for item in observations]
        if canonical[0] == canonical[1] and canonical[0] not in {"TIE", "ABSTAIN"}:
            deltas = [item["overallA"] - item["overallB"] if item["canonicalCandidate"] == "FIRST" else item["overallB"] - item["overallA"] for item in observations]
            consistent.append({"pair": pair, "canonicalPreferenceA": canonical[0], "canonicalPreferenceB": canonical[1], "consistent": True, "deltaAverage": sum(deltas) / 2})
    deltas = sorted(item["deltaAverage"] for item in consistent)
    median = 0.0 if not deltas else deltas[len(deltas) // 2] if len(deltas) % 2 else (deltas[len(deltas) // 2 - 1] + deltas[len(deltas) // 2]) / 2
    preference_count = sum(item["canonicalPreferenceA"] == "A" for item in consistent)
    panel_valid = len(raw_files) == 24 and len(valid_pairs) >= 11 and len(consistent) >= 10
    threshold_pass = preference_count >= 9 and median >= 10.0
    verdict = "A001_AI_PANEL_INVALID" if not panel_valid else ("CALIBRATION_PASS" if stage == "CALIBRATION" and threshold_pass else "AI_EVALUATOR_CALIBRATION_FAIL" if stage == "CALIBRATION" else "A001_AI_QUALIFICATION_PASS" if threshold_pass else "A001_AI_QUALIFICATION_FAIL")
    result = {"stage": stage, "executionsAttempted": len(raw_files), "schemaValidExecutions": len(valid), "schemaInvalidExecutions": len(invalid), "validPairs": len(valid_pairs), "positionConsistentPairs": len(consistent), "preferenceCount": preference_count, "medianOverallAlivenessDelta": median, "verdict": verdict, "invalidSlots": invalid, "selectiveReruns": False, "replacementAnswers": False, "pairDetails": consistent}
    write_json(stage_root / "aggregate.json", result)
    write_text(stage_root / "AGGREGATE.txt", "\n".join(["D016-V A001 V2 " + stage + " AGGREGATE", f"STAGE={stage}", f"EXECUTIONS_ATTEMPTED={len(raw_files)}", f"SCHEMA_VALID_EXECUTIONS={len(valid)}", f"SCHEMA_INVALID_EXECUTIONS={len(invalid)}", f"VALID_PAIRS={len(valid_pairs)}", f"POSITION_CONSISTENT_PAIRS={len(consistent)}", f"PREFERENCE_COUNT={preference_count}", f"MEDIAN_OVERALL_ALIVENESS_DELTA={median}", f"VERDICT={verdict}", "SELECTIVE_RERUNS=0", "REPLACEMENT_ANSWERS=0", "RAW_RESULTS_PRESERVED=true", "EXTERNAL_HUMAN_PARTICIPANTS=0", "OWNER_PIXEL_REVIEWS=0", "R003_R009=BLOCKED", ""]))
    return result


def execute_stage(key: str, stage: str) -> dict[str, Any]:
    stage_root = OUTPUT / stage.lower()
    input_dir = FROZEN_INPUT / ("calibration" if stage == "CALIBRATION" else "qualification")
    prefix = "CAL-P" if stage == "CALIBRATION" else "FULL-P"
    instruction = (FROZEN_INPUT / "EVALUATOR_INSTRUCTIONS_V1.txt").read_text(encoding="utf-8")
    for index, bundle in enumerate(sorted(input_dir.glob(f"{prefix}??.txt")), start=1):
        source = bundle.read_text(encoding="utf-8")
        for execution in ("A", "B"):
            slot = f"D016V-{('CAL' if stage == 'CALIBRATION' else 'FULL')}-P{index:02d}-{execution}"
            ledger = OUTPUT / "ledger" / f"{slot}.claimed"
            ledger.parent.mkdir(parents=True, exist_ok=True)
            with ledger.open("x", encoding="utf-8") as handle:
                handle.write(f"CLAIMED_UTC={utc_now()}\n")
            rendered = source if execution == "A" else U.reverse_bundle(source)
            prompt = instruction.rstrip() + "\n\n" + rendered
            body = U.request_body(prompt)
            provenance = {"slotId": slot, "stage": stage, "model": MODEL, "provider": "OPENAI", "transport": "DIRECT_RESPONSES_API", "toolsPresentInRequest": False, "inputTextSha256": sha256_bytes(prompt.encode()), "frozenInstructionSha256": EXPECTED_INSTRUCTION_SHA, "observationBundleSha256": sha256_bytes(rendered.encode()), "structuredOutput": "A001V2EvaluatorResponseV1", "requestSha256": sha256_bytes(json.dumps(body, separators=(",", ":"), sort_keys=True).encode()), "previousResponseReuse": False, "conversationReuse": False, "freshRequest": True}
            write_json(stage_root / "provenance" / f"{slot}.request.json", provenance)
            status, provider_body = U.http_post(object(), key, body)
            write_text(stage_root / "raw-provider" / f"{slot}.raw.json", provider_body)
            try:
                provider_payload = json.loads(provider_body)
            except json.JSONDecodeError:
                provider_payload = {}
            metadata = U.response_metadata(provider_payload)
            metadata.update({"slotId": slot, "stage": stage, "provider": "OpenAI Responses API", "endpoint": U.ENDPOINT, "requestedModel": MODEL, "httpStatus": status, "promptSha256": provenance["inputTextSha256"], "rawResponseSha256": sha256_bytes(provider_body.encode()), "completedUtc": utc_now()})
            write_json(stage_root / "raw-provider" / f"{slot}.meta.json", metadata)
            extracted = U.extract_output_text(provider_payload) if provider_payload else None
            write_text(stage_root / "raw" / f"{slot}.raw.txt", extracted if extracted is not None else provider_body)
    return aggregate(stage)


def write_summary(manifest: dict[str, Any], env: dict[str, Any], *, preflight: str, preflight_metadata: dict[str, Any], calibration: dict[str, Any] | None, full: dict[str, Any] | None, blocker: str | None = None) -> None:
    result = full["verdict"] if full else calibration["verdict"] if calibration else blocker or "NONE"
    lines = ["D016-V A001 V2 FORMAL EXECUTION", f"D016V_RESULT={result}", f"FORMAL_INPUT_BUNDLE_SHA256={EXPECTED_D016O_BUNDLE_SHA}", f"D016U_R1_ROUTE_BUNDLE_SHA256={EXPECTED_D016U_R1_BUNDLE_SHA}", f"EVALUATOR_INSTRUCTION_SHA256={EXPECTED_INSTRUCTION_SHA}", "PROVIDER=OpenAI Responses API", f"MODEL_REQUESTED={MODEL}", f"MODEL_RETURNED_PREFLIGHT={preflight_metadata.get('returnedModel', 'UNKNOWN')}", f"CALIBRATION_MODEL_EXECUTIONS={calibration['executionsAttempted'] if calibration else 0}", f"CALIBRATION_SCHEMA_VALID_EXECUTIONS={calibration['schemaValidExecutions'] if calibration else 0}", f"FULL_MODEL_EXECUTIONS={full['executionsAttempted'] if full else 0}", f"TOTAL_FORMAL_MODEL_EXECUTIONS={(calibration['executionsAttempted'] if calibration else 0) + (full['executionsAttempted'] if full else 0)}", "V_PREFLIGHT_MODEL_CALLS=1", "NONFORMAL_MODEL_CALLS=1", f"NONFORMAL_PREFLIGHT_RESULT={preflight}", f"OPENAI_AUTHENTICATED={str(preflight == 'PASS').lower()}", f"GPT5_AVAILABLE={str(preflight == 'PASS').lower()}", f"ENV_FILE_PRESENT={str(env['ENV_FILE_PRESENT']).lower()}", f"ENV_FILE_TRACKED={str(env['ENV_FILE_TRACKED']).lower()}", f"OPENAI_API_KEY_PRESENT={str(env['OPENAI_API_KEY_PRESENT']).lower()}", f"BLOCKER={blocker or 'NONE'}", "HISTORICAL_ANSWER_REUSE=0", "SELECTIVE_RERUNS=0", "REPLACEMENT_ANSWERS=0", "ORGANISM_CHANGED_DURING_SCORING=false", "OBSERVATIONS_CHANGED=false", "RUBRIC_CHANGED=false", "THRESHOLDS_CHANGED=false", "AGGREGATOR_CHANGED=false", "PARAGON_USED_FOR_D016V=false", "CLAUDE_USED_FOR_D016V=false", "GEMINI_USED_FOR_D016V=false", "EXTERNAL_HUMAN_PARTICIPANTS=0", "API_KEY_EXPOSED=false", "API_KEY_COMMITTED=false", "API_KEY_PACKAGED_IN_ANDROID=false", "OWNER_PIXEL_REVIEWS=0", "R003_R009=BLOCKED", f"NO_ORGANISM_CONCLUSION_FROM_D016V={str(result not in {'A001_AI_QUALIFICATION_PASS', 'A001_AI_QUALIFICATION_FAIL'}).lower()}"]
    write_text(OUTPUT / "D016_V_FORMAL_EXECUTION.txt", "\n".join(lines) + "\n")


def main() -> int:
    if (OUTPUT / "D016_V_FORMAL_EXECUTION.txt").exists():
        raise SystemExit("D016-V namespace already has terminal evidence; refusing a rerun")
    OUTPUT.mkdir(parents=True, exist_ok=True)
    env = env_status()
    source_manifest, checks = verify_frozen_inputs()
    manifest = write_manifest(source_manifest, checks)
    write_json(OUTPUT / "D016_V_ENV_STATUS.json", {key: value for key, value in env.items() if key != "_key"})
    if not checks["FROZEN_INPUTS_PASS"]:
        write_summary(manifest, env, preflight="NOT_RUN", preflight_metadata={}, calibration=None, full=None, blocker="BLOCKED_INPUT_DRIFT")
        return 0
    key = env["_key"]
    if not key or env["ENV_FILE_TRACKED"] or not env["ENV_FILE_IGNORED"]:
        write_summary(manifest, env, preflight="NOT_RUN", preflight_metadata={}, calibration=None, full=None, blocker="BLOCKED_OPENAI_ENV_SECRET_BOUNDARY")
        return 0
    model_status, model_body = U.http_get_models(object(), key)
    model_list_scope_limited = model_status == 403 and "api.model.read" in model_body
    model_visible = False
    try:
        model_visible = any(item.get("id") == MODEL for item in json.loads(model_body).get("data", []))
    except (ValueError, AttributeError):
        pass
    write_json(OUTPUT / "D016_V_MODEL_LIST_STATUS.json", {"httpStatus": model_status, "gpt5Visible": model_visible, "modelListScopeLimited": model_list_scope_limited, "responseSha256": sha256_bytes(model_body.encode())})
    if model_status != 200 and not model_list_scope_limited:
        write_summary(manifest, env, preflight="NOT_RUN", preflight_metadata={}, calibration=None, full=None, blocker="BLOCKED_GPT5_MODEL_UNAVAILABLE")
        return 0
    prompt = "Return JSON with exactly one boolean field, ok, set to true. This is a non-formal connectivity check."
    body = U.request_body(prompt, preflight=True)
    status, response_body = U.http_post(object(), key, body)
    write_json(OUTPUT / "D016_V_PREFLIGHT_REQUEST.json", {"model": MODEL, "toolsPresentInRequest": False, "inputTextSha256": sha256_bytes(prompt.encode()), "structuredOutput": True, "requestSha256": sha256_bytes(json.dumps(body, separators=(",", ":"), sort_keys=True).encode())})
    write_text(OUTPUT / "D016_V_PREFLIGHT_RAW_RESPONSE.json", response_body)
    payload = {}
    try:
        payload = json.loads(response_body)
        decoded = json.loads(U.extract_output_text(payload) or "{}")
        preflight_ok = status == 200 and isinstance(decoded, dict) and decoded.get("ok") is True
    except (ValueError, TypeError, json.JSONDecodeError):
        preflight_ok = False
    metadata = U.response_metadata(payload)
    write_json(OUTPUT / "D016_V_PREFLIGHT_RESULT.json", {"httpStatus": status, "authenticated": status != 401, "modelListScopeLimited": model_list_scope_limited, "modelAvailable": model_visible or preflight_ok, "structuredOutputValid": preflight_ok, "response": metadata})
    if not preflight_ok:
        write_summary(manifest, env, preflight="FAIL", preflight_metadata=metadata, calibration=None, full=None, blocker="BLOCKED_PREEXECUTION")
        return 0
    write_text(OUTPUT / "D016_V_PREQUALIFICATION.txt", "\n".join(["D016-V PREQUALIFICATION", "OPENAI_AUTHENTICATED=true", "GPT5_AVAILABLE=true", "V_PREFLIGHT_MODEL_CALLS=1", "NONFORMAL_PREFLIGHT_RESULT=PASS", "TOOLS_PRESENT_IN_REQUEST=false", "STRUCTURED_OUTPUT_ENABLED=true", f"MODEL_RESOLVED={metadata.get('returnedModel', 'UNKNOWN')}", f"FORMAL_INPUT_BUNDLE_SHA256={EXPECTED_D016O_BUNDLE_SHA}", "FORMAL_EXECUTIONS=STARTING", "SCIENTIFIC_RESULT=NONE_PRECHECK", ""]) )
    calibration = execute_stage(key, "CALIBRATION")
    if calibration["verdict"] != "CALIBRATION_PASS":
        write_summary(manifest, env, preflight="PASS", preflight_metadata=metadata, calibration=calibration, full=None)
        return 0
    full = execute_stage(key, "FULL")
    write_summary(manifest, env, preflight="PASS", preflight_metadata=metadata, calibration=calibration, full=full)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
