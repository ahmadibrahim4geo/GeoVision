#!/usr/bin/env python3
"""Validate offline Android/iOS GIS acceptance evidence without claiming device execution.

Usage: python scripts/validate_mobile_device_evidence.py path/to/report.json
"""
import argparse
import json
from pathlib import Path

VALID_STATUSES = {"PASS", "FAIL", "BLOCKED", "NOT_RUN"}
REQUIRED = {"case_id", "platform", "mode", "status"}
HARDWARE_CASES = {"MOB-03", "MOB-04", "MOB-06", "MOB-11", "MOB-15"}


def validate_report(report):
    errors = []
    if not isinstance(report, dict):
        return ["root must be a JSON object"]
    if report.get("schema_version") != "1.0":
        errors.append("schema_version must be 1.0")
    cases = report.get("cases")
    if not isinstance(cases, list) or not cases:
        return errors + ["cases must be a nonempty list"]
    seen = set()
    for i, row in enumerate(cases):
        prefix = "cases[{}]".format(i)
        if not isinstance(row, dict):
            errors.append(prefix + " must be an object")
            continue
        for key in REQUIRED:
            if key not in row:
                errors.append(prefix + " missing " + key)
        platform = row.get("platform")
        mode = row.get("mode")
        status = row.get("status")
        ident = row.get("case_id")
        if platform not in ("android", "ios"):
            errors.append(prefix + " unsupported platform")
        if mode not in ("physical", "emulator", "simulator", "ci-fixture"):
            errors.append(prefix + " invalid mode")
        if not isinstance(status, str) or status not in VALID_STATUSES:
            errors.append(prefix + " invalid status")
        if not isinstance(ident, str) or not ident.startswith("MOB-"):
            errors.append(prefix + " invalid case_id")
        elif isinstance(platform, str) and (platform, ident) in seen:
            errors.append(prefix + " duplicate platform/case_id")
        if isinstance(platform, str) and isinstance(ident, str):
            seen.add((platform, ident))
        if status == "NOT_RUN":
            if not isinstance(row.get("reason"), str) or not row["reason"].strip():
                errors.append(prefix + " NOT_RUN requires honest reason")
        elif status in {"FAIL", "BLOCKED"}:
            if not isinstance(row.get("reason"), str) or not row["reason"].strip():
                errors.append(prefix + " FAIL/BLOCKED requires reason")
        elif status == "PASS":
            for key in ("device", "os_version", "app_build_sha", "observed"):
                if not isinstance(row.get(key), str) or not row[key].strip():
                    errors.append(prefix + " PASS missing " + key)
            evidence = row.get("evidence")
            if not isinstance(evidence, list) or not evidence or not all(
                isinstance(item, str) and item.strip() for item in evidence
            ):
                errors.append(prefix + " PASS requires evidence references")
            if mode == "ci-fixture":
                errors.append(prefix + " fixture cannot PASS native device test")
            if platform == "android" and mode == "simulator":
                errors.append(prefix + " Android simulator label invalid (use emulator)")
            if platform == "ios" and mode == "emulator":
                errors.append(prefix + " iOS emulator label invalid (use simulator)")
            if isinstance(ident, str) and ident in HARDWARE_CASES and mode != "physical":
                errors.append(prefix + " hardware/privacy case requires physical device for PASS")
    return errors


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("path", type=Path)
    args = parser.parse_args()
    try:
        report = json.loads(args.path.read_text(encoding="utf-8"))
    except (OSError, ValueError) as exc:
        parser.error("cannot read report: " + str(exc))
    errors = validate_report(report)
    if errors:
        for error in errors:
            print("ERROR: " + error)
        return 1
    print("Mobile device evidence valid (status declarations checked, no native execution performed)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
