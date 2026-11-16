#!/usr/bin/env python3
"""Offline evidence coverage audit of versioned GIS source license records.

No network requests, legal conclusions or automatic external license detection.
"""
import argparse
import json
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CATALOG = ROOT / "references" / "project-catalog.json"


def classify(records):
    """Classify source-file inspection evidence; no inference from license string."""
    verified = [p for p in records if p.get("license_verified") is True]
    unverified = [p for p in records if p.get("license_verified") is not True]
    return {
        "total": len(records),
        "verified_source_licenses": len(verified),
        "unverified_source_licenses": len(unverified),
        "verified_by_category": dict(sorted(Counter(p.get("category", "uncategorized") for p in verified).items())),
    }


def scan(records):
    warnings = []
    for index, item in enumerate(records):
        if item.get("license_verified") is not True:
            continue
        for field in ("license_scope", "license_checked_on", "license_evidence_url", "license_caveat"):
            if not isinstance(item.get(field), str) or not item[field].strip():
                warnings.append("projects[{}] verified but lacks {}".format(index, field))
        url = item.get("license_evidence_url")
        if not isinstance(url, str) or not url.startswith("https://github.com/"):
            warnings.append("projects[{}] invalid license evidence URL".format(index))
    return warnings


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--catalog", type=Path, default=CATALOG)
    parser.add_argument("--summary", action="store_true")
    parser.add_argument("--unverified", action="store_true")
    parser.add_argument("--limit", type=int, default=25)
    parser.add_argument("--json", action="store_true")
    args = parser.parse_args(argv)
    if args.limit < 0:
        parser.error("--limit must be >= 0")
    try:
        data = json.loads(args.catalog.read_text(encoding="utf-8"))
        records = data["projects"]
        if not isinstance(records, list):
            raise ValueError("projects must be an array")
    except (ValueError, OSError, KeyError, TypeError) as exc:
        parser.error(str(exc))
    problems = scan(records)
    if problems:
        parser.error("; ".join(problems))
    counts = classify(records)
    if args.unverified:
        selected = [{"name":p.get("name"),"url":p.get("url"),"license":p.get("license")}
                    for p in records if p.get("license_verified") is not True][:args.limit]
        if args.json:
            print(json.dumps({"summary":counts,"unverified":selected}, ensure_ascii=False, indent=2))
        else:
            for x in selected:
                print("{} | {} | {}".format(x["name"], x["license"], x["url"]))
            print("Shown {} unverified entries; actual source rights are unknown".format(len(selected)))
    elif args.json:
        print(json.dumps(counts, ensure_ascii=False, indent=2))
    else:
        print("Audited file evidence: {}/{} project source licenses; {} unverified".format(
            counts["verified_source_licenses"], counts["total"], counts["unverified_source_licenses"]))
        print("Hosted APIs, model weights, datasets and commercial GIS host licenses are separate.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
