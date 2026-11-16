#!/usr/bin/env python3
"""Search local curated geospatial source metadata, without network access."""
import argparse
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DEFAULT_CATALOG = ROOT / "references" / "geospatial-data-repositories.json"


def search(entries, query="", category="", kind="", coverage="", limit=25):
    if limit < 0:
        raise ValueError("limit cannot be negative")
    query = query.strip().casefold()
    selected = []
    for item in entries:
        if category and item.get("category") != category:
            continue
        if kind and item.get("type") != kind:
            continue
        if coverage and coverage.casefold() not in item.get("coverage", "").casefold():
            continue
        searchable = " ".join(str(item.get(key, "")) for key in
                              ("name", "id", "category", "coverage", "notes", "origin")).casefold()
        if query and query not in searchable:
            continue
        selected.append(item)
        if len(selected) == limit:
            break
    return selected[:limit]


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--file", type=Path, default=DEFAULT_CATALOG)
    parser.add_argument("--query", default="")
    parser.add_argument("--category", default="")
    parser.add_argument("--type", dest="kind", default="")
    parser.add_argument("--coverage", default="")
    parser.add_argument("--limit", type=int, default=25)
    parser.add_argument("--json", action="store_true")
    args = parser.parse_args(argv)
    if args.limit < 0:
        parser.error("--limit must not be negative")
    try:
        document = json.loads(args.file.read_text(encoding="utf-8"))
        results = search(document["repositories"], args.query, args.category,
                         args.kind, args.coverage, args.limit)
    except (OSError, ValueError, KeyError, TypeError) as exc:
        parser.error(str(exc))
    if args.json:
        print(json.dumps(results, ensure_ascii=False, indent=2))
    else:
        for entry in results:
            print("{} | {} | {} | {}".format(
                entry["id"], entry["type"], entry["name"], entry["url"]))
        print("{} matching source references (availability not checked)".format(len(results)))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
