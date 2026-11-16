#!/usr/bin/env python3
"""Offline capability candidate planner; never probes hardware or requests permissions."""
import argparse
import json
from pathlib import Path

REGISTRY = Path(__file__).resolve().parents[1] / "references/system-capability-registry.json"
PLATFORMS = ("windows", "qt", "browser", "electron", "tauri", "linux", "apple", "android", "python", "unknown")


def load_registry(path=REGISTRY):
    data = json.loads(Path(path).read_text(encoding="utf-8"))
    valid = set(data["capabilities"])
    used = set()
    if not valid:
        raise ValueError("Empty capability registry")
    for adapter in data["adapters"]:
        if adapter["id"] in used or adapter["platform"] not in PLATFORMS:
            raise ValueError("Duplicate ID or unknown platform")
        used.add(adapter["id"])
        if not set(adapter["capabilities"]) <= valid or adapter["review_status"] != "candidate_unverified":
            raise ValueError("Invalid capability or unearned adapter verification claim")
        if not adapter["url"].startswith("https://"):
            raise ValueError("Untrusted reference scheme")
    return data


def plan(platform, capabilities, registry=None):
    if platform not in PLATFORMS:
        raise ValueError("Unknown platform")
    registry = registry if registry is not None else load_registry()
    selected = list(dict.fromkeys(capabilities))
    if not selected or set(selected) - set(registry["capabilities"]):
        raise ValueError("Empty or unknown requested capability")
    result = []
    for cap in selected:
        spec = registry["capabilities"][cap]
        candidates = [a["id"] for a in registry["adapters"]
                      if a["platform"] == platform and cap in a["capabilities"]]
        result.append({
            "capability_id": cap,
            "availability": "CANDIDATE" if candidates else "UNASSESSED",
            "permission_status": "NOT_REQUIRED" if spec["permission"] == "not_required" else "NOT_REQUESTED",
            "candidate_adapters": candidates,
            "fallback": spec["fallback"],
            "native_verified": False,
            "test_status": "NOT RUN",
        })
    return {"schema_version": "1.0", "target_platform": platform,
            "purpose": "offline planning only: no OS/device access or permission granted",
            "capabilities": result}


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--platform", choices=PLATFORMS, required=True)
    parser.add_argument("--capability", action="append", required=True)
    args = parser.parse_args(argv)
    try:
        print(json.dumps(plan(args.platform, args.capability), indent=2, ensure_ascii=False))
    except (ValueError, OSError, KeyError) as error:
        parser.error(str(error))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
