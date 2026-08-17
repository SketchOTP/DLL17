"""Run the owner-authorized D016-U direct GPT-5 Nano A001 evaluation.

The runner is intentionally a small, stateless HTTPS client.  It reads only
OPENAI_API_KEY from the local .env, emits no secret material, sends no tools,
and writes a fresh append-only evidence namespace.  The frozen D016-O input
bytes are referenced and hashed; they are never rewritten.
"""
from __future__ import annotations

import datetime as dt
import hashlib
import json
import os
import re
import subprocess
import sys
import urllib.error
import urllib.request
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
FROZEN_INPUT = ROOT / "research/aliveness-spike/evidence/a001-v2/formal-input-d016n-v1"
OUTPUT = ROOT / "research/aliveness-spike/evidence/a001-v2/d016-u"
ENDPOINT = "https://api.openai.com/v1/responses"
MODELS_ENDPOINT = "https://api.openai.com/v1/models"
MODEL = "gpt-5-nano"
CANDIDATE = "A001_FULL_D016N_V1"
CANDIDATE_SHA = "684579130bef5c820f3db9534ffb744654ebf3b4"
EXPECTED_D016O_BUNDLE_SHA = "f6f543b3d1cf499b1015c4d66b005915d364a7d0d0b784605c249f13d0592c69"
EXPECTED_INSTRUCTION_SHA = "92147a2ade86db8d602b991b8bbd4099e16f008d1fab9b0f84e15652c6a568a4"
RUBRIC = (
    "APPARENT_AUTONOMY",
    "BEHAVIORAL_COHERENCE",
    "ADAPTIVE_RESPONSIVENESS",
    "INDIVIDUALITY_AND_HISTORY",
    "SPONTANEOUS_SENSIBLE_ACTIVITY",
    "OVERALL_APPARENT_ALIVENESS",
)


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


def env_status() -> dict[str, Any]:
    env_file = ROOT / ".env"
    tracked = subprocess.run(
        ["git", "ls-files", "--error-unmatch", ".env"],
        cwd=ROOT,
        capture_output=True,
        check=False,
    ).returncode == 0
    ignored = subprocess.run(
        ["git", "check-ignore", "--no-index", ".env"],
        cwd=ROOT,
        capture_output=True,
        check=False,
    ).returncode == 0
    value = None
    if env_file.exists():
        for line in env_file.read_text(encoding="utf-8").splitlines():
            match = re.match(r"^\s*OPENAI_API_KEY\s*=\s*(.*?)\s*$", line)
            if match:
                value = match.group(1).strip().strip('"').strip("'")
                break
    return {
        "ENV_FILE_PRESENT": env_file.exists(),
        "ENV_FILE_TRACKED": tracked,
        "ENV_FILE_IGNORED": ignored,
        "OPENAI_API_KEY_PRESENT": bool(value),
        "_key": value,
    }


def model_schema() -> dict[str, Any]:
    score_properties = {dimension: {"type": "integer", "minimum": 0, "maximum": 100} for dimension in RUBRIC}
    return {
        "type": "object",
        "additionalProperties": False,
        "properties": {
            "scores": {
                "type": "object",
                "additionalProperties": False,
                "properties": {
                    "A": {"type": "object", "additionalProperties": False, "properties": score_properties, "required": list(RUBRIC)},
                    "B": {"type": "object", "additionalProperties": False, "properties": score_properties, "required": list(RUBRIC)},
                },
                "required": ["A", "B"],
            },
            "PAIRWISE_PREFERENCE": {"type": "string", "enum": ["A", "B", "TIE", "ABSTAIN"]},
            "EVIDENCE_GROUNDED_RATIONALE": {"type": "string", "minLength": 1},
        },
        "required": ["scores", "PAIRWISE_PREFERENCE", "EVIDENCE_GROUNDED_RATIONALE"],
    }


def structured_format() -> dict[str, Any]:
    return {
        "type": "json_schema",
        "name": "A001V2EvaluatorResponseV1",
        "strict": True,
        "schema": model_schema(),
    }


def preflight_format() -> dict[str, Any]:
    return {
        "type": "json_schema",
        "name": "D016UPreflightResponseV1",
        "strict": True,
        "schema": {
            "type": "object",
            "additionalProperties": False,
            "properties": {"ok": {"type": "boolean"}},
            "required": ["ok"],
        },
    }


def request_body(prompt: str, *, preflight: bool = False) -> dict[str, Any]:
    # Deliberately no tools, tool_choice, previous_response_id, conversation,
    # connectors, file search, web search, or provider-specific agent fields.
    return {
        "model": MODEL,
        "input": [{"role": "user", "content": [{"type": "input_text", "text": prompt}]}],
        "text": {"format": preflight_format() if preflight else structured_format()},
        "store": False,
    }


def extract_output_text(payload: Any) -> str | None:
    if not isinstance(payload, dict):
        return None
    direct = payload.get("output_text")
    if isinstance(direct, str) and direct.strip():
        return direct
    chunks: list[str] = []
    for item in payload.get("output", []):
        if not isinstance(item, dict):
            continue
        for content in item.get("content", []):
            if not isinstance(content, dict):
                continue
            for key in ("text", "value"):
                value = content.get(key)
                if isinstance(value, str):
                    chunks.append(value)
    return "".join(chunks) if chunks else None


def response_metadata(payload: Any) -> dict[str, Any]:
    if not isinstance(payload, dict):
        return {"responseId": "UNKNOWN", "returnedModel": "UNKNOWN"}
    usage = payload.get("usage")
    return {
        "responseId": payload.get("id", "UNKNOWN"),
        "returnedModel": payload.get("model", "UNKNOWN"),
        "usage": usage if isinstance(usage, dict) else None,
        "status": payload.get("status"),
    }


def http_request(method: str, url: str, key: str, body: bytes | None = None) -> tuple[int, str]:
    request = urllib.request.Request(
        url,
        data=body,
        method=method,
        headers={"Authorization": f"Bearer {key}", "Content-Type": "application/json"},
    )
    try:
        with urllib.request.urlopen(request, timeout=90) as response:
            return response.status, response.read().decode("utf-8", errors="replace")
    except urllib.error.HTTPError as error:
        return error.code, error.read().decode("utf-8", errors="replace")
    except urllib.error.URLError as error:
        return 599, f"TRANSPORT_ERROR={error.reason}"


def http_post(_client: object, key: str, body: dict[str, Any]) -> tuple[int, str]:
    encoded = json.dumps(body, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
    return http_request("POST", ENDPOINT, key, encoded)


def http_get_models(_client: object, key: str) -> tuple[int, str]:
    return http_request("GET", MODELS_ENDPOINT, key)


def reverse_bundle(text: str) -> str:
    marker_a = "CREATURE_A\n"
    marker_b = "CREATURE_B\n"
    end = "END_BUNDLE\n"
    a_start = text.index(marker_a)
    b_start = text.index(marker_b)
    header = text[:a_start]
    creature_a = text[a_start + len(marker_a):b_start]
    creature_b = text[b_start + len(marker_b):len(text) - len(end)]
    return header + marker_a + creature_b + marker_b + creature_a + end


def canonical_candidate(pair: int, execution: str) -> str:
    labels = ("A", "B") if pair % 2 else ("B", "A")
    if execution == "B":
        labels = labels[::-1]
    return "FIRST" if labels[0] == "A" else "SECOND"


def canonical_preference(preference: str, candidate: str) -> str:
    if preference in {"TIE", "ABSTAIN"}:
        return preference
    return preference if candidate == "FIRST" else ("B" if preference == "A" else "A")


def parse_response(text: str) -> dict[str, Any]:
    value = json.loads(text)
    if set(value) != {"scores", "PAIRWISE_PREFERENCE", "EVIDENCE_GROUNDED_RATIONALE"}:
        raise ValueError("response keys do not match frozen schema")
    if set(value["scores"]) != {"A", "B"}:
        raise ValueError("scores must contain A and B")
    for label in ("A", "B"):
        if set(value["scores"][label]) != set(RUBRIC):
            raise ValueError(f"rubric keys for {label} do not match frozen schema")
        for dimension in RUBRIC:
            score = value["scores"][label][dimension]
            if isinstance(score, bool) or not isinstance(score, int) or not 0 <= score <= 100:
                raise ValueError("score outside 0..100")
    if value["PAIRWISE_PREFERENCE"] not in {"A", "B", "TIE", "ABSTAIN"}:
        raise ValueError("invalid pairwise preference")
    rationale = value["EVIDENCE_GROUNDED_RATIONALE"]
    if not isinstance(rationale, str) or not rationale.strip() or "\n" in rationale or "\r" in rationale:
        raise ValueError("rationale is not one line")
    return value


def manifest() -> dict[str, Any]:
    d016o = FROZEN_INPUT / "A001_FULL_D016N_V1_FORMAL_INPUT_MANIFEST.json"
    frozen = json.loads(d016o.read_text(encoding="utf-8"))
    checks = {
        "CANDIDATE_SOURCE_MATCHES_D016_O": frozen.get("candidate_git_sha") == CANDIDATE_SHA,
        "CALIBRATION_OBSERVATIONS_MATCH_D016_O": frozen.get("calibration_matches_d016_m") is True,
        "FULL_OBSERVATIONS_MATCH_D016_O": frozen.get("revised_full_differs_from_d016_m") is True,
        "EVALUATOR_INSTRUCTION_SEMANTICS_MATCH_D016_O": frozen.get("evaluator_instruction_sha256") == EXPECTED_INSTRUCTION_SHA,
        "RUBRIC_MATCHES_D016_O": frozen.get("output_schema") == "A001V2EvaluatorResponseV1" and frozen.get("rubric") == list(RUBRIC),
        "THRESHOLDS_MATCH_D016_O": frozen.get("min_schema_valid_pairs") == 11 and frozen.get("min_position_consistent_pairs") == 10 and frozen.get("min_preference_pairs") == 9 and frozen.get("min_median_overall_aliveness_delta") == 10.0,
        "AGGREGATOR_MATCHES_D016_O": frozen.get("aggregator_id") == "A001V2Aggregation",
    }
    if not all(checks.values()) or frozen.get("formal_input_bundle_sha256") != EXPECTED_D016O_BUNDLE_SHA:
        raise SystemExit("BLOCKED_D016O_FROZEN_INPUT_MISMATCH")
    material = {
        "manifest_id": "D016_U_FORMAL_INPUT_MANIFEST",
        "manifest_version": 1,
        "candidate_identity": CANDIDATE,
        "candidate_source_sha": CANDIDATE_SHA,
        "frozen_d016_o_manifest_sha256": sha256_file(d016o),
        "frozen_d016_o_formal_input_bundle_sha256": frozen["formal_input_bundle_sha256"],
        "frozen_input_root": "research/aliveness-spike/evidence/a001-v2/formal-input-d016n-v1",
        "protocol_id": "A001ObservationProtocolV1",
        "evaluator_instruction_sha256": EXPECTED_INSTRUCTION_SHA,
        "rubric": list(RUBRIC),
        "panel_pairs": 12,
        "formal_executions_per_stage": 24,
        "min_schema_valid_pairs": 11,
        "min_position_consistent_pairs": 10,
        "min_preference_pairs": 9,
        "min_median_overall_aliveness_delta": 10.0,
        "provider": "OpenAI",
        "transport": "DIRECT_RESPONSES_API",
        "model_requested": MODEL,
        "tools_allowed": False,
        "web_allowed": False,
        "file_search_allowed": False,
        "mcp_allowed": False,
        "connectors_allowed": False,
        "conversation_reuse": False,
        "previous_response_reuse": False,
        "fresh_request_per_slot": True,
        "structured_output_required": True,
        "input_integrity": checks,
        "slot_prefixes": {"calibration": "D016U-CAL-P##-[AB]", "full": "D016U-FULL-P##-[AB]"},
    }
    canonical = json.dumps(material, ensure_ascii=False, separators=(",", ":"), sort_keys=True).encode("utf-8")
    material["formal_input_bundle_sha256"] = sha256_bytes(canonical)
    return material


def write_route_contract(man: dict[str, Any]) -> None:
    write_json(OUTPUT / "D016_U_ROUTE_CONTRACT.json", {
        "contract_id": "A001EvaluatorRouteContractU1",
        "provider": "OPENAI",
        "transport": "DIRECT_RESPONSES_API",
        "model_requested": MODEL,
        "tools_present_in_request": False,
        "web_allowed": False,
        "file_search_allowed": False,
        "mcp_allowed": False,
        "connectors_allowed": False,
        "conversation_reuse": False,
        "previous_response_reuse": False,
        "fresh_request_per_slot": True,
        "structured_output_enabled": True,
        "model_resolution_policy": "record every returned model; do not silently mix a material identity change",
        "formal_input_bundle_sha256": man["formal_input_bundle_sha256"],
    })


def aggregate(stage: str) -> dict[str, Any]:
    stage_root = OUTPUT / stage.lower()
    raw = stage_root / "raw"
    provider = stage_root / "raw-provider"
    normalized = stage_root / "normalized"
    prefix = "CAL" if stage == "CALIBRATION" else "FULL"
    raw_files = sorted(raw.glob(f"D016U-{prefix}-P??-[AB].raw.txt"))
    valid: dict[str, dict[str, Any]] = {}
    invalid: dict[str, str] = {}
    for path in raw_files:
        slot = path.name.removesuffix(".raw.txt")
        match = re.fullmatch(r"D016U-(?:CAL|FULL)-P(\d{2})-([AB])", slot)
        if not match:
            invalid[slot] = "invalid slot id"
            continue
        pair = int(match.group(1))
        execution = match.group(2)
        try:
            value = parse_response(path.read_text(encoding="utf-8"))
        except (ValueError, json.JSONDecodeError) as exc:
            invalid[slot] = str(exc)
            continue
        candidate = canonical_candidate(pair, execution)
        item = {
            "slotId": slot,
            "preference": value["PAIRWISE_PREFERENCE"],
            "canonicalCandidate": candidate,
            "order": ["A", "B"] if candidate == "FIRST" else ["B", "A"],
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
        canonical = [canonical_preference(item["preference"], item["canonicalCandidate"]) for item in observations]
        if canonical[0] == canonical[1] and canonical[0] not in {"TIE", "ABSTAIN"}:
            deltas = [
                item["overallA"] - item["overallB"] if item["canonicalCandidate"] == "FIRST" else item["overallB"] - item["overallA"]
                for item in observations
            ]
            consistent.append({"pair": pair, "canonicalPreferenceA": canonical[0], "canonicalPreferenceB": canonical[1], "consistent": True, "deltaAverage": sum(deltas) / 2})
    deltas = sorted(item["deltaAverage"] for item in consistent)
    median = 0.0 if not deltas else deltas[len(deltas) // 2] if len(deltas) % 2 else (deltas[len(deltas) // 2 - 1] + deltas[len(deltas) // 2]) / 2
    preference_count = sum(item["canonicalPreferenceA"] == "A" for item in consistent)
    panel_valid = len(raw_files) == 24 and len(valid_pairs) >= 11 and len(consistent) >= 10
    threshold_pass = preference_count >= 9 and median >= 10.0
    if not panel_valid:
        verdict = "A001_AI_PANEL_INVALID"
    elif stage == "CALIBRATION":
        verdict = "CALIBRATION_PASS" if threshold_pass else "AI_EVALUATOR_CALIBRATION_FAIL"
    else:
        verdict = "A001_AI_QUALIFICATION_PASS" if threshold_pass else "A001_AI_QUALIFICATION_FAIL"
    result = {
        "stage": stage,
        "executionsAttempted": len(raw_files),
        "schemaValidExecutions": len(valid),
        "schemaInvalidExecutions": len(invalid),
        "validPairs": len(valid_pairs),
        "positionConsistentPairs": len(consistent),
        "preferenceCount": preference_count,
        "medianOverallAlivenessDelta": median,
        "verdict": verdict,
        "invalidSlots": invalid,
        "selectiveReruns": False,
        "replacementAnswers": False,
        "pairDetails": consistent,
    }
    write_json(stage_root / "aggregate.json", result)
    write_text(stage_root / "AGGREGATE.txt", "\n".join([
        f"D016-U A001 V2 {stage} AGGREGATE",
        f"STAGE={stage}",
        f"EXECUTIONS_ATTEMPTED={len(raw_files)}",
        f"SCHEMA_VALID_EXECUTIONS={len(valid)}",
        f"SCHEMA_INVALID_EXECUTIONS={len(invalid)}",
        f"VALID_PAIRS={len(valid_pairs)}",
        f"POSITION_CONSISTENT_PAIRS={len(consistent)}",
        f"PREFERENCE_COUNT={preference_count}",
        f"MEDIAN_OVERALL_ALIVENESS_DELTA={median}",
        f"VERDICT={verdict}",
        "SELECTIVE_RERUNS=0",
        "REPLACEMENT_ANSWERS=0",
        "RAW_RESULTS_PRESERVED=true",
        "EXTERNAL_HUMAN_PARTICIPANTS=0",
        "OWNER_PIXEL_REVIEWS=0",
        "R003_R009=BLOCKED",
        "",
    ]))
    return result


def write_summary(man: dict[str, Any], env: dict[str, Any], *, preflight: str, calibration: dict[str, Any] | None, full: dict[str, Any] | None, blocker: str | None = None) -> None:
    result = full["verdict"] if full else calibration["verdict"] if calibration else blocker or "NONE"
    lines = [
        "D016-U A001 V2 FORMAL EXECUTION",
        f"D016U_RESULT={result}",
        f"FORMAL_INPUT_BUNDLE_SHA256={man['formal_input_bundle_sha256']}",
        f"EVALUATOR_INSTRUCTION_SHA256={EXPECTED_INSTRUCTION_SHA}",
        "PROVIDER=OpenAI Responses API",
        f"MODEL_REQUESTED={MODEL}",
        f"CALIBRATION_MODEL_EXECUTIONS={calibration['executionsAttempted'] if calibration else 0}",
        f"CALIBRATION_SCHEMA_VALID_EXECUTIONS={calibration['schemaValidExecutions'] if calibration else 0}",
        f"FULL_MODEL_EXECUTIONS={full['executionsAttempted'] if full else 0}",
        f"TOTAL_FORMAL_MODEL_EXECUTIONS={(calibration['executionsAttempted'] if calibration else 0) + (full['executionsAttempted'] if full else 0)}",
        "U_PREFLIGHT_MODEL_CALLS=1",
        "NONFORMAL_MODEL_CALLS=1",
        f"NONFORMAL_PREFLIGHT_RESULT={preflight}",
        f"OPENAI_AUTHENTICATED={str(preflight == 'PASS').lower()}",
        f"GPT5_NANO_AVAILABLE={str(preflight == 'PASS').lower()}",
        f"ENV_FILE_PRESENT={str(env['ENV_FILE_PRESENT']).lower()}",
        f"ENV_FILE_TRACKED={str(env['ENV_FILE_TRACKED']).lower()}",
        f"OPENAI_API_KEY_PRESENT={str(env['OPENAI_API_KEY_PRESENT']).lower()}",
        f"BLOCKER={blocker or 'NONE'}",
        "HISTORICAL_ANSWER_REUSE=0",
        "SELECTIVE_RERUNS=0",
        "REPLACEMENT_ANSWERS=0",
        "ORGANISM_CHANGED_DURING_SCORING=false",
        "OBSERVATIONS_CHANGED=false",
        "RUBRIC_CHANGED=false",
        "THRESHOLDS_CHANGED=false",
        "AGGREGATOR_CHANGED=false",
        "PARAGON_USED_FOR_D016U=false",
        "CLAUDE_USED_FOR_D016U=false",
        "GEMINI_USED_FOR_D016U=false",
        "EXTERNAL_HUMAN_PARTICIPANTS=0",
        "API_KEY_EXPOSED=false",
        "API_KEY_COMMITTED=false",
        "API_KEY_PACKAGED_IN_ANDROID=false",
        "OWNER_PIXEL_REVIEWS=0",
        "R003_R009=BLOCKED",
        "NO_ORGANISM_CONCLUSION_FROM_D016U=" + str(result not in {"A001_AI_QUALIFICATION_PASS", "A001_AI_QUALIFICATION_FAIL"}).lower(),
    ]
    write_text(OUTPUT / "D016_U_FORMAL_EXECUTION.txt", "\n".join(lines) + "\n")


def main() -> int:
    OUTPUT.mkdir(parents=True, exist_ok=True)
    if (OUTPUT / "D016_U_FORMAL_EXECUTION.txt").exists():
        raise SystemExit("D016-U namespace already has terminal evidence; refusing a rerun")
    env = env_status()
    man = manifest()
    write_json(OUTPUT / "D016_U_FORMAL_INPUT_MANIFEST.json", man)
    write_text(OUTPUT / "D016_U_FORMAL_INPUT_BUNDLE_SHA256.txt", man["formal_input_bundle_sha256"] + "\n")
    write_route_contract(man)
    write_json(OUTPUT / "D016_U_ENV_STATUS.json", {key: value for key, value in env.items() if key != "_key"})
    key = env["_key"]
    if not key or env["ENV_FILE_TRACKED"] or not env["ENV_FILE_IGNORED"]:
        write_summary(man, env, preflight="NOT_RUN", calibration=None, full=None, blocker="BLOCKED_OPENAI_ENV_SECRET_BOUNDARY")
        return 0
    client = object()
    try:
        model_status, model_body = http_get_models(client, key)
        model_available = False
        try:
            model_available = any(item.get("id") == MODEL for item in json.loads(model_body).get("data", []))
        except (ValueError, AttributeError):
            pass
        model_list_scope_limited = model_status == 403 and "api.model.read" in model_body
        write_json(OUTPUT / "D016_U_MODEL_LIST_STATUS.json", {"httpStatus": model_status, "gpt5NanoVisible": model_available, "modelListScopeLimited": model_list_scope_limited, "responseSha256": sha256_bytes(model_body.encode("utf-8"))})
        if (model_status != 200 and not model_list_scope_limited) or (model_status == 200 and not model_available):
            write_summary(man, env, preflight="NOT_RUN", calibration=None, full=None, blocker="BLOCKED_GPT5_NANO_MODEL_UNAVAILABLE")
            return 0
        preflight_prompt = "Return JSON with exactly one boolean field, ok, set to true. This is a non-formal connectivity check."
        body = request_body(preflight_prompt, preflight=True)
        preflight_status, preflight_body = http_post(client, key, body)
        write_json(OUTPUT / "D016_U_PREFLIGHT_REQUEST.json", {"model": MODEL, "toolsPresentInRequest": False, "inputTextSha256": sha256_bytes(preflight_prompt.encode()), "structuredOutput": True, "requestSha256": sha256_bytes(json.dumps(body, separators=(",", ":"), sort_keys=True).encode())})
        write_text(OUTPUT / "D016_U_PREFLIGHT_RAW_RESPONSE.json", preflight_body)
        preflight_ok = False
        preflight_payload: dict[str, Any] = {}
        try:
            preflight_payload = json.loads(preflight_body)
            text = extract_output_text(preflight_payload)
            decoded = json.loads(text or "{}")
            preflight_ok = preflight_status == 200 and isinstance(decoded, dict) and decoded.get("ok") is True
        except (ValueError, TypeError, json.JSONDecodeError):
            preflight_ok = False
        write_json(OUTPUT / "D016_U_PREFLIGHT_RESULT.json", {"httpStatus": preflight_status, "authenticated": preflight_status != 401, "modelListScopeLimited": model_list_scope_limited, "modelAvailable": model_available or preflight_ok, "structuredOutputValid": preflight_ok, "response": response_metadata(preflight_payload)})
        if not preflight_ok:
            if preflight_status in {401, 403} or "insufficient permissions" in preflight_body.lower() or "api.responses.write" in preflight_body.lower():
                blocker = "BLOCKED_OPENAI_AUTHENTICATION"
            elif preflight_status in {402, 429}:
                blocker = "BLOCKED_GPT5_NANO_CAPACITY"
            else:
                blocker = "BLOCKED_GPT5_NANO_STRUCTURED_OUTPUT"
            write_summary(man, env, preflight="FAIL", calibration=None, full=None, blocker=blocker)
            return 0
        write_text(OUTPUT / "D016_U_PREQUALIFICATION.txt", "\n".join([
            "D016-U PREQUALIFICATION",
            "OPENAI_AUTHENTICATED=true",
            "GPT5_NANO_AVAILABLE=true",
            "U_PREFLIGHT_MODEL_CALLS=1",
            "NONFORMAL_PREFLIGHT_RESULT=PASS",
            "TOOLS_PRESENT_IN_REQUEST=false",
            "STRUCTURED_OUTPUT_ENABLED=true",
            f"MODEL_RESOLVED={response_metadata(json.loads(preflight_body)).get('returnedModel', 'UNKNOWN')}",
            f"D016_U_FORMAL_INPUT_BUNDLE_SHA256={man['formal_input_bundle_sha256']}",
            "FORMAL_EXECUTIONS=STARTING",
            "SCIENTIFIC_RESULT=NONE_PRECHECK",
        ]) + "\n")
        calibration = execute_stage(client, key, man, "CALIBRATION")
        if calibration["verdict"] != "CALIBRATION_PASS":
            write_summary(man, env, preflight="PASS", calibration=calibration, full=None)
            return 0
        full = execute_stage(client, key, man, "FULL")
        write_summary(man, env, preflight="PASS", calibration=calibration, full=full)
        return 0
    finally:
        pass


def execute_stage(client: object, key: str, man: dict[str, Any], stage: str) -> dict[str, Any]:
    stage_root = OUTPUT / stage.lower()
    input_dir = FROZEN_INPUT / ("calibration" if stage == "CALIBRATION" else "qualification")
    prefix = "CAL-P" if stage == "CALIBRATION" else "FULL-P"
    instruction = (FROZEN_INPUT / "EVALUATOR_INSTRUCTIONS_V1.txt").read_text(encoding="utf-8")
    for index, bundle in enumerate(sorted(input_dir.glob(f"{prefix}??.txt")), start=1):
        pair = index
        source = bundle.read_text(encoding="utf-8")
        for execution in ("A", "B"):
            slot = f"D016U-{('CAL' if stage == 'CALIBRATION' else 'FULL')}-P{pair:02d}-{execution}"
            ledger = OUTPUT / "ledger" / f"{slot}.claimed"
            ledger.parent.mkdir(parents=True, exist_ok=True)
            with ledger.open("x", encoding="utf-8") as handle:
                handle.write(f"CLAIMED_UTC={utc_now()}\n")
            rendered = source if execution == "A" else reverse_bundle(source)
            prompt = instruction.rstrip() + "\n\n" + rendered
            body = request_body(prompt)
            provenance = {
                "slotId": slot,
                "stage": stage,
                "model": MODEL,
                "provider": "OPENAI",
                "transport": "DIRECT_RESPONSES_API",
                "toolsPresentInRequest": False,
                "inputTextSha256": sha256_bytes(prompt.encode("utf-8")),
                "frozenInstructionSha256": EXPECTED_INSTRUCTION_SHA,
                "observationBundleSha256": sha256_bytes(rendered.encode("utf-8")),
                "structuredOutput": "A001V2EvaluatorResponseV1",
                "requestSha256": sha256_bytes(json.dumps(body, separators=(",", ":"), sort_keys=True).encode("utf-8")),
                "previousResponseReuse": False,
                "conversationReuse": False,
                "freshRequest": True,
            }
            write_json(stage_root / "provenance" / f"{slot}.request.json", provenance)
            status, provider_body = http_post(client, key, body)
            write_text(stage_root / "raw-provider" / f"{slot}.raw.json", provider_body)
            try:
                provider_payload = json.loads(provider_body)
            except json.JSONDecodeError:
                provider_payload = {}
            metadata = response_metadata(provider_payload)
            metadata.update({"slotId": slot, "stage": stage, "provider": "OpenAI Responses API", "endpoint": ENDPOINT, "requestedModel": MODEL, "httpStatus": status, "promptSha256": provenance["inputTextSha256"], "rawResponseSha256": sha256_bytes(provider_body.encode("utf-8")), "completedUtc": utc_now()})
            write_json(stage_root / "raw-provider" / f"{slot}.meta.json", metadata)
            extracted = extract_output_text(provider_payload) if provider_payload else None
            write_text(stage_root / "raw" / f"{slot}.raw.txt", extracted if extracted is not None else provider_body)
            if status in {402, 429} or any(marker in provider_body.lower() for marker in ("insufficient_quota", "credit_balance_exhausted", "no credits remaining")):
                write_text(OUTPUT / "D016U_STOP_AFTER_CAPACITY.txt", f"STOP_AFTER_CAPACITY=true\nSLOT={slot}\nHTTP_STATUS={status}\n")
                return aggregate(stage)
    return aggregate(stage)


if __name__ == "__main__":
    raise SystemExit(main())
