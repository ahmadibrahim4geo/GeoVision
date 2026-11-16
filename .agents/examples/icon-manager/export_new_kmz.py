#!/usr/bin/env python3
"""Create a NEW single-placemark KMZ using a user-supplied local PNG.

Not an importer/editor of other people's KML/KMZ; no network access or external packages.
"""
import argparse
import math
import struct
import zlib
from pathlib import Path
from xml.etree import ElementTree as ET
from zipfile import ZIP_DEFLATED, ZipFile, ZipInfo

PNG_SIGNATURE = b"\x89PNG\r\n\x1a\n"
MAX_PNG_BYTES = 4 * 1024 * 1024
MAX_DIMENSION = 4096
MAX_PNG_CHUNKS = 256
KML_NS = "http://www.opengis.net/kml/2.2"
KML = "{" + KML_NS + "}"
ET.register_namespace("", KML_NS)
ARCHIVE_ICON = "files/icons/marker.png"


def validate_png(data):
    """Validate signature, chunk boundaries/CRCs and IHDR/IEND (not full pixel decode)."""
    if len(data) > MAX_PNG_BYTES or len(data) < 45 or not data.startswith(PNG_SIGNATURE):
        raise ValueError("PNG must be a valid-sized PNG file (up to 4 MiB)")
    pos = len(PNG_SIGNATURE)
    seen_ihdr = False
    seen_idat = False
    seen_iend = False
    count = 0
    while pos < len(data):
        count += 1
        if count > MAX_PNG_CHUNKS or pos + 12 > len(data):
            raise ValueError("Invalid PNG chunk structure/count")
        length = struct.unpack(">I", data[pos:pos + 4])[0]
        chunk_type = data[pos + 4:pos + 8]
        pos += 8
        if length > MAX_PNG_BYTES or pos + length + 4 > len(data):
            raise ValueError("Malformed PNG chunk length")
        body = data[pos:pos + length]
        crc = struct.unpack(">I", data[pos + length:pos + length + 4])[0]
        if zlib.crc32(chunk_type + body) & 0xffffffff != crc:
            raise ValueError("PNG chunk CRC mismatch")
        pos += length + 4
        if not seen_ihdr:
            if chunk_type != b"IHDR" or length != 13:
                raise ValueError("Expected PNG IHDR")
            width, height, depth, color_type = struct.unpack(">IIBB", body[:10])
            if not (1 <= width <= MAX_DIMENSION and 1 <= height <= MAX_DIMENSION):
                raise ValueError("PNG dimensions out of bounds")
            if depth not in (1, 2, 4, 8, 16) or color_type not in (0, 2, 3, 4, 6):
                raise ValueError("Unsupported PNG color layout")
            seen_ihdr = True
        elif chunk_type == b"IHDR":
            raise ValueError("Duplicate PNG IHDR")
        if chunk_type == b"IDAT":
            seen_idat = True
        if chunk_type == b"IEND":
            if length != 0 or pos != len(data):
                raise ValueError("Invalid PNG trailer")
            seen_iend = True
            break
    if not seen_idat or not seen_iend:
        raise ValueError("Missing PNG image data or trailer")


def finite_number(value, minimum, maximum, label):
    value = float(value)
    if not math.isfinite(value) or value < minimum or value > maximum:
        raise ValueError("%s outside range [%s, %s]" % (label, minimum, maximum))
    return value


def kml_abgr(css_hex, opacity=1.0):
    """Convert explicit CSS #RRGGBB plus alpha to KML aabbggrr."""
    if not isinstance(css_hex, str) or len(css_hex) != 7 or css_hex[0] != "#":
        raise ValueError("Icon tint must be #RRGGBB")
    try:
        r, g, b = (int(css_hex[i:i + 2], 16) for i in (1, 3, 5))
    except ValueError as e:
        raise ValueError("Icon tint must be hexadecimal #RRGGBB") from e
    alpha = finite_number(opacity, 0, 1, "opacity")
    a = round(alpha * 255)
    return "%02x%02x%02x%02x" % (a, b, g, r)


def add(parent, tag, value=None, **attributes):
    node = ET.SubElement(parent, KML + tag, attributes)
    if value is not None:
        node.text = str(value)
    return node


def kmz_bytes(png, name, lon, lat, color="#FFFFFF", opacity=1,
              scale=1, heading=0, attribution=None, anchor_y=0):
    validate_png(png)
    if not isinstance(name, str) or not name.strip() or len(name) > 256:
        raise ValueError("Placemark name must contain 1–256 characters")
    lon = finite_number(lon, -180, 180, "longitude")
    lat = finite_number(lat, -90, 90, "latitude")
    scale = finite_number(scale, 0.05, 20, "scale")
    heading = finite_number(heading, 0, 360, "heading")
    anchor_y = finite_number(anchor_y, 0, 1, "anchor_y")
    if attribution is not None and len(attribution) > 1000:
        raise ValueError("Attribution too long")

    root = ET.Element(KML + "kml")
    doc = add(root, "Document")
    style = add(doc, "Style", id="new-user-icon")
    icon_style = add(style, "IconStyle")
    add(icon_style, "color", kml_abgr(color, opacity))
    add(icon_style, "scale", format(scale, ".9g"))
    add(icon_style, "heading", format(heading, ".9g"))
    add(add(icon_style, "Icon"), "href", ARCHIVE_ICON)
    add(icon_style, "hotSpot", x="0.5", y=format(anchor_y, ".9g"), xunits="fraction", yunits="fraction")

    placemark = add(doc, "Placemark")
    add(placemark, "name", name)
    add(placemark, "styleUrl", "#new-user-icon")
    if attribution:
        data = add(add(placemark, "ExtendedData"), "Data", name="icon_source_attribution")
        add(data, "value", attribution)
    point = add(placemark, "Point")
    add(point, "coordinates", "%.9f,%.9f" % (lon, lat))
    document = ET.tostring(root, encoding="utf-8", xml_declaration=True)

    # Deterministic archive names; caller cannot inject ZIP member paths.
    from io import BytesIO
    buf = BytesIO()
    with ZipFile(buf, "w", ZIP_DEFLATED) as zf:
        for archive_path, payload in (("doc.kml", document), (ARCHIVE_ICON, png)):
            info = ZipInfo(archive_path, (2026, 1, 1, 0, 0, 0))
            info.compress_type = ZIP_DEFLATED
            zf.writestr(info, payload)
    return buf.getvalue()


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--png", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--name", required=True)
    parser.add_argument("--lon", type=float, required=True)
    parser.add_argument("--lat", type=float, required=True)
    parser.add_argument("--color", default="#FFFFFF",
                        help="Optional KML image tint; #FFFFFF preserves PNG colors")
    parser.add_argument("--opacity", type=float, default=1)
    parser.add_argument("--scale", type=float, default=1)
    parser.add_argument("--heading", type=float, default=0)
    parser.add_argument("--attribution", default=None)
    parser.add_argument("--anchor-y", type=float, default=0,
                        help="0 for pin tip / bottom, 0.5 for centered symbols")
    parser.add_argument("--force", action="store_true", help="Allow replacing output file")
    args = parser.parse_args(argv)
    if not args.png.is_file():
        parser.error("PNG file not found")
    if args.output.suffix.lower() != ".kmz":
        parser.error("Output filename must end with .kmz")
    if args.output.resolve() == args.png.resolve():
        parser.error("Output must differ from input PNG")
    if args.output.exists() and not args.force:
        parser.error("Output already exists; use a new filename or --force")
    package = kmz_bytes(args.png.read_bytes(), args.name, args.lon, args.lat,
                        args.color, args.opacity, args.scale, args.heading,
                        args.attribution, args.anchor_y)
    args.output.write_bytes(package)
    print("Created %s (%d bytes) — NEW placemark; source KML/KMZ not modified" %
          (args.output, len(package)))


if __name__ == "__main__":
    main()
