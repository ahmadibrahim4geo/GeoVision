#!/usr/bin/env python3
"""Deterministic, dependency-free GeoJSON coordinate audit.

This is the offline core for the golden QGIS workflow.  It never edits the
source file and only writes into a new output directory.
"""

from __future__ import annotations

import hashlib
import json
import math
from pathlib import Path


SCHEMA_VERSION = "1.0"
SUPPORTED_CRS = "EPSG:4326"
MAX_INPUT_BYTES = 10 * 1024 * 1024
MAX_FEATURES = 100_000


class AuditError(ValueError):
    """Raised when input or output safety requirements are not met."""


def _reject_duplicate_keys(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise AuditError("Duplicate JSON key: {}".format(key))
        result[key] = value
    return result


def _read_json(path: Path):
    try:
        with path.open("r", encoding="utf-8-sig") as stream:
            return json.load(stream, object_pairs_hook=_reject_duplicate_keys)
    except AuditError:
        raise
    except (OSError, UnicodeError, json.JSONDecodeError) as exc:
        raise AuditError("Unable to read valid UTF-8 JSON: {}".format(exc)) from exc


def _canonical_bytes(value) -> bytes:
    return (json.dumps(value, ensure_ascii=False, indent=2, sort_keys=True) + "\n").encode("utf-8")


def _sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def _declared_crs(document) -> str | None:
    crs = document.get("crs")
    if not isinstance(crs, dict) or crs.get("type") != "name":
        return None
    properties = crs.get("properties")
    if not isinstance(properties, dict):
        return None
    name = properties.get("name")
    return name.upper() if isinstance(name, str) else None


def _valid_number(value) -> bool:
    return isinstance(value, (int, float)) and not isinstance(value, bool) and math.isfinite(value)


def _inspect_feature(feature, index, seen_ids):
    if not isinstance(feature, dict) or feature.get("type") != "Feature":
        return None, "feature-not-object"

    feature_id = feature.get("id")
    if not isinstance(feature_id, str) or not feature_id.strip():
        return None, "missing-string-id"
    if feature_id in seen_ids:
        return None, "duplicate-id"

    geometry = feature.get("geometry")
    if not isinstance(geometry, dict) or geometry.get("type") != "Point":
        return None, "geometry-not-point"
    coordinates = geometry.get("coordinates")
    if not isinstance(coordinates, list) or len(coordinates) < 2:
        return None, "coordinates-not-position"
    longitude, latitude = coordinates[0], coordinates[1]
    if not _valid_number(longitude) or not _valid_number(latitude):
        return None, "coordinates-not-finite-numbers"
    if not -180 <= longitude <= 180 or not -90 <= latitude <= 90:
        return None, "coordinates-out-of-range"

    seen_ids.add(feature_id)
    normalized = {
        "type": "Feature",
        "id": feature_id,
        "geometry": {"type": "Point", "coordinates": [longitude, latitude]},
        "properties": feature.get("properties") if isinstance(feature.get("properties"), dict) else {},
    }
    return normalized, None


def audit_document(document, source_name, source_sha256):
    if not isinstance(document, dict) or document.get("type") != "FeatureCollection":
        raise AuditError("Input must be a GeoJSON FeatureCollection")
    if _declared_crs(document) != SUPPORTED_CRS:
        raise AuditError("Input must explicitly declare EPSG:4326")
    features = document.get("features")
    if not isinstance(features, list):
        raise AuditError("FeatureCollection.features must be an array")
    if len(features) > MAX_FEATURES:
        raise AuditError("Feature count exceeds safe limit of {}".format(MAX_FEATURES))

    valid_features = []
    rejected = []
    seen_ids = set()
    for index, feature in enumerate(features):
        normalized, reason = _inspect_feature(feature, index, seen_ids)
        if reason:
            rejected.append({"feature_index": index, "reason": reason})
        else:
            valid_features.append(normalized)

    artifact = {
        "type": "FeatureCollection",
        "crs": {"type": "name", "properties": {"name": SUPPORTED_CRS}},
        "features": valid_features,
    }
    artifact_bytes = _canonical_bytes(artifact)
    summary = {
        "schema_version": SCHEMA_VERSION,
        "evidence_tier": "T1",
        "source_file": source_name,
        "source_sha256": source_sha256,
        "declared_crs": SUPPORTED_CRS,
        "input_feature_count": len(features),
        "accepted_feature_count": len(valid_features),
        "rejected_feature_count": len(rejected),
        "rejected": rejected,
        "artifact_file": "audited_points.geojson",
        "artifact_sha256": _sha256(artifact_bytes),
        "native_qgis_status": "NOT RUN",
    }
    return artifact, summary


def run_audit(source_path, output_directory):
    source = Path(source_path).resolve()
    destination = Path(output_directory).resolve()
    if not source.is_file():
        raise AuditError("Input file does not exist: {}".format(source))
    if source.stat().st_size > MAX_INPUT_BYTES:
        raise AuditError("Input exceeds safe {} byte limit".format(MAX_INPUT_BYTES))
    if destination.exists():
        raise AuditError("Output directory must not already exist: {}".format(destination))
    if source == destination or destination in source.parents:
        raise AuditError("Output directory cannot replace the input path")

    source_bytes = source.read_bytes()
    document = _read_json(source)
    artifact, summary = audit_document(document, source.name, _sha256(source_bytes))
    artifact_bytes = _canonical_bytes(artifact)
    summary_bytes = _canonical_bytes(summary)

    destination.mkdir(parents=True, exist_ok=False)
    artifact_path = destination / "audited_points.geojson"
    summary_path = destination / "summary.json"
    artifact_path.write_bytes(artifact_bytes)
    summary_path.write_bytes(summary_bytes)

    # Read back both outputs; creation alone is not treated as proof.
    reread_artifact = _read_json(artifact_path)
    reread_summary = _read_json(summary_path)
    if reread_artifact != artifact or reread_summary != summary:
        raise AuditError("Output readback did not match generated values")
    if _sha256(artifact_path.read_bytes()) != summary["artifact_sha256"]:
        raise AuditError("Artifact hash mismatch after readback")
    return summary
