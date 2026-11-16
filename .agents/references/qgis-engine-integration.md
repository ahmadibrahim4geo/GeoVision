# QGIS Engine Integration — official repositories, architecture and reuse boundaries

Use [QGIS Engine Integration skill](../skills/qgis-engine-integration/SKILL.md). The **canonical upstream source is [qgis/QGIS](https://github.com/qgis/QGIS)**. This reference distinguishes the QGIS GIS engine from its complete desktop GUI, a hosted plugin, a browser mapping library or a web service.

## Upstream research map

| Official source | Useful material | Adoption caution |
| --- | --- | --- |
| [qgis/QGIS](https://github.com/qgis/QGIS) | Core, GUI, Analysis, Processing, 3D, Server, CMake, providers and tests | GPL-2.0-or-later; do not paste into MIT projects unreviewed |
| [qgis/QGIS-Documentation](https://github.com/qgis/QGIS-Documentation) | PyQGIS cookbook, stand-alone startup, plugins, user/Server workflows | Check documentation/code-example reuse terms and applicable version |
| [QGIS C++ API](https://api.qgis.org/api/) | `QgsApplication`, `QgsMapCanvas`, raster/vector, coordinate transforms | Match the installed QGIS release |
| [PyQGIS Cookbook](https://docs.qgis.org/testing/en/docs/pyqgis_developer_cookbook/) | Native application initialization and examples | Testing docs follow development; pin stable docs for production |
| [qgis/QGIS-Enhancement-Proposals](https://github.com/qgis/QGIS-Enhancement-Proposals) | Planned feature architecture | Proposals do not prove availability in a release |
| [qgis/qwc2](https://github.com/qgis/qwc2) and [qwc2-demo-app](https://github.com/qgis/qwc2-demo-app) | Web GIS user interface and map service client | Not a native QGIS desktop engine; check each license |
| [qgis/QGIS-Plugins-Website](https://github.com/qgis/QGIS-Plugins-Website) and [plugin directory](https://plugins.qgis.org/) | Plugin discovery and compatibility | Each plugin has its own licensing |
| [QGIS project](https://qgis.org/) | Current downloads, LTR/version and release notes | Do not assume one universal Qt/Python major version |

## Components worth integrating

- **QGIS Core** handles vector/raster layers, projects, data providers, geometry, CRS and coordinate transforms.
- **QGIS GUI** provides `QgsMapCanvas`, tools and widgets for a Qt shell. It is not automatically a full ribbon, globe or installer.
- **Analysis / Processing** provides native algorithms, custom providers and headless processing. Check provider registration and availability for each installation.
- **QGIS 3D** provides supported QGIS scenes, not automatic CesiumJS/KML visual parity.
- **QGIS Server** delivers map/OGC services. Use only when a server is needed and expose services securely.
- **GDAL/OGR, PROJ, GEOS, Qt, PyQt/SIP** are separately versioned foundations with their own license/deployment considerations.

## Route selection

| Need | Architecture | Gate |
| --- | --- | --- |
| QGIS plugin or Processing provider | In-app PyQGIS / Qt plugin | Plugin metadata, lifecycle and supported host versions |
| New desktop GIS using Qt | Qt + QGIS Core/GUI + Analysis where required | ABI, provider paths, GUI threading and packaging |
| Scheduled raster/vector work | `QgsApplication([], False)` headless PyQGIS or validated `qgis_process` | Native runtime, memory, input/outputs, provider availability |
| Existing application using a different UI or map renderer | Keep its own tested UI; optionally connect isolated PyQGIS worker over validated files/IPC | Do not replace renderer or presume host compatibility |
| Published web maps | QGIS Server + separately licensed UI | Auth, OGC compatibility, rate limits, hosting and caching |
| Simple reprojection only | GDAL/PROJ directly when sufficient | No unnecessary QGIS bundle |

## Cross-platform GIS processing adapter blueprint

Keep the existing application responsive and leave its map renderer available even when a QGIS worker fails. This applies equally to desktop, web-client, headless and mixed-language architectures. Propose a versioned request payload with `job_id`, `schema_version`, `operation`, `input_uri`, `output_uri`, `source_crs`, `target_crs` and controlled options; return `status`, `outputs`, `warnings`, `error_code`, `crs`, `units` and `provenance`. **These are proposed adapter fields, not built-in QGIS methods.** Prefer files plus an explicit process boundary first. Reject arbitrary executable paths/SQL, remote resource schemes, oversized jobs and untrusted paths; use limited workers, cancellation and temp-to-atomic outputs.

## Correctness and fidelity boundaries

Check coordinate operations, axis order, datum-grid availability, angular versus projected distance units, elevations and vertical datums, encoding/attributes, geometry counts, invalid geometry, raster NoData and pixel alignment. On GeoPackage/GeoJSON outputs reopen and check types, CRS, count and actual coordinates. On KML/KMZ, **geometry conversion != visually faithful recreation**: route Styles, StyleMap, original icons, KMZ resources, timestamps, gx extensions, ExtendedData and original folder hierarchy to [KML/KMZ contract](google-kml-kmz-compatibility.md).

## Licensing, builds and test evidence

- The QGIS upstream source declares GPL-2.0-or-later. Linking, modification, packaging and related worker separations need case-specific analysis; process isolation **is not an automatic legal exemption**. Verify actual upstream source license, Qt rights, modules/providers, third-party plugins, symbols, imagery and notices before distribution.
- Pin QGIS and related native versions. Test a clean target machine; do not assume general system Python knows the installed QGIS ABI.
- **T0**: requirements/source read. **T1**: offline Python syntax and documentation fixtures. **T2**: real installed QGIS headless run. **T3**: real interactive target-application operations. **T4**: packaged fresh-machine deployment. CI documentation tests **never prove** T2-T4.
- Execute [native sample](../examples/qgis-engine/README.md), [native harness](../native_tests/qgis_acceptance.py) and record [acceptance report](../templates/GIS_ACCEPTANCE_REPORT.md) with observed outputs, limitations and exact runtime.

## Capability/fallback principle

Not every GIS project requires QGIS: the decision router selects it **only when its native GIS functionality is needed**. If the required QGIS runtime/provider is missing, block the QGIS-dependent operation with an actionable error and preserve unrelated map display, file browsing and data workflows. Offer a separately tested GDAL/PROJ alternative only where feature and license parity were checked. Every claimed integration must record its host version and native test results; an example in this repository is not proof that all applications can run it.
