#!/usr/bin/env python3
"""Inspect user-supplied Esri Projection Engine factory CSV/JSON metadata.

Standard library only; no coordinate transformation, Esri binary, or network use.
Folder labels follow ArcGIS Pro documentation; Esri PE does not publish its full
UI folder tree in the factory object CSV/JSON tables.
"""
import argparse
import csv
import json
import re
from pathlib import Path

OBJECT_TYPES = {
    "geogcs": "Geographic Coordinate Systems",
    "projcs": "Projected Coordinate Systems",
    "vertcs": "Vertical Coordinate Systems",
    "hvcoordsys": "Horizontal/Vertical Compound Coordinate Systems",
    "gxyzcs": "Geocentric Coordinate Systems",
    "geogtran": "Geographic Transformations",
    "verttran": "Vertical Transformations",
    "datum": "Datums",
    "vdatum": "Vertical Datums",
    "spheroid": "Ellipsoids",
    "primem": "Prime Meridians",
    "projection": "Projections",
    "method": "Methods",
    "parameter": "Parameters",
    "angunit": "Angular Units",
    "linunit": "Linear Units",
    "areaunit": "Area Units",
}
HORIZONTAL_TYPES = ("geogcs", "projcs")
VERTICAL_TYPES = ("vertcs",)
IDENTIFIER = re.compile(r"^[A-Za-z][A-Za-z0-9_-]*:[0-9]+$")


def source_paths(source):
    """Prefer JSON over CSV for each Esri object-family file when both exist."""
    path = Path(source)
    if not path.exists():
        raise ValueError("Source path not found: {}".format(path))
    paths = [path] if path.is_file() else list(path.rglob("pe_list_*.json")) + list(path.rglob("pe_list_*.csv"))
    selected = {}
    for item in sorted(paths, key=lambda p: (p.stem, p.suffix != ".json")):
        if item.suffix.lower() not in (".json", ".csv") or not item.stem.startswith("pe_list_"):
            continue
        selected.setdefault(item.stem, item)
    if not selected:
        raise ValueError("No pe_list_*.json or pe_list_*.csv files found")
    return [selected[key] for key in sorted(selected)]


def rows_from_file(path):
    if path.suffix.lower() == ".csv":
        with path.open("r", encoding="utf-8-sig", newline="") as stream:
            yield from csv.DictReader(stream)
        return
    with path.open("r", encoding="utf-8-sig") as stream:
        payload = json.load(stream)
    if isinstance(payload, dict):
        groups = [v for v in payload.values() if isinstance(v, list)]
    elif isinstance(payload, list):
        groups = [payload]
    else:
        raise ValueError("Expected a JSON object containing arrays, or an array")
    for group in groups:
        for row in group:
            if isinstance(row, dict):
                yield row


def read_catalog(source):
    result = []
    for path in source_paths(source):
        kind = path.stem[len("pe_list_"):]
        for original in rows_from_file(path):
            item = dict(original)
            item["object_type"] = kind
            result.append(item)
    return result


def longitude_intervals(west, east):
    # Accept an extent crossing the antimeridian when west > east.
    if west <= east:
        return [(west, east)]
    return [(west, 180.0), (-180.0, east)]


def intersects(record, bbox):
    """Geographic bounding-box intersection, inclusive, degrees from Greenwich."""
    if bbox is None:
        return True
    try:
        s, n = float(record["slat"]), float(record["nlat"])
        w, e = float(record["llon"]), float(record["rlon"])
    except (KeyError, ValueError, TypeError):
        return False
    xmin, ymin, xmax, ymax = bbox
    if ymin > ymax or n < ymin or s > ymax:
        return False
    return any(a <= d and c <= b
               for a, b in longitude_intervals(w, e)
               for c, d in longitude_intervals(xmin, xmax))


def filter_rows(rows, query="", kind=None, bbox=None):
    tokens = [token.casefold() for token in query.strip().split()]
    for row in rows:
        if kind and row.get("object_type") != kind:
            continue
        searchable = " ".join(str(row.get(k, "")) for k in
                              ("wkid", "latestWkid", "name", "description",
                               "authority", "areaname", "macro")).casefold()
        if all(token in searchable for token in tokens) and intersects(row, bbox):
            yield row


def favorite_file(path):
    if not path:
        raise ValueError("Specify --favorites FILE to persist favorites")
    return Path(path)


def load_favorites(path):
    file = Path(path)
    if not file.exists():
        return []
    contents = json.loads(file.read_text(encoding="utf-8"))
    if not isinstance(contents, list) or not all(isinstance(x, str) and IDENTIFIER.fullmatch(x) for x in contents):
        raise ValueError("Favorites file must contain a list of AUTHORITY:code identifiers")
    return contents


def change_favorite(path, identifier, action):
    if not IDENTIFIER.fullmatch(identifier):
        raise ValueError("Favorites require AUTHORITY:integer-code (e.g. EPSG:4326)")
    identifier = identifier.upper()
    file = favorite_file(path)
    favorites = load_favorites(file)
    if action == "add" and identifier not in favorites:
        favorites.append(identifier)
    if action == "remove":
        favorites = [x for x in favorites if x != identifier]
    file.parent.mkdir(parents=True, exist_ok=True)
    file.write_text(json.dumps(favorites, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    return favorites


def load_layers(path):
    if not path:
        return []
    data = json.loads(Path(path).read_text(encoding="utf-8"))
    if not isinstance(data, list) or not all(isinstance(x, dict) and "name" in x for x in data):
        raise ValueError("Layers must be a JSON array of objects with a name")
    return data


def tree_summary(rows, favorites, layers):
    from collections import Counter
    counts = Counter(row["object_type"] for row in rows)
    # Only these top-level folder names follow Esri. Regional folders are
    # deliberately omitted because the PE tables do not store UI paths.
    return {
        "Horizontal systems": {
            "Favorites": favorites,
            "Layers": layers,
            "Geographic Coordinate Systems": counts["geogcs"],
            "Projected Coordinate Systems": counts["projcs"],
            "Custom": "user-provided; not a factory list",
        },
        "Vertical systems": {
            "Favorites": favorites,
            "Layers": layers,
            "Vertical Coordinate Systems": counts["vertcs"],
            "Custom": "user-provided; not a factory list",
        },
        "Other Esri factory object tables": {
            OBJECT_TYPES.get(kind, kind): count
            for kind, count in sorted(counts.items())
            if kind not in HORIZONTAL_TYPES + VERTICAL_TYPES
        },
        "hierarchy_provenance": "Official Esri top-level terminology; no claimed exact regional folder mapping",
    }


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", help="Esri projection-engine-db-doc JSON/CSV folder or file")
    parser.add_argument("--kind", choices=sorted(OBJECT_TYPES), help="PE object family, e.g. projcs")
    parser.add_argument("--query", default="", help="Search code, name, area, description and authority")
    parser.add_argument("--bbox", nargs=4, type=float, metavar=("WEST", "SOUTH", "EAST", "NORTH"),
                        help="Geographic bounding-box in degrees; may wrap 180th meridian")
    parser.add_argument("--format", choices=("text", "json"), default="text")
    parser.add_argument("--limit", type=int, default=20)
    parser.add_argument("--tree", action="store_true", help="Summarize Esri top-level UI folders and object counts")
    parser.add_argument("--favorites", help="User-owned JSON favorites file; never modifies Esri application settings")
    parser.add_argument("--favorite-add", metavar="AUTHORITY:CODE")
    parser.add_argument("--favorite-remove", metavar="AUTHORITY:CODE")
    parser.add_argument("--layers", help="Optional JSON list of current document's layers; no live application introspection")
    args = parser.parse_args(argv)

    try:
        if args.limit < 1:
            raise ValueError("--limit must be at least 1")
        if args.favorite_add and args.favorite_remove:
            raise ValueError("Choose either --favorite-add or --favorite-remove")
        if args.favorite_add or args.favorite_remove:
            favorites = change_favorite(
                args.favorites, args.favorite_add or args.favorite_remove,
                "add" if args.favorite_add else "remove")
            print(json.dumps({"Favorites": favorites}, indent=2))
            return 0
        if not args.source:
            raise ValueError("--source is required for search or --tree")
        records = read_catalog(args.source)
        if args.tree:
            favorites = load_favorites(args.favorites) if args.favorites else []
            layers = load_layers(args.layers)
            print(json.dumps(tree_summary(records, favorites, layers), ensure_ascii=False, indent=2))
            return 0
        matches = list(filter_rows(records, args.query, args.kind, args.bbox))
        trimmed = matches[:args.limit]
        if args.format == "json":
            print(json.dumps({"matched": len(matches), "displayed": len(trimmed), "results": trimmed},
                             ensure_ascii=False, indent=2))
        else:
            print("{} match(es), showing {}".format(len(matches), len(trimmed)))
            for row in trimmed:
                print("{}:{} [latestWkid={}] [{}] {} — {}".format(
                    row.get("authority", "?"), row.get("wkid", "?"),
                    row.get("latestWkid", "?"), row.get("object_type", "?"),
                    row.get("name", "?"), row.get("areaname", "")))
        return 0
    except (ValueError, OSError, json.JSONDecodeError, csv.Error) as exc:
        parser.exit(2, "CRS catalog error: {}\n".format(exc))


if __name__ == "__main__":
    raise SystemExit(main())
