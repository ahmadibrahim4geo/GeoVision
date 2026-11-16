# KMZ style-preservation regression (standard-library only)

**Purpose:** demonstrate why import/export must preserve original per-feature KML styles and their packed image/media bytes. The checker compares two saved KMZ archives for matching Style/StyleMap/StyleUrl subtrees and SHA-256 of every non-KML member.

Run:
```bash
python -m unittest discover -s tests -v
python examples/kml-style-preservation/verify_kmz_style.py original.kmz roundtripped.kmz
```

Tests build two synthetic archives in memory (no third-party icons). One contains shared style + highlight map, original `Icon/href`, blue KML line `ffff0000`, polygon alpha and label. A mutated export intentionally replaces the icon, changes line color or removes the style link; checker must reject it. Unchanged repackage must pass.

**Security and scope:** inspection enforces basic ZIP path, size/count, symlink and XML DTD safety. It does not extract files, open network links or execute icon payloads. It does **not** validate 3D renderer, full XML extensions, XML signature equivalence, geometry, source-byte order or actual screenshots. If used in an import pipeline, implement a robust KML parser and additional separate ZIP defenses. The checker is a **narrow T1 regression fixture, not a production archive sanitizer**.

See [the preservation contract](../../references/kml-kmz-symbol-preservation.md). Always use the project's native integration tests before claiming ZEarth/Google Earth appearance fidelity.
