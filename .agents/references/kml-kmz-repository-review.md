# KML/KMZ open-source engineering references

**Reviewed:** 2026-10-25. These are upstream **code licenses**, independently checked against the exact linked license files. This is an engineering inventory, not an assertion that the packages have been embedded, run, or legally cleared for every distribution. **Do not change an upstream license, or relabel external code as repository MIT.**

| Repository | Primary code license and evidence | Relevant capabilities | Implementation / redistribution cautions |
| --- | --- | --- | --- |
| [google/libkml](https://github.com/google/libkml) | **BSD-3-Clause** — [upstream LICENSE](https://github.com/google/libkml/blob/master/LICENSE) and [COPYING](https://github.com/google/libkml/blob/master/COPYING) | KML object model, serialization and KMZ | Retain copyright/license/disclaimer; no Google endorsement; review bundled third-party parts, maintenance and build |
| [OSGeo/gdal](https://github.com/OSGeo/gdal) | **MIT-style core** — [upstream LICENSE.TXT](https://github.com/OSGeo/gdal/blob/master/LICENSE.TXT) | OGR KML/LIBKML drivers and GIS feature conversion | GDAL source tree and binary dependencies can have **other licenses**, possibly GPL/LGPL/proprietary; check the exact build; not scene-fidelity equivalence |
| [cleder/fastkml](https://github.com/cleder/fastkml) | **LGPL-2.1 license text** — [upstream LICENSE](https://github.com/cleder/fastkml/blob/develop/LICENSE); [package metadata](https://github.com/cleder/fastkml/blob/develop/pyproject.toml) states LGPL | Python KML read/write, object model | LGPL obligations matter when distributing/modifying the library; check actual installed release, dependencies and extension support |
| [tylere/pykml](https://github.com/tylere/pykml) | **BSD-3-Clause** — [upstream LICENSE.txt](https://github.com/tylere/pykml/blob/master/LICENSE.txt) | XML/KML authoring and validation | Retain notices and no-endorsement clause; examine dependency/maintenance status |
| [CesiumGS/cesium](https://github.com/CesiumGS/cesium) | **Apache-2.0** — [upstream LICENSE.md](https://github.com/CesiumGS/cesium/blob/main/LICENSE.md) | Browser KML and 3D globe rendering | Keep applicable notices and verify third-party/asset/provider rights; CesiumJS source license does **not** license imagery, terrain or Cesium ion services |
| [mapbox/togeojson](https://github.com/mapbox/togeojson) | **BSD-2-Clause** — [upstream LICENSE](https://github.com/mapbox/togeojson/blob/master/LICENSE) | KML-to-GeoJSON conversion | Preserve notice; GeoJSON does not represent all KML scene/style/time semantics |
| [qgis/QGIS](https://github.com/qgis/QGIS) | **GPL-2.0 family** — [upstream COPYING](https://github.com/qgis/QGIS/blob/master/COPYING) | Desktop GIS KML interoperability | Review actual file/dependency licenses and redistribution terms; QGIS plugin/standalone integrations differ |

## Research-to-implementation and license gate

1. **Reference is not redistribution:** A hyperlink to third-party code is not incorporation of that code. The [MIT license of GIS Development Skills](../LICENSE) covers **this repository's original material only**; it cannot override upstream licenses, bundled fonts, icons, remote APIs, data or imagery.
2. Before copying, vendoring, modifying or distributing third-party code: identify **exact repository, revision and file**, inspect the license at that revision (and NOTICE/third-party dependency files), record compatible use and preservation of copyright/attribution, and document approval. `LGPL`, `GPL`, `Apache`, `MIT` and `BSD` are distinct, even when all are open source.
3. For a KML/KMZ task, test each candidate's support for Placemark, nested Folder, ExtendedData, StyleMap, GroundOverlay, NetworkLink, altitude, time, gx:Track, gx:Tour and COLLADA. Do not promise universal interoperability from an XML parse or a single fixture.
4. Prefer separate adapters for ArcGIS Pro, ArcMap, QGIS, Cesium and portable file workflows. Record lossy conversions. Never silently replace original styles or icon assets on import.
5. Add fixtures and a conversion-loss report, including invalid XML, zip bombs, traversal, HTML XSS, network fetch, and missing assets; test actual round-trip in each requested host. Keep host execution evidence separate from offline tests.
6. Review upstream licenses **again at the version selected for distribution**. A verified 2026-10-25 source snapshot is not permanent clearance for future releases.

See [KML/KMZ engineering skill](../skills/kml-kmz-engineering/SKILL.md), [source-preservation guide](google-kml-kmz-compatibility.md), and [free-first policy](free-first-policy.md).
