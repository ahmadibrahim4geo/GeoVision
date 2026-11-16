#!/usr/bin/env python3
"""Measure synthetic endpoint screening only; not a licensed GIS benchmark."""
import argparse
import importlib.util
import json
from pathlib import Path
import statistics
import time
import tracemalloc

ROOT = Path(__file__).resolve().parents[1]


def measure(samples=5):
    spec = importlib.util.spec_from_file_location("net", ROOT / "examples/network-topology/audit_network.py")
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    fixture = json.loads((ROOT / "examples/network-topology/fixtures/network.geojson").read_text(encoding="utf-8"))
    durations, peaks = [], []
    for _ in range(samples):
        tracemalloc.start()
        before = time.perf_counter()
        result = mod.audit(fixture)
        durations.append(time.perf_counter() - before)
        _, peak = tracemalloc.get_traced_memory()
        peaks.append(peak)
        tracemalloc.stop()
        if (result["unique_lines"], len(result["duplicates"]), len(result["dangles"])) != (3, 1, 4):
            raise AssertionError("Mismatch with expected spatial output")
    return {"fixture": "synthetic 4-line GeoJSON", "runs": samples,
            "median_wall_seconds": statistics.median(durations),
            "peak_python_allocated_bytes": max(peaks),
            "expected_result": "3 unique, 1 duplicate, 4 dangles",
            "tier": "T1", "native_gis_verified": False}


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--runs", type=int, default=5)
    args = parser.parse_args(argv)
    if not 1 <= args.runs <= 100:
        parser.error("runs must be between 1 and 100")
    print(json.dumps(measure(args.runs), indent=2))


if __name__ == "__main__":
    main()
