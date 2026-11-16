# Esri Projection Engine + SpatialReference.org + PROJ: integration specification

Reviewed: 2026-10-24. The official sources are **not interchangeable**:

| System | Authoritative for | Not a replacement for |
|---|---|---|
| [Esri Projection Engine DB documentation](https://github.com/Esri/projection-engine-db-doc) (Apache-2.0 repository) | Esri factory object codes, metadata, WKT, areas, transformations, `wkid` / `latestWkid` | ArcGIS Pro exact GUI folder tree; full Esri executable projection engine |
| [ArcGIS Pro coordinate-system folder structure](https://doc.esri.com/en/arcgis-pro/latest/help/mapping/properties/coordinate-system-folder-structure.html) | Folder labels, hierarchy rules, special US and Zone Series cases | Machine-readable code definitions |
| [SpatialReference.org](https://spatialreference.org/about) ([source](https://github.com/OSGeo/spatialreference.org), MIT repository) | PROJ-derived EPSG, ESRI, IAU:2015, IGNF, NKG, OGC authority pages and exports | Guaranteed available datum transformation operation |
| [PROJ / proj.db](https://proj.org/) | Standards-based CRS identification, transformation pipelines and grids, when installed | Esri native naming/hierarchy or Esri-compatible exact output in every version |

## Exact Esri UI terms, safely modeled

```text
Available Coordinate Systems
├── Horizontal systems [tab]
│   ├── Favorites
│   ├── Layers
│   ├── Geographic Coordinate Systems
│   │   └── [official region] / [country] / [CRS]
│   ├── Projected Coordinate Systems
│   │   └── [official region] / [country] / [CRS]
│   │       └── Zone Series / [series] / [datum] / [zone]  (where applicable)
│   └── Custom
└── Vertical systems [tab]
    ├── Favorites
    ├── Layers
    ├── Vertical Coordinate Systems
    │   ├── Ellipsoidal-based
    │   └── Gravity-related
    └── Custom
```

The display above is a **model**, not a verbatim dump from Esri software. Within geographic folders Esri groups by region, then by country; countries/groups with four or more CRSs may get subfolders. UTM/Gauss–Krüger zone series get `Zone Series` folders. State Plane systems have special North America / United States / state nesting. ArcGIS Pro keeps horizontal and vertical favorites on separate tabs; the Favorites folder holds shortcuts, not source CRS definitions. Some favorites can be nested via local `.prj` directories. For exact region/country paths use **verified Esri mapping only**, not auto-classification of `areaname`. See official docs above.

## Esri database directory inventory (confirmed against repository)

- `json/pe_list_<objtype>.json` and `csv/pe_list_<objtype>.csv`: structured objects. Known families include `geogcs` (geographic CRS), `projcs` (projected CRS), `vertcs` (vertical CRS), `hvcoordsys`, `gxyzcs`, `geogtran` and `verttran` (datum operations), `datum`, `vdatum`, `spheroid`, `primem`, `projection`, `method`, `parameter`, `angunit`, `linunit`, `areaunit`.
- `text/`: companion tabular listings; `objedit/`: user object definition format.
- `shp/`, `fgdb/pe_area_extents.gdb`: area-of-use geometries for objects with extents.
- `gdal/`, `proj4/`: historical interchange data; **do not blindly overwrite modern installed GDAL/PROJ databases** with repository legacy files.
- `other/`: docs including `PE_user_defined_objects.pdf` and projection notes.

Esri's README currently identifies PE 13.7.0 for ArcGIS Pro 3.7, incorporating EPSG 12.049 subject to stated exclusions. Store source commit/release, PE/EPSG/target GIS version for reproducibility; do not assume newest database definition is identical in ArcMap.

### Critical attributes and schema

Published CSV columns commonly include `wkid,latestWkid,macro,name,wkt,wkt2,description,authority,version,deprecated,areaname,slat,nlat,llon,rlon`. Some object families omit extent fields or add `epoch`, `model`; parse by header rather than positional offsets. JSON top-level arrays are grouped under object-type names (e.g., `AngularUnitsOfMeasure`). One WKID can change: `wkid` may be the original and `latestWkid` the new one. Authority must not be inferred from numeric range.

- `slat, nlat, llon, rlon` are geographic bounds **in degrees from Greenwich**, not projected meters.
- Transformation accuracy values published by Esri are in **meters**, when available; inspect grid/data dependencies separately.
- Treat `deprecated: yes` as a warning and search for an endorsed replacement; do not silently rewrite persisted CRS metadata without reporting.
- Prefer WKT2 for modern interchange; use Esri WKT for Esri compatibility where appropriate. PROJ.4 strings are legacy and can lose vertical/modern datum semantics.

### SpatialReference.org URLs

Base canonical form: `https://spatialreference.org/ref/<authority>/<code>`, for example `https://spatialreference.org/ref/epsg/4326`. Documented endpoints below are *examples of published patterns*; their availability is subject to website versions:

- `.../prettywkt2.txt` — WKT2:2019
- `.../prettywkt.txt` — WKT1
- `.../esriwkt.txt` — Esri WKT, useful when working with `.prj`
- `.../projjson.json` — PROJJSON
- `.../proj4.txt` — legacy, potentially lossy

Catalog authorities include **EPSG, ESRI, IAU:2015, IGNF, NKG, OGC**. Its content is rebuilt from PROJ's SRS database at PROJ releases; it is *not* the Esri PE dataset and cannot guarantee equivalent codes/definitions for every Esri software generation. For interactive filtering, compare [CRS Explorer](https://crs-explorer.proj.org/).

## Integration workflow: same naming, operational correctness

1. **Factory source:** keep the original `Esri:name`, `wkid`, `latestWkid`, `authority`, object family, Esri PE source-version and WKT without overwriting metadata.
2. **UI layer:** display verified ArcGIS folder names and axes. Favorites = user preferences, Layers = active layers, Custom = user definitions. Geographic folders = official mapping when available; otherwise label as **area-of-use suggestion**.
3. **Lookup:** match authority+code, then alias WKIDs; detect duplicate codes across object types. Search all descriptive fields and source area names.
4. **Suitability:** check spatial footprint intersects area of use, object deprecation, angular/linear units, projection distortion goal, transformation grid support and vertical datum.
5. **Execution:** apply transformations with tested runtime (`arcpy`, `pyproj`, PROJ, `osgeo.osr`, or `QgsCoordinateTransform`); do not execute a transformation by manipulating WKT strings.
6. **Display/export:** retain CRS axis order rules, floating-point precision, WKT2/Esri WKT distinction, and metadata provenance.
7. **Acceptance:** known control-point transformation + inverse tolerance, Earth extent/date line, mixed vertical/horizontal, a missing-grid failure, and a deprecated `wkid` alias.
8. **Security/operations:** check licensing, local packaging of `proj.db` and datum grids, version pinning, no unauthorized network downloads, and no silently assumed file format support.

### CLI demonstration

Download the official repository separately (do not bundle its multi-megabyte dataset into this skills repository). Then:

```bash
python scripts/crs_catalog.py --source path/to/projection-engine-db-doc/csv --tree
python scripts/crs_catalog.py --source path/to/projection-engine-db-doc/csv --kind geogcs --query Egypt
python scripts/crs_catalog.py --source path/to/projection-engine-db-doc/json --kind projcs --bbox 24 22 37 32 --format json
python scripts/crs_catalog.py --source path/to/projection-engine-db-doc/csv --favorite-add EPSG:32636 --favorites my-favorites.json
```

The CLI inspects source records and persists user favorites. It does **not** contain a transformation implementation or exact private Esri GUI per-country folder mapping.

## Platform-specific implementation notes

- **ArcMap:** ArcGIS Desktop 10.8.2 is legacy; keep Python 2.7 / ArcPy-specific code separate. Do not import the new standalone Python 3 catalog CLI inside ArcMap's Python 2 runtime.
- **ArcGIS Pro:** `arcpy.SpatialReference`, `arcpy.ListTransformations`, `Project`/`ProjectRaster`; distinguish assigning vs transforming spatial reference and record the selected operation.
- **QGIS:** `QgsCoordinateReferenceSystem`, `QgsCoordinateTransformContext`, `QgsCoordinateTransform`; inspect available geodetic operations.
- **Desktop / GIS engines:** PROJ + proj.db; use `pyproj.CRS` and `pyproj.TransformerGroup` or equivalent to show grid availability/accuracy. GUI labeling should not silently replace the runtime CRS authority registry.

## Attribution

Original Esri PE data/docs: © Esri, Apache-2.0 (see its repository). SpatialReference.org source: MIT; generated CRS records originate from multiple authorities with their own provenance. This project's documentation and CLI are newly authored MIT code; no third-party source code or bulk factory tables are redistributed.
