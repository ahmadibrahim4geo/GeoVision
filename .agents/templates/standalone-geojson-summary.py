#!/usr/bin/env python3
"""Lightweight, read-only GeoJSON feature counter (not a GIS geometry validator)."""
import argparse
import collections
import json
from pathlib import Path


def summary(path):
    data = json.loads(Path(path).read_text(encoding='utf-8'))
    if data.get('type') != 'FeatureCollection':
        raise ValueError('Expected GeoJSON FeatureCollection')
    counts = collections.Counter()
    features = data.get('features', [])
    if not isinstance(features, list):
        raise ValueError('GeoJSON features must be an array')
    for feature in features:
        geom = feature.get('geometry') or {}
        counts[geom.get('type', 'null')] += 1
    return {'features': len(features), 'geometry_types': dict(sorted(counts.items()))}


if __name__ == '__main__':
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument('input_geojson')
    args = ap.parse_args()
    print(json.dumps(summary(args.input_geojson), indent=2, ensure_ascii=False))