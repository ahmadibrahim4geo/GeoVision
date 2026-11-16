#!/usr/bin/env python3
"""Read-only ArcGIS Pro package compatibility comparison with official Esri JSON."""
import argparse
import importlib.metadata
import json
import re
import sys
from pathlib import Path

def normalized_name(value):
    return re.sub(r"[-_.]+", "-", str(value).strip()).casefold()

def installed_packages(snapshot=None):
    if snapshot is None:
        return {normalized_name(d.metadata["Name"]): d.version
                for d in importlib.metadata.distributions() if d.metadata.get("Name")}
    if not isinstance(snapshot, list):
        raise ValueError("pip list snapshot must be a JSON array")
    out = {}
    for row in snapshot:
        if not isinstance(row, dict) or not row.get("name") or not row.get("version"):
            raise ValueError("Each package requires name and version")
        out[normalized_name(row["name"])] = str(row["version"])
    return out

def compare(distribution, release, installed, packages=None):
    if not isinstance(distribution, dict) or release not in distribution:
        raise ValueError("Requested ArcGIS Pro release not in distribution JSON")
    mapping = distribution[release]
    if not isinstance(mapping, dict):
        raise ValueError("Release must contain package/version mapping")
    expected = {normalized_name(k): str(v) for k, v in mapping.items()
                if k.lower() != "arcgispro" and isinstance(v, (str, int, float))}
    if not expected:
        raise ValueError("Release contains no comparable package records")
    selected = set(expected) if packages is None else {normalized_name(p) for p in packages}
    if not selected:
        raise ValueError("No packages selected")
    unknown = sorted(selected - set(expected))
    if unknown:
        raise ValueError("Unknown packages in this release: " + ", ".join(unknown))
    return [{"name": name, "expected": expected[name], "actual": installed.get(name),
             "status": ("missing" if installed.get(name) is None
                        else "match" if installed[name] == expected[name] else "mismatch")}
            for name in sorted(selected)]

def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--distribution-json", type=Path, required=True,
                        help="Local copy of Esri/arcpy/docs/arcgis-dist.json")
    parser.add_argument("--release", required=True, help="Exact ArcGIS Pro version key")
    parser.add_argument("--installed-json", type=Path, help="Optional pip list --format=json file")
    parser.add_argument("--packages", help="Optional comma-separated package subset")
    args = parser.parse_args(argv)
    try:
        distribution = json.loads(args.distribution_json.read_text(encoding="utf-8"))
        snapshot = json.loads(args.installed_json.read_text(encoding="utf-8")) if args.installed_json else None
        selected = [x.strip() for x in args.packages.split(",") if x.strip()] if args.packages else None
        rows = compare(distribution, args.release, installed_packages(snapshot), selected)
    except (ValueError, OSError) as exc:
        print("INPUT ERROR: " + str(exc), file=sys.stderr)
        return 2
    for row in rows:
        print("{status:8s} {name}: expected={expected}, installed={actual}".format(**row))
    bad = sum(r["status"] != "match" for r in rows)
    print("ArcGIS Pro {}: {} checked / {} matching / {} missing or mismatched".format(
        args.release, len(rows), len(rows) - bad, bad))
    return 1 if bad else 0

if __name__ == "__main__":
    raise SystemExit(main())
