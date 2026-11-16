#!/usr/bin/env python3
"""Validate the canonical T0-T4 registry and reject offline-as-native drift."""
import argparse
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REGISTRY = ROOT / "references" / "evidence-tiers.json"
SCAN_DIRS = ("skills", "references", "examples", "docs", "templates")
ROOT_MARKDOWN = ("README.md", "README.ar.md", "CONTRIBUTING.md", "PUBLISHING.md")

# T2 is reserved for an observed named native/runtime/provider integration.
# These patterns catch the historical drift where local fixtures were called T2.
FORBIDDEN_T2 = (
    re.compile(r"\bT2\b\s*(?:(?:is|=|:|[-—])\s*)?(?:an?\s+)?"
               r"(?:offline|synthetic|static|mock|fixtures?|fixture[- ](?:only|driven))\b", re.I),
    re.compile(r"\b(?:offline|synthetic|static|mock)\b\s+"
               r"(?:fixture[- ]only\s+(?:evidence\s*)?|fixture\s+)?(?:is\s+)?[:=-]?\s*\bT2\b", re.I),
    re.compile(r"\bT0\s*[–-]\s*T2\b[^\n]{0,32}\b(?:static|offline)\b", re.I),
    re.compile(r"\bT0\s*[–-]\s*T2\b[^\n]{0,32}\bnot\s+native\b", re.I),
    re.compile(r"\bT1\s*/\s*T2\b[^\n]{0,32}\b(?:local|planner|offline|fixture)\b", re.I),
)


def validate_registry(data):
    errors = []
    if data.get("schema_version") != "1.0":
        errors.append("evidence tier registry schema_version must be 1.0")
    tiers = data.get("tiers")
    if not isinstance(tiers, list):
        return errors + ["evidence tier registry tiers must be an array"]
    expected = ["T0", "T1", "T2", "T3", "T4"]
    found = [tier.get("id") for tier in tiers if isinstance(tier, dict)]
    if found != expected:
        errors.append("evidence tier registry must define T0-T4 in order")
    for ordinal, tier in enumerate(tiers):
        if not isinstance(tier, dict):
            errors.append("tier {} must be an object".format(ordinal))
            continue
        if tier.get("ordinal") != ordinal:
            errors.append("{} ordinal must be {}".format(tier.get("id", "tier"), ordinal))
        for field in ("name", "required_evidence", "does_not_establish"):
            if not isinstance(tier.get(field), str) or not tier[field].strip():
                errors.append("{} {} must be nonempty text".format(tier.get("id", "tier"), field))
        for field in ("native_runtime_required", "output_readback_required", "clean_environment_required"):
            if not isinstance(tier.get(field), bool):
                errors.append("{} {} must be boolean".format(tier.get("id", "tier"), field))
    return errors


def load_registry(path=REGISTRY):
    data = json.loads(Path(path).read_text(encoding="utf-8"))
    return data, validate_registry(data)


def iter_markdown(root):
    root = Path(root)
    for relative in ROOT_MARKDOWN:
        path = root / relative
        if path.is_file():
            yield path
    for directory in SCAN_DIRS:
        base = root / directory
        if base.is_dir():
            yield from base.rglob("*.md")


def scan_text(text, label="document"):
    errors = []
    for number, line in enumerate(text.splitlines(), 1):
        if any(pattern.search(line) for pattern in FORBIDDEN_T2):
            errors.append("{}:{}: offline/static/synthetic evidence cannot be labeled T2".format(label, number))
    return errors


def scan_repository(root=ROOT):
    root = Path(root).resolve()
    registry = root / "references" / "evidence-tiers.json"
    _, errors = load_registry(registry)
    for path in iter_markdown(root):
        errors.extend(scan_text(path.read_text(encoding="utf-8-sig"), str(path.relative_to(root))))
    return errors


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=ROOT)
    args = parser.parse_args(argv)
    errors = scan_repository(args.root)
    for error in errors:
        print("ERROR:", error)
    if errors:
        return 1
    print("Evidence tiers valid: T0-T4 registry and documentation labels are consistent")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
