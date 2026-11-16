#!/usr/bin/env python3
"""Read-only endpoint topology screening for WGS84 GeoJSON LineStrings.

Does NOT node intersections, infer legitimate terminal nodes, or perform
complete network topology validation. Python 3.11+ standard library.
"""
import argparse
import json
from collections import defaultdict
from pathlib import Path


def xy(coord, decimals):
    if len(coord) < 2:
        raise ValueError("A vertex must contain longitude and latitude")
    return (round(float(coord[0]), decimals), round(float(coord[1]), decimals))


def audit(collection, decimals=8):
    if not isinstance(decimals, int) or not 0 <= decimals <= 12:
        raise ValueError("decimals must be between 0 and 12")
    if collection.get("type") != "FeatureCollection":
        raise ValueError("Expected GeoJSON FeatureCollection")
    seen = {}
    degree = defaultdict(int)
    duplicates = []
    unique = 0
    for position, feature in enumerate(collection.get("features", [])):
        geometry = feature.get("geometry") or {}
        if geometry.get("type") != "LineString":
            raise ValueError("Only LineString geometry is supported")
        raw = geometry.get("coordinates", [])
        if len(raw) < 2:
            raise ValueError("LineString must have at least 2 points")
        vertices = tuple(xy(point, decimals) for point in raw)
        if len(set(vertices)) < 2:
            raise ValueError("Degenerate LineString")
        identifier = str(feature.get("id", position))
        direction_independent = min(vertices, tuple(reversed(vertices)))
        if direction_independent in seen:
            duplicates.append({"id": identifier, "same_as": seen[direction_independent]})
            continue
        seen[direction_independent] = identifier
        degree[vertices[0]] += 1
        degree[vertices[-1]] += 1
        unique += 1
    dangles = [{"coordinate": list(p), "degree": value}
               for p, value in sorted(degree.items()) if value == 1]
    return {"unique_lines": unique, "duplicates": duplicates, "dangles": dangles,
            "limitations": "Rounded WGS84 endpoints only; no crossing noding, metric tolerance or automatic repair."}


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--decimals", type=int, default=8)
    args = parser.parse_args(argv)
    if args.input.resolve() == args.output.resolve():
        parser.error("Output must not overwrite source")
    result = audit(json.loads(args.input.read_text(encoding="utf-8")), args.decimals)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print("Unique lines: {}, duplicates: {}, dangling endpoints: {}".format(
        result["unique_lines"], len(result["duplicates"]), len(result["dangles"])))


if __name__ == "__main__":
    main()
