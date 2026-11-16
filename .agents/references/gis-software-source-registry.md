# GIS Software & Source Code Registry — Developer's Guide

**Purpose:** find real GIS applications, programming libraries, map engines, server components, mobile clients and scientific workflows without losing the development path. This is a guide **over the existing canonical [GIS project catalog](project-catalog.json)**, not a second machine-readable project list. Browse and filter the full catalog in the bilingual [GIS Explorer](../explorer.html#catalog).

**Review date:** 2026-10-26. The additions recorded in the catalog were checked for public GitHub repository accessibility; **none was built, installed, integration-tested or audited for transitive licenses by this index**. GitHub license metadata is a hint, not proof that every file or dependency can be redistributed. Listings are independent references, not endorsements, bundled engines or licensed map/data services.

## Start here: what are you building?

| Delivery target | Source code to study first | Implementation owner / next document |
| --- | --- | --- |
| Full GIS desktop workstation | [QGIS](https://github.com/qgis/QGIS), [GeoLibre](https://github.com/opengeos/GeoLibre), [RustGIS](https://github.com/opengeos/RustGIS) | [Desktop Development](../skills/gis-desktop-application-development/SKILL.md) + [QGIS Engine](../skills/qgis-engine-integration/SKILL.md) when appropriate |
| Desktop spatial editor / cartography | [OpenJUMP](https://github.com/openjump-gis/openjump), [GeoDa](https://github.com/GeoDaCenter/geoda), [OpenOrienteering Mapper](https://github.com/OpenOrienteering/mapper) | Desktop + [Cartography](../skills/gis-cartography-symbology/SKILL.md) |
| 2D/3D web map | [MapLibre](https://github.com/maplibre/maplibre-gl-js), [OpenLayers](https://github.com/openlayers/openlayers), [CesiumJS](https://github.com/CesiumGS/cesium), [TerriaJS](https://github.com/TerriaJS/terriajs) | [Web Mapping](../skills/geospatial-web-mapping/SKILL.md) |
| Ready-to-adapt WebGIS portal | [MapStore2](https://github.com/geosolutions-it/MapStore2), [GeoNode](https://github.com/GeoNode/geonode), [Lizmap](https://github.com/3liz/lizmap-web-client) | Web Mapping + [Spatial Databases](../skills/spatial-databases/SKILL.md) + [Data Catalogs](../skills/geospatial-data-catalogs/SKILL.md) |
| Mobile / offline field collection | [QField](https://github.com/opengisch/QField), [Mergin Maps](https://github.com/MerginMaps/mobile), [MapLibre Native](https://github.com/maplibre/maplibre-native) | [Mobile GIS Development](../skills/gis-mobile-application-development/SKILL.md) + selective QGIS/rendering adapters when needed |
| GIS computation and file conversion | [GDAL](https://github.com/OSGeo/gdal), [PROJ](https://github.com/OSGeo/PROJ), [GEOS](https://github.com/libgeos/geos), [GRASS](https://github.com/OSGeo/grass) | [Geospatial Data Processing](../skills/geospatial-data-processing/SKILL.md) + [CRS](../skills/coordinate-reference-systems/SKILL.md) |
| .NET / C# map application | [Mapsui](https://github.com/Mapsui/Mapsui), [NetTopologySuite](https://github.com/NetTopologySuite/NetTopologySuite) | Desktop + [Desktop/.NET implementation guide](dotnet-gis.md) |
| Raster / imagery / geospatial AI | [SNAP Desktop](https://github.com/senbox-org/snap-desktop), [Open Data Cube](https://github.com/opendatacube/datacube-core), [eo-learn](https://github.com/sentinel-hub/eo-learn) | [Remote Sensing Pipelines](../skills/remote-sensing-pipelines/SKILL.md) |
| Online services, maps and places | [GeoServer](https://github.com/geoserver/geoserver), [MapServer](https://github.com/MapServer/MapServer), [Martin](https://github.com/maplibre/martin), [Nominatim](https://github.com/osm-search/Nominatim) | [API Integration](../skills/geospatial-api-integration/SKILL.md) + Spatial Databases |

## Source categories: applications versus reusable components

### 1. Complete desktop applications

- **QGIS** — complete, mature cross-platform GIS and extensive plugin/Processing ecosystem. Investigate Qt, provider and Python runtime ABI as well as GPL requirements.
- **GeoLibre** — modern cloud-native GIS application architecture; inspect real available features, build instructions and platform packages instead of assuming all target platforms work.
- **RustGIS** — Rust-written open-source GIS application; examine actual source modules, geometry/rendering maturity, test fixtures and packaging before selecting for production.
- **OpenJUMP / GeoDa** — references for geometry editing, topology and spatial statistics.
- **Marble** — Qt atlas and virtual-globe navigation; not a substitute for a full GIS analysis engine.
- **ESA SNAP Desktop** — professional remote-sensing application UI; pair with its distinct SNAP Engine and validate satellite-processing dependencies.
- **OpenOrienteering Mapper** — precision cartographic editor and symbol model.

**Desktop rule:** a complete application can be forked, but it is not automatically a reusable SDK. Choose between full fork, public plugin API, library embedding and isolated process integration **after** checking its license, buildability and maintainability.

### 2. Full web applications and geographic user interfaces

- **MapStore2 / GeoNode / Lizmap / TerriaMap** — examples of map portals, sharing, user roles, catalogs, QGIS project publishing and map-state persistence.
- **Kepler.gl** — large-scale interactive geospatial visual exploration and filtering.
- **uMap / GeoJSON.io / OpenStreetMap iD** — map editing, feature capture, serialization, undo/redo and user interaction patterns.
- **STAC Browser** — human-friendly navigation of STAC metadata. A catalog listing is not a guaranteed scientific asset.

**Web rule:** distinguish UI shell, browser renderer, map tiles, processing service, data license, authentication, catalog search and saved output. Test actual edit/export/readback.

### 3. Map rendering, 2D, 3D, tiles and spatial visualization

- **MapLibre GL JS / MapLibre Native / OpenLayers / Leaflet / CesiumJS** — choose one primary renderer matched to browser, native mobile, desktop WebView, and 3D terrain requirements.
- **TerriaJS / deck.gl / loaders.gl** — data exploration, 3D/large-data visualization and loading; the capabilities differ from complete GIS analysis tools.
- **OpenMapTiles / PMTiles / Martin** — generation, archival and delivery of tile data; inspect content and hosted basemap terms separately.

**Rendering rule:** map rendering libraries do **not** inherently provide GDAL-level raster processing, topology, full KML/KMZ fidelity, or desktop project management.

### 4. GIS engines and geometry libraries

- **GDAL/OGR** handles raster/vector I/O and transformations of file formats.
- **PROJ** handles CRS and datum coordinate operations; verify transformation grids, epoch and axis order.
- **GEOS / Shapely / GeoTools / NetTopologySuite** handle geometry and feature programming in their respective runtimes.
- **GRASS GIS / PDAL / WhiteboxTools** supply specialized processing for geoprocessing, point clouds and terrain.
- **GeoPandas / Rasterio / PySTAC / Open Data Cube / eo-learn** supply Python-oriented data, STAC and Earth observation workflows.

**Correctness rule:** for format or algorithm parity, create deterministic input/output fixtures, inspect nodata and metadata, and reopen the output independently.

### 5. Spatial databases, OGC services and deployment

- **PostGIS** supplies spatial SQL on PostgreSQL; **GeoServer / MapServer** publish GIS data and services.
- **Martin / TiTiler / eoAPI** are specialized vector/raster/API delivery components.
- **Nominatim** supplies OSM place-name search; API usage, hosting, privacy and rate limits are independent considerations.
- **Google Earth Engine API** exposes service clients, not a redistributable Earth Engine backend. Check Google Earth Engine access terms separately.

**Server rule:** evaluate OGC compliance, permission boundaries, data lineage, index plans, API limits, cache strategies and clean provisioning; an online demo is not deployability proof.

### 6. Mobile and offline GIS

Use the dedicated [Mobile GIS Application Development skill](../skills/gis-mobile-application-development/SKILL.md), [offline synchronization](mobile-gis-offline-sync.md), [sign-in and offline session guide](mobile-gis-authentication.md), and [Android/iOS device test matrix](mobile-gis-device-test-matrix.md).

**QField** and **Mergin Maps** are representative QGIS-centric field applications. Study touch interaction, GPS permissions, offline GeoPackage/project storage, edits, conflict resolution and sync. Compare **MapLibre Native** where only embedded native map rendering is needed. Mobile apps still require device-native runtime testing.

## How the AI agent must select sources

1. **Prompt intake:** [Intent Orchestrator](../skills/prompt-intent-orchestrator/SKILL.md) normalizes the Arabic/English request, intended delivery target and side effects. The repository entrypoint must require approval for consequential multi-step changes.
2. **Route by target:** [GIS Development Router](../skills/gis-development/SKILL.md) selects only needed specialist skills; this registry is *discovery*, not an execution skill.
3. **Search one canonical catalog:** query [project-catalog.json](project-catalog.json) using [search_catalog.py](../scripts/search_catalog.py), the [bilingual Explorer](../explorer.html#catalog), or the UI. Do not create a second copy of catalog entries elsewhere.
4. **Select at most a few candidates** by actual platform, role (application, library, engine, framework, server), required function, runtime maturity, external dependencies and cost.
5. **Review upstream:** inspect exact commit/release, LICENSE and notices, dependency licensing, genuine source availability, build scripts, required paid GIS hosts, map/data attribution, security advisories and maintenance.
6. **Record integration status:** `reference-only`, `experimental`, or `verified`. All new entries remain `reference-only`; a checked public repository URL does **not** grant `verified` integration.
7. **Plan an independent vertical slice:** open real vector/raster, apply CRS, render/analyze, save/export, read back, package as applicable.
8. **Security + permission:** consult [GIS Security Guardian](../skills/gis-security-guardian/SKILL.md), get explicit host-confirmed authorization where needed, protect credentials and original datasets, and keep rollback.
9. **Test before recommending for production:** T0 source/schema, T1 offline fixtures, T2 actual GIS engine, T3 genuine UI, T4 installed binary. Mark absent tests **NOT RUN**.
10. **Report evidence:** document chosen components, upstream URLs/versions, license findings, runtime tested, benchmarks, output readback, host-specific issues and limits. Never call a project fully verified just because a URL works.

## Repository discovery metadata: important distinction

The catalog's `repository_access_checked_on` means that the **public GitHub repository was found** on that date. `license_metadata_unverified` is a preliminary GitHub license label, not a file-level legal review. `license_verified: false`, `status: unknown`, `last_checked: null` and `native_runtime_verified: false` make this limitation explicit.

The catalog's `category` is an existing UI filter and `primary_skill` on new records points to the *recommended owner*. These do not create an executable integration or a mandatory dependency.

## العربية — خطوات الاستخدام

إذا كنت تريد تطوير برنامج GIS جديد:
1. حدد هل المطلوب برنامج مكتبي، موقع خرائط، تطبيق موبايل، محرك معالجة أو خادم GIS.
2. افتح [مستكشف البرامج والمكتبات](../explorer.html#catalog) وابحث باستخدام الاسم أو التصنيف أو بيئة التشغيل؛ ستظهر أيضًا المشروعات الجديدة من نفس قاعدة البيانات الأساسية.
3. اقرأ مهارة التطوير المناسبة من الجدول بالأعلى ثم قارن المحركات والواجهات المرشحة.
4. تحقق من رخصة **كل مشروع وتبعياته** قبل استخدام الكود. وجود مستودع عام لا يعني السماح بإعادة توزيع الصور والبيانات أو تشغيل خدمات مرخصة.
5. اجعل الوكيل يمر أولًا عبر مهارة فهم الطلب والتوجيه والأمان، ثم اختبر العمل على بيانات GIS حقيقية. لا تعتبر اختبارات التوثيق دليلًا على نجاح التطبيق.

**Maintenance:** Add, update or de-duplicate source URLs only in [project-catalog.json](project-catalog.json). Keep this guide selective and navigational; do not replicate JSON inventory or manually claim tested host compatibility. The [OpenGeoS GIS software guide](gishub-software.md) remains the specialist guide for that ecosystem, and the [data repositories catalog](geospatial-data-repositories.md) remains separate from *software source code*.
