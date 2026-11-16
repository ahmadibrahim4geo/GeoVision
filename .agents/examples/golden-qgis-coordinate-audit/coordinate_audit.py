#!/usr/bin/env python3
"""Command-line entry point for the golden coordinate audit workflow."""

import argparse
import json
import sys
from pathlib import Path

from audit_core import AuditError, run_audit


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("input", type=Path, help="EPSG:4326 GeoJSON FeatureCollection")
    parser.add_argument("output_directory", type=Path, help="New output directory")
    args = parser.parse_args(argv)
    try:
        summary = run_audit(args.input, args.output_directory)
    except AuditError as exc:
        print("ERROR: {}".format(exc), file=sys.stderr)
        return 2
    print(json.dumps(summary, ensure_ascii=False, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
