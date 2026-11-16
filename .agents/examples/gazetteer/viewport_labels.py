"""Offline GeoNames 2D reference labels demo; Python 3 standard library.

No live API, no data bundled, no native Cesium/WPF/ArcGIS/QGIS renderer.
Uses extracted tab-separated GeoNames dump: https://www.geonames.org/export/
"""
import argparse
import json
import sqlite3
from pathlib import Path


def priority(feature_class, code, population):
    """Lower = more important; not a substitute for local editorial ranking."""
    if code in ("PCLI", "PCL", "PCLD"):
        return 0
    if code == "PPLC":
        return 1
    if code in ("ADM1", "PPLA", "PPLA2"):
        return 2
    if feature_class == "P":
        return 2 if population >= 500000 else (3 if population >= 50000 else
               (4 if population >= 1000 else 5))
    if feature_class == "A":
        return 4 if code == "ADM2" else 5
    if feature_class in ("H", "T"):
        return 3 if code in ("SEA", "LK", "MT", "MTS", "VLY", "WAD") else 5
    return 5


def create_schema(db):
    db.executescript("""
        CREATE TABLE IF NOT EXISTS places (
            id INTEGER PRIMARY KEY, name TEXT NOT NULL, lat REAL NOT NULL,
            lon REAL NOT NULL, feature_class TEXT NOT NULL, feature_code TEXT NOT NULL,
            country TEXT NOT NULL, population INTEGER NOT NULL, priority INTEGER NOT NULL
        );
        CREATE INDEX IF NOT EXISTS places_view ON places(priority,lat,lon);
        CREATE INDEX IF NOT EXISTS places_name ON places(name);
    """)


def import_tsv(db, path, batch_size=5000):
    """Stream supplied extracted GeoNames country/world TSV in bounded batches."""
    create_schema(db)
    buffer = []
    count = 0
    with Path(path).open(encoding="utf-8") as file:
        for line in file:
            f = line.rstrip("\r\n").split("\t")
            if len(f) < 15:
                continue
            try:
                gid, lat, lon = int(f[0]), float(f[4]), float(f[5])
                population = max(0, int(f[14] or "0"))
            except ValueError:
                continue
            if not (-90 <= lat <= 90 and -180 <= lon <= 180) or not f[1]:
                continue
            fc, code = f[6], f[7]
            buffer.append((gid, f[1], lat, lon, fc, code, f[8], population,
                           priority(fc, code, population)))
            if len(buffer) >= batch_size:
                db.executemany("INSERT OR REPLACE INTO places VALUES (?,?,?,?,?,?,?,?,?)", buffer)
                count += len(buffer)
                buffer.clear()
        if buffer:
            db.executemany("INSERT OR REPLACE INTO places VALUES (?,?,?,?,?,?,?,?,?)", buffer)
            count += len(buffer)
    db.commit()
    return count


def max_priority(scale):
    if not 0 < scale <= 1e12:
        raise ValueError("Scale denominator must be positive")
    return (1 if scale > 20000000 else 2 if scale > 5000000 else
            3 if scale > 1000000 else 4 if scale > 250000 else 5)


def viewport_labels(db, bbox, scale, width_px=1200, height_px=800,
                    enabled=True, limit=120, categories=None):
    """Approximate declutter only; real GIS renderer must use font metrics."""
    if not enabled:
        return []
    west, south, east, north = bbox
    if not (-180 <= west <= 180 and -180 <= east <= 180 and
            -90 <= south < north <= 90):
        raise ValueError("Invalid WGS84 bbox")
    if width_px <= 0 or height_px <= 0 or not 0 < limit <= 5000:
        raise ValueError("Invalid display size/label limit")
    span = east - west if east > west else east + 360 - west
    if span <= 0 or span > 360:
        raise ValueError("Invalid longitude span")
    clause = "lon BETWEEN ? AND ?" if east >= west else "(lon >= ? OR lon <= ?)"
    query = ("SELECT id,name,lat,lon,feature_class,feature_code,country,population,priority "
             "FROM places WHERE lat BETWEEN ? AND ? AND " + clause +
             " AND priority <= ? ORDER BY priority,population DESC,id LIMIT ?")
    args = (south, north, west, east, max_priority(scale),
            min(50000, max(1000, limit * 30)))
    accepted = None if categories is None else set(categories)
    visible, boxes = [], []
    for gid, name, lat, lon, fc, code, country, pop, rank in db.execute(query, args):
        if accepted is not None and fc not in accepted:
            continue
        x = (lon - west if lon >= west else lon + 360 - west) / span * width_px
        y = (north - lat) / (north - south) * height_px
        box = (x - 3, y - 12, x + min(250, 12 + 8 * len(name)), y + 9)
        if any(not (box[2] < b[0] or box[0] > b[2] or
                    box[3] < b[1] or box[1] > b[3]) for b in boxes):
            continue
        boxes.append(box)
        visible.append({"provider_id": "geonames", "place_id": gid,
                        "name": name, "coordinates": [lon, lat], "crs": "EPSG:4326",
                        "feature_class": fc, "feature_code": code,
                        "country_code": country, "population": pop, "priority": rank,
                        "pixel": [round(x, 2), round(y, 2)],
                        "attribution": "GeoNames — CC BY 4.0"})
        if len(visible) >= limit:
            break
    return visible


def search_places(db, query, limit=10):
    """Offline primary-name lookup only; add language-tagged aliases for production."""
    if not query.strip() or not 1 <= limit <= 100:
        raise ValueError("Supply a name and limit 1..100")
    q = query.strip().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
    rows = db.execute(
        "SELECT id,name,lat,lon,country,feature_code FROM places "
        "WHERE name LIKE ? ESCAPE '\\' ORDER BY priority,population DESC LIMIT ?",
        ("%" + q + "%", limit))
    return [{"place_id": i, "name": n, "coordinates": [lo, la],
             "country_code": c, "feature_code": fc, "provider_id": "geonames"}
            for i, n, la, lo, c, fc in rows]


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--db", required=True)
    sub = p.add_subparsers(dest="command", required=True)
    imp = sub.add_parser("import")
    imp.add_argument("--tsv", required=True)
    lab = sub.add_parser("labels")
    lab.add_argument("--bbox", nargs=4, required=True, type=float,
                     metavar=("WEST", "SOUTH", "EAST", "NORTH"))
    lab.add_argument("--scale", required=True, type=float)
    lab.add_argument("--width", type=int, default=1200)
    lab.add_argument("--height", type=int, default=800)
    lab.add_argument("--limit", type=int, default=120)
    sr = sub.add_parser("search")
    sr.add_argument("--query", required=True)
    a = p.parse_args()
    with sqlite3.connect(a.db) as db:
        result = ({"imported_rows": import_tsv(db, a.tsv)} if a.command == "import"
                  else viewport_labels(db, a.bbox, a.scale, a.width, a.height, limit=a.limit)
                  if a.command == "labels" else search_places(db, a.query))
    print(json.dumps(result, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
