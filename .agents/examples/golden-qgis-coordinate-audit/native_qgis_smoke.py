#!/usr/bin/env python3
"""Run the golden workflow through QGIS Processing and emit native evidence."""

import argparse
import hashlib
import json
from pathlib import Path
import sys

from qgis.core import Qgis, QgsApplication
from processing.core.Processing import Processing
import processing

ROOT = Path(__file__).resolve().parent
ALGORITHM_DIR = ROOT / "qgis_processing"
if str(ALGORITHM_DIR) not in sys.path:
    sys.path.insert(0, str(ALGORITHM_DIR))

from coordinate_audit_algorithm import CoordinateAuditAlgorithm  # noqa: E402


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--report", type=Path, required=True, help="New JSON evidence file")
    parser.add_argument("--work-dir", type=Path, required=True, help="New output directory")
    args = parser.parse_args(argv)
    if args.report.exists() or args.work_dir.exists():
        parser.error("--report and --work-dir must not already exist")

    application = QgsApplication([], False)
    application.initQgis()
    try:
        Processing.initialize()
        algorithm = CoordinateAuditAlgorithm()
        result = processing.run(
            algorithm,
            {
                "INPUT": str(ROOT / "fixtures" / "points.geojson"),
                "OUTPUT_DIRECTORY": str(args.work_dir),
            },
        )
        artifact = Path(result["ARTIFACT"])
        summary_path = Path(result["SUMMARY"])
        summary = json.loads(summary_path.read_text(encoding="utf-8"))
        digest = hashlib.sha256(artifact.read_bytes()).hexdigest()
        if digest != summary["artifact_sha256"]:
            raise RuntimeError("QGIS-produced artifact failed independent hash readback")
        report = {
            "schema_version": "1.0",
            "workflow_id": "golden-qgis-coordinate-audit",
            "qgis_version": Qgis.QGIS_VERSION,
            "evidence_tier": "T3",
            "status": "verified",
            "algorithm_id": algorithm.name(),
            "artifact_sha256": digest,
            "accepted_feature_count": summary["accepted_feature_count"],
            "rejected_feature_count": summary["rejected_feature_count"],
            "clean_environment_T4": "NOT RUN",
        }
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(
            json.dumps(report, ensure_ascii=False, indent=2, sort_keys=True) + "\n",
            encoding="utf-8",
        )
        print(json.dumps(report, ensure_ascii=False, sort_keys=True))
        return 0
    finally:
        application.exitQgis()


if __name__ == "__main__":
    raise SystemExit(main())
