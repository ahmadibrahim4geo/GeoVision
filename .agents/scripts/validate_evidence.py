#!/usr/bin/env python3
"""Validate GIS acceptance claims; stdlib, fails closed on missing evidence."""
import argparse
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
TIER_REGISTRY = ROOT / "references" / "evidence-tiers.json"
REQUIRED = ("target", "os", "application_version", "python_runtime", "crs_units",
            "evidence_tier", "commands_executed", "fixtures_used", "expected_result",
            "observed_result", "not_tested", "known_limitations", "source_changes",
            "provenance_licensing", "status", "native_runtime_verified", "evidence_links")


def load_levels(path=TIER_REGISTRY):
    registry = json.loads(Path(path).read_text(encoding="utf-8"))
    tiers = registry.get("tiers", [])
    levels = {tier["id"]: tier["ordinal"] for tier in tiers
              if isinstance(tier, dict) and "id" in tier and "ordinal" in tier}
    if levels != {"T0": 0, "T1": 1, "T2": 2, "T3": 3, "T4": 4}:
        raise ValueError("Invalid canonical evidence tier registry")
    return levels


LEVELS = load_levels()
STATES = {"pass", "partial", "blocked", "fail", "not tested"}


def validate(data):
    errors = []
    if not isinstance(data, dict):
        return ["Evidence report must be a JSON object"]
    for name in REQUIRED:
        if name not in data:
            errors.append("Missing field: " + name)
    if errors:
        return errors
    for name in ("target", "os", "application_version", "python_runtime", "crs_units",
                 "expected_result", "observed_result", "source_changes", "provenance_licensing"):
        if not isinstance(data[name], str) or not data[name].strip():
            errors.append(name + " must contain nonempty text")
    for name in ("commands_executed", "fixtures_used", "not_tested", "known_limitations", "evidence_links"):
        if not isinstance(data[name], list) or any(not isinstance(x, str) or not x.strip() for x in data[name]):
            errors.append(name + " must be a list of nonempty strings")
    if data["evidence_tier"] not in LEVELS:
        errors.append("Invalid evidence tier")
    if data["status"] not in STATES:
        errors.append("Invalid status")
    if not isinstance(data["native_runtime_verified"], bool):
        errors.append("native_runtime_verified must be boolean")
    tier = LEVELS.get(data["evidence_tier"], -1)
    if tier >= 2 and not data["native_runtime_verified"]:
        errors.append("T2+ requires verified native runtime")
    if tier >= 2 and not data["evidence_links"]:
        errors.append("T2+ requires actual log/artifact evidence_links")
    if tier >= 1 and not data["commands_executed"]:
        errors.append("T1+ requires commands actually executed")
    if tier >= 1 and not data["fixtures_used"]:
        errors.append("T1+ requires fixture evidence")
    if tier >= 3 and not data["observed_result"]:
        errors.append("T3+ requires observed result")
    if data["status"] == "pass" and ("not tested" in data["observed_result"].lower() or
                                      "verify current" in data["observed_result"].lower()):
        errors.append("Cannot claim pass from unverified results")
    if data["status"] == "pass" and (tier < 1 or not data["commands_executed"]):
        errors.append("pass requires executed verification")
    return errors


def main(argv=None):
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("path", type=Path)
    a = p.parse_args(argv)
    try:
        report = json.loads(a.path.read_text(encoding="utf-8"))
    except (OSError, ValueError) as exc:
        print("ERROR: " + str(exc))
        return 1
    problems = validate(report)
    for problem in problems:
        print("ERROR: " + problem)
    if problems:
        return 1
    print("Evidence contract valid; tier={}, status={}".format(report["evidence_tier"], report["status"]))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
