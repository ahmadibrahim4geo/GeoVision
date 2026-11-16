# Free-first GIS development policy

**Reviewed 2026-10-25. Default for general GIS work: no mandatory paid license, subscription, cloud credits, billed AI, or expiring trial.** The MIT-licensed skills/documentation may be used freely; external GIS host products, libraries, datasets, hosted APIs and model weights have separate legal and operational terms.

## Execution selection rules

1. Start with locally installable open-source alternatives when feasible: QGIS/PyQGIS, GDAL, PROJ, GEOS/Shapely, Rasterio, GeoPandas, PostgreSQL/PostGIS, SQLite/SpatiaLite, GRASS GIS, PDAL, MapLibre, Leaflet and CesiumJS. Confirm each license/version.
2. When the user explicitly needs ArcGIS/ArcPy or Global Mapper/.gms/SDK, do not silently replace the vendor environment: disclose the required commercial host and suggest an independent **analogous** free workflow without claiming proprietary API compatibility.
3. Free academic/noncommercial, registered, keyed, fair-use, quota-limited and evaluation offers have restrictions. Never treat free download or a public map tile/metadata search as unlimited legally reusable source data.
4. Prefer offline/no-key execution; if a registered free tier is necessary, disclose request/data quotas, attribution, caching rights, eligible users, privacy, and any later cost. Do not hide personal API keys in client bundles or require billing silently.
5. A model-running agent must have deterministic local actions without purchasing a hosted LLM. Local/GPU computation and data transfer can still incur practical expenses. Earth Engine and hosted cloud datasets may require eligible accounts or quotas.
6. If a free solution is not equivalent in accuracy/outputs to a paid GIS feature, state the limitation; do not invent feature parity.

## All 35 specialists

| Specialist | Free independent path | Optional dependency or limitation |
| --- | --- | --- |
| `gis-development` | Free decision router, offline fixtures and Python examples | Optional commercial hosts only by explicit request |
| `gis-code-discovery` | Free deterministic offline search across vetted open-source snippets and recipes (MIT/Apache/BSD/QGIS); no paid AI/API required | Public repositories and documentation have independent licenses; proprietary SDKs (e.g. ArcGIS Pro SDK) require host licenses for native execution |
| `gis-architecture-visualization` | Local Archify interactive HTML with editable JSON; no paid renderer/API key; offline route/plan validation | External Archify runtime (reviewed Apache-2.0 source), Node.js and a usable browser for browser checks; host setup permissions, source privacy and incomplete upstream Arabic RTL support remain explicit |
| `clean-room-reference-implementation` | Free read-only reference-file metadata observation, independently authored open-source implementations, deterministic local audit scripts and public KML/OGC standards | Proprietary program binaries/shaders/imagery remain restricted; native Windows tests, authorized agents and external services require separate verification; no mandatory paid provider |
| `completion-evidence` | Free Python standard-library report consistency checker; no API key or hosted AI required | Host permissions, real command execution, provider receipts and native T2–T4 checks are not performed by this offline validator |
| `agent-development-journal` | Free local Python 3 standard-library numbered project logs, hash checks, repair and test suite | Requires only project file permissions; host/GIS runtime and secure storage remain separate |
| `prompt-intent-orchestrator` | Offline structured-task planning, standard-library JSON validation and user-review workflow; optional locally hosted language model | Actual AI model, agent tool access, authenticated approvals and proprietary GIS execution depend on the host and may require separate permissions or costs |
| `arcgis-toolbox-engineering` | Review toolbox source; reimplement analogous operations with QGIS/PyQGIS/GDAL | ArcPy execution requires suitably licensed ArcGIS Pro/ArcMap |
| `global-mapper-development` | Use QGIS/GDAL/Whitebox for similar raster/vector tasks | Global Mapper and SDK/DLL require commercial licenses |
| `qgis-plugin-development` | QGIS/PyQGIS and Processing with open-source licensing | Optional third-party plugins and providers may charge |
| `qgis-engine-integration` | Native QGIS Core/GUI/Processing in a compatible Qt app or headless PyQGIS worker; WPF/Cesium can use an isolated GIS worker | QGIS GPL-2.0-or-later and separate Qt/provider/asset rights require review; install/test actual matching QGIS runtime |
| `coordinate-reference-systems` | PROJ/pyproj and QGIS CRS metadata | Esri factory metadata does not distribute its projection engine |
| `geospatial-data-processing` | GDAL, Rasterio, Shapely, GeoPandas | External data/formats have independent terms |
| `terrain-hydrology-lidar` | GRASS GIS, PDAL, GDAL, license-permitted Whitebox | ArcGIS/Global Mapper extensions are optional |
| `remote-sensing-pipelines` | STAC/public imagery assets after license checks, GDAL/Rasterio/xarray | GEE/cloud processing or AI may have account/quota requirements |
| `spatial-databases` | Local PostgreSQL/PostGIS or SQLite/SpatiaLite | Managed hosted databases may charge |
| `geospatial-web-mapping` | MapLibre/Leaflet/CesiumJS with permitted local/self-hosted layers | Hosted imagery/terrain/maps may charge or forbid caching |
| `gis-desktop-application-development` | .NET, Tauri and free GIS engines under their licenses | ArcGIS/Global Mapper runtime DLLs need paid licensing |
| `gis-mobile-application-development` | Free/native Android/iOS SDKs with license-reviewed MapLibre/Qt/QGIS adapters and local geodata; deterministic offline sync tests | Signing, native hardware tests, optional provider accounts and map data rights require separate verification and user permission |
| `geospatial-data-discovery` | Metadata catalogs and independently permitted free datasets | Downloading or redistributing assets may be restricted |
| `geospatial-data-catalogs` | Offline Python source directory search | A directory is not a free scientific data download |
| `geospatial-api-integration` | Locally served endpoints and verified conditional free-tier sources | Trial/billed providers not default |
| `geospatial-ai-agents` | Deterministic PyQGIS/CLI agent, optional permitted local models | Commercial LLM tokens/cloud/GPU cost not mandatory |
| `mcp-integration-and-tool-orchestration` | Free stdlib offline MCP/native candidate router and open protocol references; native tool fallback when authorized | Actual MCP servers, host integration, credentials, network access, operating systems, GIS licenses and T3/T4 runs are separate |
| `gis-cartography-symbology` | QGIS/PyQGIS, MapLibre Style JSON, custom palettes and fully licensed upstream open style sources | ArcMap/Pro require host licenses; original Esri/USGS/BGS fonts/styles retain per-asset rights |
| `visual-asset-discovery-integration` | Project-driven discovery from current provider/source websites and any relevant internet source; named catalogs are optional examples; local Pillow preparation and host design tools | Review each asset license and brand terms; source/code license does not license all imagery. Actual network/generation tools and target UI must be available; no default paid provider |
| `system-capabilities-and-permissions` | Native browser/OS capability APIs and free local date/time adapters; optional open-source Python/Qt/.NET capability wrappers after license check | Hardware, sensor location, notifications, WebView origins, provider quotas and OS user consent must be verified; no default paid API or implicit permissions |
| `gis-device-context` | Offline UTC/IANA timezone parsing with explicit coordinates; consented native/browser location API if supported | Precise geolocation needs permissions and hardware/services; Sunlight rendering needs real engine |
| `gis-internationalization-arabic` | UTF-8/RTL/BiDi localization, open-source Unicode/ICU tooling, native GIS Arabic-label fixtures | Verify Arabic fonts, shaping and host-specific GeoPackage/ArcGIS/QGIS exports; commercial host licensing remains independent |
| `kml-kmz-engineering` | Open-source ZIP/XML safety checks, KML parsing and GDAL/OGR/QGIS where supported | ArcGIS/Google Earth native runtime proprietary; source conversion may lose unsupported KML features |
| `gis-ui-command-audit` | Free read-only Python standard-library source inventory for HTML/XAML GIS controls and command IDs | Native handler verification needs the target app and optionally free UIA/Qt tools; GitHub code access is not a runtime license |
| `gis-desktop-e2e-testing` | Open-source FlaUI/pywinauto and eligible Playwright/Qt UI tests with synthetic data | Native Windows UI Automation requires Windows interactive session and permitted app; ArcGIS/Global Mapper host licenses stay separate |
| `gis-data-flow-diagnostics` | Free local JSON trace validation and GDAL/QGIS-capable independent readback where licensed | Real GIS engines, data service permissions and optional remote telemetry require separate authorization |
| `gis-ui-ux-design-testing` | Free design documentation, keyboard/manual checks, open-source Penpot/axe and reusable CSS/Qt/WPF patterns | Native assistive technology and proprietary UI frameworks or design services may have platform/license requirements |
| `gis-security-guardian` | Offline Python 3 standard-library security audits and ZIP/KMZ archive guards; optional open-source scanners by risk | Third-party scanning services may require network/API keys; policy text does not enforce OS sandbox |

## API classification (catalog review, not live verification)

The 52 versioned API entries currently classify as **42 conditional free**, **1 self-hosted open source**, **3 restricted public**, **1 trial-only**, **4 commercial/metered**, and **1 temporarily unavailable**. Filter out commercial, trial, restricted and unavailable providers from free-only usage by default. Individual terms and upstream outages can change.

**Official grounding:** [QGIS free download](https://www.qgis.org/download/), [Esri ArcGIS Pro trial](https://www.esri.com/en-us/arcgis/products/arcgis-pro/trial), [Global Mapper SDK licensing](https://support.globalmapper.com/hc/en-us/articles/53166637315860-Licensing).

## Verification

- Distinguish a freely readable vendor-specific skill from free licensed execution.
- All specialists link this document and provide a distinct no-cost path in the matrix.
- Data/API catalog metadata must expose access tier without claiming live provider or commercial entitlement.
- CI validates static counts, labels, links and fallback logic. Real native GIS/browser/hosted API acceptance is a separate test tier.
