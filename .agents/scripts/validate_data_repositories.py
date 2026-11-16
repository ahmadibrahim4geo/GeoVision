#!/usr/bin/env python3
"""Validate GIS repository directory metadata; offline, not endpoint testing."""
import json
from pathlib import Path
from urllib.parse import urlparse

ROOT = Path(__file__).resolve().parents[1]
CATALOG = ROOT / "references" / "geospatial-data-repositories.json"
REQUIRED = ("id", "name", "url", "type", "category", "coverage", "access",
            "license", "notes", "origin", "status", "last_checked",
            "tested_by_repository", "license_verified")
TYPES = {"directory", "catalog-index", "portal", "data-reference"}


def validate(path=CATALOG):
    problems = []
    try:
        document = json.loads(Path(path).read_text(encoding="utf-8"))
    except (OSError, ValueError) as exc:
        return ["Cannot read repository directory: {}".format(exc)]
    if not isinstance(document, dict) or document.get("schema_version") != "1.0":
        return ["Unsupported repository catalog schema"]
    entries = document.get("repositories")
    if not isinstance(entries, list) or not entries:
        return ["Catalog must have a nonempty repositories array"]
    ids, urls = set(), set()
    for index, entry in enumerate(entries):
        where = "repositories[{}]".format(index)
        if not isinstance(entry, dict):
            problems.append(where + " must be object")
            continue
        for key in REQUIRED:
            if key not in entry:
                problems.append(where + " missing " + key)
        if any(key not in entry for key in REQUIRED):
            continue
        for key in ("id", "name", "url", "type", "category", "coverage",
                    "access", "license", "notes", "origin", "status"):
            if not isinstance(entry[key], str) or not entry[key].strip():
                problems.append(where + " invalid " + key)
        url = entry["url"]
        if not isinstance(url, str):
            problems.append(where + " invalid url")
            continue
        parsed = urlparse(url)
        if parsed.scheme != "https" or not parsed.netloc or parsed.username or parsed.password:
            problems.append(where + " invalid HTTPS url")
        if entry["type"] not in TYPES:
            problems.append(where + " unknown source type")
        if entry["id"] in ids:
            problems.append(where + " duplicate id")
        if url.rstrip("/").casefold() in urls:
            problems.append(where + " duplicate url")
        ids.add(entry["id"])
        urls.add(url.rstrip("/").casefold())
        if entry["status"] not in ("active", "archived", "unknown"):
            problems.append(where + " invalid status")
        if entry["last_checked"] is not None and not isinstance(entry["last_checked"], str):
            problems.append(where + " invalid last_checked")
        if not isinstance(entry["tested_by_repository"], bool):
            problems.append(where + " invalid tested_by_repository")
        if not isinstance(entry["license_verified"], bool):
            problems.append(where + " invalid license_verified")
    return problems


if __name__ == "__main__":
    issues = validate()
    if issues:
        print("\n".join("ERROR: " + issue for issue in issues))
        raise SystemExit(1)
    print("Data source directory valid: {} entries".format(
        len(json.loads(CATALOG.read_text(encoding="utf-8"))["repositories"])))
