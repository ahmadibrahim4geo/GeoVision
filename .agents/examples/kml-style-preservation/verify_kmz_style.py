#!/usr/bin/env python3
"""Narrow offline KMZ style and resource regression checker (not a renderer)."""
import argparse
import hashlib
import json
import stat
from pathlib import PurePosixPath
from xml.etree import ElementTree as ET
from zipfile import ZipFile


MAX_FILES = 1000
MAX_TOTAL = 32 * 1024 * 1024
MAX_ENTRY = 8 * 1024 * 1024
KML = "{http://www.opengis.net/kml/2.2}"
STYLE_TAGS = {KML + tag for tag in ("Style", "StyleMap", "styleUrl")}


def normalized_element(element):
    """Normalize formatting-only whitespace and attribute order, not values."""
    return (element.tag, sorted(element.attrib.items()),
            (element.text or "").strip(), [normalized_element(child) for child in element])


def inspect_kmz(path):
    """Read styles and media safely without extracting any member."""
    with ZipFile(path, "r") as z:
        members = z.infolist()
        if not members or len(members) > MAX_FILES:
            raise ValueError("KMZ member count out of bounds")
        total = 0
        for info in members:
            name = info.filename
            pure = PurePosixPath(name)
            if (not name or name.startswith("/") or "\\" in name or
                    any(part in ("..", ".") for part in name.split("/")) or
                    len(pure.parts) == 0 or pure.is_absolute()):
                raise ValueError("Unsafe KMZ member path")
            if stat.S_ISLNK(info.external_attr >> 16):
                raise ValueError("KMZ symlink entry rejected")
            if info.flag_bits & 1:
                raise ValueError("KMZ encrypted entry rejected")
            if info.file_size > MAX_ENTRY:
                raise ValueError("Oversized KMZ member")
            total += info.file_size
            if total > MAX_TOTAL:
                raise ValueError("Oversized KMZ archive")
        names = [info.filename for info in members if not info.is_dir()]
        if len(names) != len(set(names)):
            raise ValueError("Duplicate member name")
        primary = [name for name in names if name.lower() == "doc.kml"]
        if not primary:
            primary = [name for name in names if name.lower().endswith(".kml")]
        if len(primary) != 1:
            raise ValueError("Expected exactly one primary KML for fixture")
        kml_raw = z.read(primary[0])
        if b"<!DOCTYPE" in kml_raw.upper() or b"<!ENTITY" in kml_raw.upper():
            raise ValueError("DTD/entities are not allowed")
        root = ET.fromstring(kml_raw)
        styles = [normalized_element(node) for node in root.iter()
                  if node.tag in STYLE_TAGS]
        assets = {name: hashlib.sha256(z.read(name)).hexdigest()
                  for name in names if name != primary[0]}
        return {"styles": styles, "assets": assets}


def compare_archives(original, exported):
    before = inspect_kmz(original)
    after = inspect_kmz(exported)
    return {"style_equal": before["styles"] == after["styles"],
            "asset_equal": before["assets"] == after["assets"],
            "original_asset_count": len(before["assets"]),
            "export_asset_count": len(after["assets"])}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("original")
    parser.add_argument("exported")
    args = parser.parse_args()
    result = compare_archives(args.original, args.exported)
    print(json.dumps(result, indent=2))
    if not result["style_equal"] or not result["asset_equal"]:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
