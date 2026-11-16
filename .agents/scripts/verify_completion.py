#!/usr/bin/env python3
"""DoneProof report-consistency checker. Does not execute or authenticate evidence."""
import argparse
import json
from pathlib import Path

STATES = {"verified", "partial", "failed", "blocked", "not_run"}
MAX_REPORT_BYTES = 262_144
KINDS = {"command_output", "artifact_readback", "provider_receipt", "native_interaction", "manual_review"}


def nonempty(x):
    return isinstance(x, str) and bool(x.strip())


def validate(data):
    errors = []
    if not isinstance(data, dict):
        return ["Report must be an object"]
    if data.get("schema_version") != "1.0":
        errors.append("Invalid schema_version")
    for key in ("task_id", "revision"):
        if not nonempty(data.get(key)):
            errors.append(key + " must be nonempty")
    overall = data.get("overall_status")
    if not isinstance(overall, str) or overall not in STATES:
        errors.append("Invalid overall_status")
    items = data.get("criteria")
    if not isinstance(items, list) or not items:
        return errors + ["criteria must be a nonempty list"]
    ids, statuses = set(), []
    for i, item in enumerate(items):
        prefix = "criteria[{}]".format(i)
        if not isinstance(item, dict):
            errors.append(prefix + " must be an object")
            continue
        key = item.get("id")
        if not nonempty(key) or key in ids:
            errors.append(prefix + ": missing or duplicate id")
        else:
            ids.add(key)
        if not nonempty(item.get("statement")):
            errors.append(prefix + ": missing statement")
        status = item.get("status")
        if not isinstance(status, str) or status not in STATES:
            errors.append(prefix + ": invalid status")
            continue
        statuses.append(status)
        observations = item.get("evidence")
        if not isinstance(observations, list):
            errors.append(prefix + ": evidence must be a list")
            continue
        if status in {"partial", "blocked", "not_run"} and not nonempty(item.get("reason")):
            errors.append(prefix + ": missing incomplete reason")
        if status == "not_run" and observations:
            errors.append(prefix + ": not_run cannot have evidence")
        passed = failed = 0
        for j, obs in enumerate(observations):
            name = "{}.evidence[{}]".format(prefix, j)
            if not isinstance(obs, dict):
                errors.append(name + ": must be an object")
                continue
            if not isinstance(obs.get("type"), str) or obs.get("type") not in KINDS:
                errors.append(name + ": unsupported type")
            for field in ("reference", "observed"):
                if not nonempty(obs.get(field)):
                    errors.append(name + ": missing " + field)
            if not nonempty(obs.get("revision")) or obs.get("revision") != data.get("revision"):
                errors.append(name + ": stale evidence revision")
            if obs.get("result") == "pass":
                passed += 1
            elif obs.get("result") == "fail":
                failed += 1
            else:
                errors.append(name + ": invalid result")
        if status == "verified" and (passed < 1 or failed > 0):
            errors.append(prefix + ": unsupported verified claim")
        if status == "failed" and failed < 1:
            errors.append(prefix + ": failed claim lacks failure observation")
    if len(statuses) == len(items) and isinstance(overall, str) and overall in STATES:
        expected = ("failed" if "failed" in statuses else
                    "verified" if all(v == "verified" for v in statuses) else
                    "not_run" if all(v == "not_run" for v in statuses) else
                    "blocked" if all(v == "blocked" for v in statuses) else "partial")
        if overall != expected:
            errors.append("overall_status must be " + expected + " for the criteria provided")
    return errors

def validate_against_plan(report, plan):
    """Bind report to original plan criteria; does not verify external execution."""
    errors = []
    if not isinstance(report, dict) or not isinstance(plan, dict):
        return ["Report and plan must be JSON objects"]
    for field in ("request_id", "revision"):
        if not nonempty(report.get(field)) or report.get(field) != plan.get(field):
            errors.append("Plan binding: " + field + " missing or mismatched")
    tasks = plan.get("tasks")
    if not isinstance(tasks, list):
        return errors + ["Plan binding: tasks must be a list"]
    matches = [t for t in tasks if isinstance(t, dict) and t.get("id") == report.get("task_id")]
    if len(matches) != 1:
        return errors + ["Plan binding: task_id must select exactly one plan task"]
    expected = matches[0].get("acceptance_criteria")
    if not isinstance(expected, list) or not expected or any(not nonempty(v) for v in expected):
        return errors + ["Plan binding: task acceptance_criteria must be nonempty strings"]
    if len(set(expected)) != len(expected):
        errors.append("Plan binding: duplicate acceptance criteria in plan")
    criteria = report.get("criteria")
    if not isinstance(criteria, list) or any(not isinstance(c, dict) for c in criteria):
        return errors + ["Plan binding: report criteria invalid"]
    observed = [c.get("statement") for c in criteria]
    if any(not nonempty(v) for v in observed):
        return errors + ["Plan binding: report has invalid criterion statements"]
    if len(observed) != len(expected) or set(observed) != set(expected):
        errors.append("Plan binding: report omits, adds, or changes acceptance criteria")
    return errors


def unique_pairs(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError("Duplicate JSON key: " + key)
        result[key] = value
    return result


def reject_constant(value):
    raise ValueError("Non-finite JSON value: " + value)


def read_json(path):
    if not path.is_file() or path.stat().st_size > MAX_REPORT_BYTES:
        raise ValueError("Missing or oversized JSON input")
    return json.loads(path.read_text(encoding="utf-8-sig"),
                      object_pairs_hook=unique_pairs, parse_constant=reject_constant)


def main(argv=None):
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("report", type=Path)
    p.add_argument("--require-verified", action="store_true")
    p.add_argument("--plan", type=Path, help="Original reviewed intent plan: detect omitted criteria")
    args = p.parse_args(argv)
    try:
        data = read_json(args.report)
        plan = read_json(args.plan) if args.plan else None
    except (OSError, ValueError) as exc:
        print("ERROR:", exc)
        return 1
    errors = validate(data)
    if args.plan:
        errors.extend(validate_against_plan(data, plan))
    if args.require_verified and not args.plan:
        errors.append("Release gate requires --plan to prevent omitted acceptance criteria")
    if args.require_verified and data.get("overall_status") != "verified":
        errors.append("Release gate: overall_status is not verified")
    if errors:
        for error in errors:
            print("ERROR:", error)
        return 1
    print("DoneProof report structure valid; status={}; observations NOT independently checked".format(
        data["overall_status"]))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
