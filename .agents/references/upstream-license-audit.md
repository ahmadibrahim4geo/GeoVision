# Upstream code license audit — 2026-10-25

This is a **repository-source audit**, not a legal opinion or a certificate permitting all usage. Free code, a provider API and data licensing must be checked separately. A GitHub `license: NOASSERTION` may still have a valid license in a README/sidecar; GitHub metadata alone is not proof.

## Verified upstream license files

| Source | License found | Evidence | Conditions/independent paid services |
| --- | --- | --- | --- |
| [qgis/QGIS](https://github.com/qgis/QGIS) | GPL-2.0 | [Original COPYING](https://github.com/qgis/QGIS/blob/HEAD/COPYING) | QGIS code is GPL; plugins, dependencies and data have separate licenses. |
| [OSGeo/gdal](https://github.com/OSGeo/gdal) | MIT-style; third-party component licenses vary | [Original LICENSE.TXT](https://github.com/OSGeo/gdal/blob/HEAD/LICENSE.TXT) | GDAL/OGR source contains a comprehensive third-party licensing list; individual drivers/data have separate terms. |
| [OSGeo/PROJ](https://github.com/OSGeo/PROJ) | MIT-style | [Original COPYING](https://github.com/OSGeo/PROJ/blob/HEAD/COPYING) | PROJ source and included core data are broadly MIT-style; verify independently supplied datum grid packages. |
| [geopandas/geopandas](https://github.com/geopandas/geopandas) | BSD-3-Clause | [Original LICENSE.txt](https://github.com/geopandas/geopandas/blob/HEAD/LICENSE.txt) | GeoPandas library code; underlying packages/datasets separately licensed. |
| [opengeos/leafmap](https://github.com/opengeos/leafmap) | MIT | [Original LICENSE](https://github.com/opengeos/leafmap/blob/HEAD/LICENSE) | Library source license; maps, hosted services and data sources separately licensed. |
| [gee-community/geemap](https://github.com/gee-community/geemap) | MIT | [Original LICENSE](https://github.com/gee-community/geemap/blob/HEAD/LICENSE) | Python library source only; Google Earth Engine and imagery/data provider terms are independent. |
| [CesiumGS/cesium](https://github.com/CesiumGS/cesium) | Apache-2.0 | [Original LICENSE.md](https://github.com/CesiumGS/cesium/blob/HEAD/LICENSE.md) | CesiumJS source license; Cesium ion, premium terrain and 3D tiles services are separately licensed. |
| [Esri/arcgis-python-api](https://github.com/Esri/arcgis-python-api) | Apache-2.0 (examples/repository) | [Original LICENSE](https://github.com/Esri/arcgis-python-api/blob/HEAD/LICENSE) | Repository examples/source are Apache-2.0; ArcGIS platform, subscriptions and service rights are separate. |

The license file of **8 of 138** indexed code references was inspected at review time. The other **130 remain unverified by this project**—including entries that have a plausible license text in `project-catalog.json` but still have `license_verified:false`. A license audit cannot establish compatibility of particular **dependencies, datasets, weights or deployed products**.

## Important differences

- **QGIS code** is GPL-licensed and independently usable, while **ArcPy** runtime requires ArcGIS Pro/ArcMap licenses.
- **ArcGIS Python API repository samples** using Apache-2.0 do **not** grant a free ArcGIS account, enterprise server, hosted layers or Esri license.
- **CesiumJS** is open source; a commercial Cesium ion token, third-party terrain tiles or host storage may incur charge.
- **GDAL** and **PROJ** include multiple copyright components or separately distributed datasets; their main license does not eliminate other data conditions.
- A project **without an explicit license file**, such as the inspected Earth View repository, should remain architecture reference only pending permission.
- The original `project-catalog.json` has many `verify-upstream` records. Treat unknown as **unverified**, not closed-source; avoid either false assertion.

## Next audit workflow (repeatable, no external data copied)

```sh
python scripts/audit_license_catalog.py --summary
python scripts/audit_license_catalog.py --unverified --limit 25
python scripts/audit_license_catalog.py --unverified --json
```

1. Prioritize widely used foundational projects and examples the agent actually imports. For each, check the upstream canonical repository **license file**, SPDX/notice, last updated release, fork provenance, and code/dependency/data licenses.
2. Add `license_verified: true` only when the relevant source license has been inspected. Record `license_scope`, `license_checked_on`, `license_evidence_url`, and caveats in the versioned project catalog.
3. Mark paid host/runtime requirements independently: repository source license does **not** determine paid service subscription or commercial entitlement.
4. Keep unknown claims transparent on the bilingual website and never suggest that GitHub's public visibility grants code redistribution rights.
5. Review the [free-first policy](free-first-policy.md) and [source intake workflow](source-intake-workflow.md). Do not change a whole catalog to “verified” based on one file or owner metadata.

Audit coverage is explicit, not a substitute for external legal advice.
