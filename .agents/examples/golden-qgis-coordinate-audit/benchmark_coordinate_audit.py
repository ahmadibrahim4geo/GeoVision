#!/usr/bin/env python3
"""Measure the offline audit core with generated Point features."""

import argparse
import hashlib
import json
import time

from audit_core import MAX_FEATURES, audit_document


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--features", type=int, default=10_000)
    args = parser.parse_args(argv)
    if not 1 <= args.features <= MAX_FEATURES:
        parser.error("--features must be between 1 and {}".format(MAX_FEATURES))

    features = [
        {
            "type": "Feature",
            "id": "point-{:06d}".format(index),
            "geometry": {
                "type": "Point",
                "coordinates": [30.0 + (index % 1000) / 1000, 29.0 + (index % 500) / 1000],
            },
            "properties": {"sequence": index},
        }
        for index in range(args.features)
    ]
    document = {
        "type": "FeatureCollection",
        "crs": {"type": "name", "properties": {"name": "EPSG:4326"}},
        "features": features,
    }
    started = time.perf_counter()
    artifact, summary = audit_document(document, "generated-benchmark.geojson", hashlib.sha256(b"generated").hexdigest())
    elapsed = time.perf_counter() - started
    if summary["accepted_feature_count"] != args.features or len(artifact["features"]) != args.features:
        raise RuntimeError("Benchmark correctness check failed")
    report = {
        "schema_version": "1.0",
        "evidence_tier": "T1",
        "scope": "generated in-memory Point features; not native QGIS",
        "feature_count": args.features,
        "elapsed_ms": round(elapsed * 1000, 3),
        "features_per_second": round(args.features / elapsed, 1),
        "correctness_check": "pass",
    }
    print(json.dumps(report, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
