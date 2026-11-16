#!/usr/bin/env python3
"""Validate a synthetic or app-emitted GIS data-flow trace without executing GIS.

This is a structural order/status/declared-invariants validator; it does not
authenticate logs, open geospatial files, verify coordinate transforms or prove
that the UI rendered anything. Native readback remains separately required.
"""
import argparse
import json
import re
from pathlib import Path

PHASES = ("IMPORT", "PARSE", "CRS", "PROCESS", "ENGINE", "RENDER",
          "SAVE", "EXPORT", "READBACK")
STATUSES = ("PASS", "FAIL", "BLOCKED", "NOT RUN")
OPERATIONS = ("lossless", "reproject", "filter", "clip", "aggregate", "simplify", "render-only")
TRACE_FIELDS = {"schema_version", "trace_id", "command_id", "task_id", "dataset_id",
                "operation", "required_phases", "events"}
EVENT_FIELDS = {"sequence", "phase", "status", "feature_count", "crs",
                "artifact_id", "error_code", "duration_ms"}
OPAQUE_ID = re.compile(r"^[a-zA-Z0-9_.:-]{1,96}$")
MAX_TRACE_BYTES = 512_000
MAX_EVENTS = 250


def unique_pairs(items):
    out = {}
    for key, value in items:
        if key in out:
            raise ValueError(f"Duplicate JSON key: {key}")
        out[key] = value
    return out


def is_number(value):
    return type(value) in (int, float) and 0 <= value < 1e12


def validate_trace(trace):
    issues = []
    if not isinstance(trace, dict):
        return {"kind": "offline_gis_trace_validation", "valid": False,
                "issues": ["Trace must be a JSON object"], "phase_summary": {},
                "native_readback_verified": False, "render_verified": False}
    unknown = set(trace) - TRACE_FIELDS
    if unknown:
        issues.append("Unsupported top-level fields (potential sensitive data): " + ", ".join(sorted(unknown)))
    if trace.get("schema_version") != "1.0":
        issues.append("schema_version must be 1.0")
    for name in ("trace_id", "command_id", "task_id", "dataset_id"):
        if not isinstance(trace.get(name), str) or not OPAQUE_ID.fullmatch(trace[name]):
            issues.append(f"{name} must be a non-sensitive opaque identifier")
    operation = trace.get("operation")
    if operation not in OPERATIONS:
        issues.append("Unsupported operation or no operation declared")
    required = trace.get("required_phases")
    if not isinstance(required, list) or not required or any(p not in PHASES for p in required):
        issues.append("required_phases must be a nonempty list of known phases")
        required = []
    elif len(required) != len(set(required)):
        issues.append("required_phases contains duplicate phases")
    if any(PHASES.index(a) >= PHASES.index(b) for a, b in zip(required, required[1:])):
        issues.append("required_phases must follow chronological GIS phase order")
    events = trace.get("events")
    if not isinstance(events, list) or not events or len(events) > MAX_EVENTS:
        issues.append(f"events must contain between 1 and {MAX_EVENTS} objects")
        events = []
    phase_counts, seen, previous_seq, previous_index = {}, set(), 0, -1
    for i, item in enumerate(events):
        field = f"events[{i}]"
        if not isinstance(item, dict):
            issues.append(field + " must be a JSON object")
            continue
        unknown_event = set(item) - EVENT_FIELDS
        if unknown_event:
            issues.append(field + " contains unsupported/sensitive fields: " +
                          ", ".join(sorted(unknown_event)))
        seq = item.get("sequence")
        if type(seq) is not int or seq <= previous_seq:
            issues.append(field + " sequence must be a strictly increasing positive integer")
        else:
            previous_seq = seq
        phase = item.get("phase")
        if phase not in PHASES:
            issues.append(field + " contains unknown phase")
            continue
        pos = PHASES.index(phase)
        if pos < previous_index:
            issues.append(field + " has a phase out of order")
        previous_index = pos
        if phase in seen:
            issues.append(field + " duplicates a phase (merge repeated spans upstream)")
        seen.add(phase)
        status = item.get("status")
        if status not in STATUSES:
            issues.append(field + " contains invalid status")
        elif status != "PASS":
            issues.append(f"{field} ({phase}) is {status}; trace cannot pass")
        if "feature_count" in item:
            count = item["feature_count"]
            if type(count) is not int or count < 0:
                issues.append(field + " feature_count must be a nonnegative integer")
            else:
                phase_counts[phase] = count
        if "duration_ms" in item and not is_number(item["duration_ms"]):
            issues.append(field + " duration_ms must be a finite nonnegative number")
        for field_name in ("crs", "artifact_id", "error_code"):
            if field_name in item and (not isinstance(item[field_name], str)
                                        or len(item[field_name]) > 128):
                issues.append(field + " " + field_name + " must be a short string")
    for phase in required:
        if phase not in seen:
            issues.append("Missing required phase: " + phase)
    if operation == "lossless":
        # A lossless claim must include actual input and output readback spans;
        # an emitter must not make an incomplete trace pass by omitting them
        # from its self-declared required_phases.
        for mandatory in ("IMPORT", "READBACK"):
            if mandatory not in required:
                issues.append("Lossless trace must require " + mandatory)
        exported = next((v for v in events if isinstance(v, dict) and v.get("phase") == "EXPORT"), None)
        reopened = next((v for v in events if isinstance(v, dict) and v.get("phase") == "READBACK"), None)
        if exported and reopened and exported.get("artifact_id") and reopened.get("artifact_id"):
            if exported["artifact_id"] != reopened["artifact_id"]:
                issues.append("EXPORT and READBACK artifact_id disagree")
        count_phases = [p for p in ("IMPORT", "PARSE", "ENGINE", "READBACK")
                        if p in seen]
        if "IMPORT" in seen and "READBACK" in seen:
            if "IMPORT" not in phase_counts or "READBACK" not in phase_counts:
                issues.append("Lossless workflow needs feature counts on IMPORT and READBACK")
            elif phase_counts["IMPORT"] != phase_counts["READBACK"]:
                issues.append("Lossless import/readback feature counts disagree")
        values = {phase_counts[p] for p in count_phases if p in phase_counts}
        if len(values) > 1:
            issues.append("Lossless workflow feature_count changed between recorded stages")
    result = {
        "kind": "offline_gis_trace_validation",
        "valid": not issues,
        "trace_id": trace.get("trace_id"),
        "command_id": trace.get("command_id"),
        "operation": operation,
        "phase_summary": {p: next((x["status"] for x in events
                                  if isinstance(x, dict) and x.get("phase") == p), "MISSING")
                          for p in required},
        "feature_counts": phase_counts,
        "issues": issues,
        "native_readback_verified": False,
        "render_verified": False,
        "limitations": "Trace order/count are checked offline. Real GIS data and native GUI were not opened.",
    }
    return result


def main(argv=None):
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--trace", required=True, help="Local JSON file with an app's sanitized trace")
    p.add_argument("--format", choices=("json", "text"), default="json")
    args = p.parse_args(argv)
    try:
        path = Path(args.trace)
        if not path.is_file() or path.stat().st_size > MAX_TRACE_BYTES:
            p.error(f"Trace missing or over {MAX_TRACE_BYTES} bytes")
        trace = json.loads(path.read_text(encoding="utf-8-sig"), object_pairs_hook=unique_pairs)
    except (OSError, UnicodeError, ValueError) as exc:
        p.error("Invalid or unsafe JSON trace: " + str(exc))
    result = validate_trace(trace)
    if args.format == "json":
        print(json.dumps(result, indent=2, ensure_ascii=False))
    else:
        print("Offline trace {}: {}".format(result.get("trace_id"), "PASS" if result["valid"] else "FAIL"))
        for error in result["issues"]:
            print("  " + error)
        print(result.get("limitations", ""))
    return 0 if result["valid"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
