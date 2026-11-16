# OpenGeoHub / OpenLandMap Integration Reference & Data Explorer Specification

Reviewed: 2026-10-30. **T0 only:** Reference and architectural design, not a functioning Z-EARTH feature, verified API, or pre-tested layer.

---

## 1. Five Curated OpenGeoHub / OpenLandMap Repositories

The five upstream repositories added to the catalog provide research algorithms, spatial data modeling patterns, and cartographic symbology. They are classified as **reference sources** (`integration: reference-only`), not drop-in dependencies or execution engines.

| Repository | Purpose | Languages & Tools | License & Evidence | Primary Skill | Registry ID |
|---|---|---|---|---|---|
| [openlandmap/scikit-map](https://github.com/openlandmap/scikit-map) | Spatial Machine Learning & prediction | Python, scikit-learn, rasterio, GDAL | **MIT** (verified at repo root; independent datasets excluded) | `gis-code-discovery` | `openlandmap-scikit-map` |
| [openlandmap/soildb](https://github.com/openlandmap/soildb) | Soil profile harmonization & modeling | Python, R, SQL, spatial scripts | **verify-upstream** (no root LICENSE found; unverified) | `geospatial-data-processing` | `openlandmap-soildb` |
| [openlandmap/GEDTM30](https://github.com/openlandmap/GEDTM30) | 30 m ensemble global elevation model | Python, GDAL, Bash | **verify-upstream** (no root LICENSE found; data varies) | `terrain-hydrology-lidar` | `openlandmap-GEDTM30` |
| [opengeohub/spatial-prediction-eml](https://github.com/opengeohub/spatial-prediction-eml) | Ensemble ML for spatial prediction | R (`mlr`, `ranger`, `sp`, `raster`) | **CC-BY-SA-4.0** (verified at repo root; share-alike terms) | `gis-code-discovery` | `opengeohub-spatial-prediction-eml` |
| [openlandmap/spatial-layers](https://github.com/openlandmap/spatial-layers) | Layer definitions, legends & color ramps | Python, JSON, SLD, QML, CartoCSS | **GPL-3.0** (verified at repo root; copyleft) | `gis-cartography-symbology` | `openlandmap-spatial-layers` |

### Detailed Repository Usage Boundaries

#### 1. `scikit-map` (Spatial Machine Learning)
- **When to use:** When developing spatial prediction pipelines, spatial cross-validation (spatial block CV to avoid spatial autocorrelation data leakage), training models with raster covariate stacks, and spatial feature extraction.
- **When NOT to use:** Do not use for generic non-spatial tabular ML (use standard `scikit-learn`). Do not assume memory scalability on massive full-resolution satellite mosaics without chunking. Do not copy code into proprietary products without retaining the MIT copyright notice.

#### 2. `soildb` (Soil Databases & Pedometric Modeling)
- **When to use:** When researching soil observation schemas, pedometric variable names, SoilGrids-compatible depth intervals (e.g. 0-5cm, 5-15cm, 15-30cm), and environmental covariate correlation methods.
- **When NOT to use:** **Direct copy prohibited** (no root LICENSE file found; unverified status). Do not treat as an installable Python package or live database API. Use only for conceptual methodology reference.

#### 3. `GEDTM30` (Global Elevation & Digital Terrain Model 30m)
- **When to use:** Conceptual and algorithmic reference for multi-source DEM fusion (SRTM, ALOS AW3D30, Copernicus GLO-30, NASADEM) to produce void-free 30 m digital elevation models.
- **When NOT to use:** **Direct copy prohibited** (no root LICENSE file found). Do not assume an active live DEM tile stream exists at this URL. Do not use for engineering-grade vertical datum transformations without independent ellipsoid/orthometric height checks.

#### 4. `spatial-prediction-eml` (Ensemble Machine Learning for Spatial Prediction)
- **When to use:** When studying ensemble spatial prediction methods, stacking algorithms, meta-learners, model error quantification, and spatial uncertainty modeling in R.
- **When NOT to use:** Do not copy or adapt into proprietary closed-source applications without complying with **CC-BY-SA-4.0** (ShareAlike copyleft clause). Do not attempt to run R scripts inside pure Python or C# .NET environments without an explicit runtime bridge.

#### 5. `spatial-layers` (Spatial Layer Definitions & Cartography)
- **When to use:** When designing scientific color palettes, classification breaks, and standard legends for environmental layers (land cover, soil properties, bioclimatic indices, vegetation).
- **When NOT to use:** Do not copy code or styling scripts into closed-source proprietary software without complying with **GPL-3.0**. Do not assume symbology automatically translates to ArcGIS layer files (`.lyrx`) without format conversion.

---

## 2. Geospatial Data Discovery & Service Protocol Distinction

When discovering or integrating OpenGeoHub and OpenLandMap datasets, the agent must distinguish between different data delivery protocols and avoid presuming hosted tile availability.

```
                           ┌───────────────────────────────┐
                           │    User Spatial Data Goal     │
                           └───────────────┬───────────────┘
                                           │
                    ┌──────────────────────┴──────────────────────┐
                    ▼                                             ▼
       ┌────────────────────────┐                    ┌────────────────────────┐
       │   Static STAC Catalog  │                    │   Dynamic STAC API     │
       │ (openlandmap/catalog)  │                    │ (/search, CQL2, bbox)  │
       └────────────┬───────────┘                    └────────────┬───────────┘
                    │                                             │
                    │ Tree traversal (GET)                        │ Query filters (POST/GET)
                    ▼                                             ▼
       ┌──────────────────────────────────────────────────────────────────────┐
       │                       STAC Item / Asset Metadata                     │
       └──────────────────────────────────┬───────────────────────────────────┘
                                          │
            ┌─────────────────────────────┼─────────────────────────────┐
            ▼                             ▼                             ▼
┌───────────────────────┐     ┌───────────────────────┐     ┌───────────────────────┐
│     Cloud-Native      │     │    OGC Map Service    │     │   Full Raster Asset   │
│   GeoTIFF (COG)       │     │     (WMS / WMTS)      │     │  (Downloadable File)  │
│  HTTP Range Requests  │     │ Map tile server / GIS │     │  Offline local store  │
└───────────────────────┘     └───────────────────────┘     └───────────────────────┘
```

### Protocol Comparison

| Protocol / Asset Type | OpenLandMap / OpenGeoHub Reality | Access Method | Quota & Auth | Agent Guidance |
|---|---|---|---|---|
| **Static STAC Catalog** | Published at `https://s3.eu-central-1.wasabisys.com/stac/openlandmap/catalog.json` | Recursive HTTP GET on linked JSON files (`catalog` -> `collection` -> `item`) | Open access; no API key required; rate-limit gently | **Never equate with a STAC API.** Cannot submit dynamic `/search` POST queries; agent must traverse the static link tree. |
| **STAC API** | `https://stac.openlandmap.org/` serves catalog landing pages; dynamic search endpoint availability must be checked | HTTP REST (`/collections`, `/items`, `/search`) | Open access when active | Always test response status code and schema before relying on dynamic `/search`. Fallback to static catalog traversal. |
| **Cloud Optimized GeoTIFF (COG)** | Primary delivery mechanism for scientific rasters (hosted on Wasabi S3 storage) | HTTP GET with `Range:` headers (byte-serving) | Open read-only; no credentials required | Use a COG-aware reader (GDAL `/vsicurl/`, Rasterio, QGIS COG driver). Do **not** download the multi-gigabyte file for map viewing. |
| **OGC WMS / WMTS** | Not universally exposed across all layers; some legacy or project-specific endpoints exist | Standard OGC `GetCapabilities`, `GetMap`, `GetTile` | Varies per server instance | **Never invent WMS URLs.** If WMS is missing, use an opt-in self-hosted local tiler (e.g. TiTiler) proxying the COG. |
| **XYZ / TMS Slippy Tiles** | Available only when backed by an active tile cache or dynamic raster tiler | Standard template `/{z}/{x}/{y}.png` | Depends on tiler | Do not assume XYZ tiles exist for every COG; verify tile HTTP response code 200 before wiring map renderers. |
| **Downloadable Raster Data** | Complete GeoTIFF or NetCDF scenes linked under STAC item assets | Direct HTTP download | Open access; check bandwidth | Inform the user of file size and spatial resolution before downloading. Never bulk-download without confirmation. |

### Access, Coverage & Licensing Summary
- **Official Portals:** [OpenGeoHub](https://opengeohub.org/), [OpenLandMap](https://openlandmap.org/), [OpenLandMap Docs](https://docs.openlandmap.org/), [STAC Docs](https://docs.openlandmap.org/020-stac.html).
- **Authentication:** Public access, no authentication headers required for public layers. No API secrets to store.
- **Licensing:** Varies per layer (ODbL, CC-BY 4.0, CC-BY-SA 4.0). Must check `item.properties.license` in the metadata.
- **Spatial Resolution:** Product-dependent (e.g., 30 m for GLC_FCS30D land cover; 250 m to 1 km for global soil properties and bioclimatic layers).
- **Spatial Coverage:** Global terrestrial coverage, with product-specific NoData masks (e.g. deserts, oceans, high-latitude ice).
- **Temporal Coverage:** Multi-epoch where documented (e.g., GLC_FCS30D land cover covers 1985–2022 with 5-year intervals before 2000 and annual intervals thereafter). Later years only exist if indexed and verified in STAC items.

### Programmatic Integration: Live OpenGeoHub STAC API

The OpenGeoHub STAC service exposes an interactive STAC Browser at `https://stac.opengeohub.org/` and an OpenAPI 3.1 REST API at `https://stac.opengeohub.org/v1/` (catalog entry `opengeohub-stac-api`).

- **Base Landing URL:** `https://stac.opengeohub.org/v1/`
- **OpenAPI Schema:** `https://stac.opengeohub.org/v1/api`
- **Catalogs Endpoint:** `https://stac.opengeohub.org/v1/cat` (`openlandmap`, `ecodatacube`, `landmetric`)
- **Search Method:** `POST https://stac.opengeohub.org/v1/cat/{catalog_id}/search` (Note: GET returns 405 Method Not Allowed; always submit JSON payload via POST).

#### Python Client Example (Requests / PySTAC)

```python
import requests

def search_openlandmap_cogs(bbox, limit=5):
    """Query OpenGeoHub STAC API for OpenLandMap COG assets and styling."""
    url = "https://stac.opengeohub.org/v1/cat/openlandmap/search"
    payload = {
        "limit": limit,
        "bbox": bbox  # [min_lon, min_lat, max_lon, max_lat]
    }
    headers = {"User-Agent": "GIS-Development-Skills/1.0", "Content-Type": "application/json"}
    
    resp = requests.post(url, json=payload, headers=headers, timeout=15)
    resp.raise_for_status()
    features = resp.json().get("features", [])
    
    results = []
    for item in features:
        item_id = item["id"]
        assets = item.get("assets", {})
        cog_urls = [a["href"] for a in assets.values() if "image/tiff" in a.get("type", "")]
        qml_styles = [a["href"] for k, a in assets.items() if "qml" in k.lower()]
        sld_styles = [a["href"] for k, a in assets.items() if "sld" in k.lower()]
        results.append({
            "id": item_id,
            "cogs": cog_urls,
            "qml": qml_styles,
            "sld": sld_styles
        })
    return results

if __name__ == "__main__":
    items = search_openlandmap_cogs(bbox=[24.0, 21.0, 37.0, 32.0], limit=2)
    for it in items:
        print(f"Dataset: {it['id']}")
        print(f"  COG: {it['cogs']}")
```

---

## 3. Z-EARTH Data Explorer Specification (View-Only Design)

This specification defines the architectural contract for adding external scientific data exploration to the Z-EARTH desktop/web application.

### UI & UX Architecture
1. **Collapsible Ribbon / Dockpane:** Add a dedicated **Data Explorer** panel. Do not destroy or replace existing ribbons or force options into dropdown-only menus.
2. **Bilingual Navigation:** Complete English and Arabic RTL support. Labels, tooltips, category descriptions, and error notices must be localized.
3. **5-Step Selection Hierarchy:**
   $$\text{Category} \longrightarrow \text{Verified Provider} \longrightarrow \text{Collection} \longrightarrow \text{Variable / Band} \longrightarrow \text{Available Epoch (Date)}$$
4. **Categories:** Land cover / land use, soil properties, vegetation indices, bioclimatic variables, digital elevation, hydrology, and marine variables.
5. **Separation of Adapters:**
   - **Discovery Adapters:** Static STAC, STAC API, OGC Catalog, or local JSON fixture.
   - **Render Adapters:** Categorical raster (discrete palette), continuous raster (gradient ramp), vector contours, 3D terrain mesh.

### Legend & Symbology Contract
- **Categorical Data:** Display official numeric class codes, names, hex colors, and NoData.
- **Continuous Variables:** Display physical measurement units (e.g. $\text{g/kg}$, $\text{m}$, ${}^\circ\text{C}$), min/max value range, and color gradient ramp.
- **Dockable & Responsive:** The legend must auto-synchronize to the currently selected layer and epoch. Include opacity slider (0–100%) and layer visibility toggle.
- **Provenance & Citation:** Every active layer must display its original publisher, citation link, and license notice.

### Timeline & Multi-Epoch Handling
- **Discrete Discovered Epochs:** Read actual `datetime` or `start_datetime` from STAC items. Never interpolate years or generate synthetic time slider ticks for years that have no underlying raster asset.
- **Irregular Time Steps:** Support unequal gaps (e.g. 5-year intervals 1985–2000, 1-year intervals 2000–2022).
- **Static Products:** Disable or hide the timeline slider when viewing time-invariant datasets (e.g. bedrock elevation, long-term climate normals).
- **Update Discovery:** Metadata caches must have a bounded TTL (e.g. 24 hours). Refresh metadata on user demand. Verify new epochs before displaying them in the timeline.

### Security & Integrity Gates
- **SSRF Prevention:** If an external raster tiler (such as TiTiler) or proxy is used, enforce strict URL allowlists limited to verified provider domains (`*.wasabisys.com`, `*.openlandmap.org`, `*.opengeohub.org`).
- **No Injected Secrets:** Do not commit API keys or private tokens.
- **No Bulk Scraping:** Cache metadata locally; respect HTTP `Cache-Control` and `ETag`.

---

## 4. Verification and Acceptance Gaps (T0–T4)

- **T0 (Reviewed Reference):** This specification, repository catalog entries, and source JSON registries.
- **T1 (Deterministic Offline Fixtures):** Offline JSON fixtures of OpenLandMap STAC catalog, sample item metadata, and mock timeline responses in `fixtures/` or unit tests.
- **T2 (Live Accessible Sample):** Verified HTTP GET to `https://s3.eu-central-1.wasabisys.com/stac/openlandmap/catalog.json` returning 200 OK and valid STAC JSON.
- **T3 (Desktop / UI Automation):** Windows UI Automation / WebView2 testing verifying panel collapse, layer addition, legend display, and timeline stepping. Marked as **NOT RUN** until native tests are conducted.
- **T4 (Packaged Application):** Full installer smoke test with live or cached scientific layers. Marked as **NOT RUN** until release build.

---

## 5. ملخص باللغة العربية (Arabic Summary)

- **المستودعات الخمسة:** أُضيفت مستودعات OpenGeoHub وOpenLandMap كـ**مراجع برمجية وعلمية** وليست أدوات تنفيذية جاهزة. مستودعات `scikit-map` (ترخيص MIT) و`spatial-prediction-eml` (ترخيص CC-BY-SA-4.0) و`spatial-layers` (ترخيص GPL-3.0) موثقة التراخيص، بينما `soildb` و`GEDTM30` لا تملكان ترخيصاً صريحاً في الجذر وتعتبران مراجع مفاهيمية يمنع نسخ كودهما مباشرة.
- **اكتشاف البيانات والبروتوكولات:** فهرس STAC الخاص بـ OpenLandMap هو فهرس استاتيكي (Static STAC Catalog) مخزن على S3 ولا يدعم بالضرورة البحث الديناميكي (`/search`). يتم الوصول إلى البيانات عبر مرئيات COG تدعم طلبات البايت (HTTP Range) ولا يُفترض وجود خوادم WMS أو XYZ جاهزة لكل طبقة دون التحقق.
- **واجهة استعراض البيانات في Z-EARTH:** تصميم الواجهة يعتمد على التدفق: (القسم ← المصدر المعتمد ← المجموعة ← المتغير ← الفترة الزمنية المؤكدة ← الإضافة للخريطة)، مع دعم كامل للغة العربية ودليل خريطة متزامن وشريط زمني يقتصر على التواريخ الفعلية فقط دون اختراع فترات زمنية وهمية.
