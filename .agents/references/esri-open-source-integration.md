# Esri open-source integration matrix — GIS Development Skills

Audit expanded: 2026-10-25. All listed GitHub repository **paths** were confirmed to exist; presence on public GitHub does not itself grant code redistribution, mapping-service or commercial-product rights. Unless an actual project license is specifically documented below, status is **verify-upstream**. Integrate as workflow and architecture *references first*, then pin tested versions and check notices before importing code.

| Official repository | Specific use in this skills project | Routing | Adoption boundary |
|---|---|---|---|
| [Esri/arcgis-pro-sdk-snippets](https://github.com/Esri/arcgis-pro-sdk-snippets) | AI-assisted Pro SDK C# Editing, Geodatabase, Map Exploration and Layout examples | ArcGIS Pro add-in | Apache-2.0 source; Pro 3.7 and .NET 10 required for current samples |
| [Esri/arcpy](https://github.com/Esri/arcpy) | ArcPy debugging and release-indexed Python package matrix | ArcGIS Toolbox Engineering | Use the [offline compatibility guide](esri-python-environment-compatibility.md); not a replacement for ArcGIS Pro |
| [Esri/arcade-expressions](https://github.com/Esri/arcade-expressions) | Profile-specific labeling, popup, renderer and attribute rules | GIS Cartography | Apache-2.0 examples; match Arcade profile and host version |
| [Esri/developer-support](https://github.com/Esri/developer-support) | Proof-of-concept Python, .NET and JS debugging | GIS development | Apache-2.0 code; inspect sample-specific runtime |
| [Esri/cad-bim-scripts](https://github.com/Esri/cad-bim-scripts) | CAD/BIM and Civil 3D ArcGIS scene processing | ArcGIS Toolbox Engineering | Apache-2.0 code; requires Pro 3.4+ and relevant tools |
| [Esri/large-network-analysis-tools](https://github.com/Esri/large-network-analysis-tools) | Parallel chunked OD/network analysis Python tools | ArcGIS Toolbox Engineering | Apache-2.0 code; licensed network and possible credits required |
| [Esri/geoportal-server-catalog](https://github.com/Esri/geoportal-server-catalog) | STAC/MCP metadata catalog discovery | Geospatial Data Catalogs | Apache-2.0 source; Geoportal v4 server breaking changes, external asset rights |
| [Esri/jsapi-resources](https://github.com/Esri/jsapi-resources) | JavaScript ArcGIS SDK templates and OAuth callback | Web Mapping | Apache-2.0 source; SDK and online service terms separate |
| [Esri/esri-leaflet-geocoder](https://github.com/Esri/esri-leaflet-geocoder) | Leaflet geocoder control | API Integration | Apache-2.0 code; ArcGIS geocoder service account and terms separate |
| [Esri/elevation-gp-python](https://github.com/Esri/elevation-gp-python) | Elevation viewshed/profile GP tools and services | Terrain / ArcGIS Toolboxes | Apache-2.0 code; Spatial Analyst and Enterprise required |
| [Esri/raster-deep-learning](https://github.com/Esri/raster-deep-learning) | Python raster function inference | Remote Sensing | Apache-2.0 code; Image Analyst, models and data rights separate |

| [Esri/projection-engine-db-doc](https://github.com/Esri/projection-engine-db-doc) | WKIDs/latestWKIDs, geographic/projected/vertical CRS, datums, extents, transformations | `coordinate-reference-systems` | Apache-2.0 data/docs; not Esri PE engine |
| [OSGeo/spatialreference.org](https://github.com/OSGeo/spatialreference.org) | Authority definitions, interoperable WKT and PROJJSON export | `coordinate-reference-systems` | MIT website source; PROJ-derived registry is versioned |
| [Esri/arcgis-pro-sdk-community-samples](https://github.com/Esri/arcgis-pro-sdk-community-samples) | ArcGIS Pro ribbon, dockpanes, editing, map events and async operations | `gis-development` / ArcGIS Pro | Requires target ArcGIS Pro SDK/application; examples are not universal plugins |
| [Esri/arcgis-python-api](https://github.com/Esri/arcgis-python-api) | Portal, feature services, geoprocessing notebooks | `gis-development` | NOT the same package as ArcPy; auth/service/license requirements |
| [Esri/raster-functions](https://github.com/Esri/raster-functions) | Raster function chains, imagery processing, reusable analytical designs | `gis-development` / remote sensing | Check installed ArcGIS/raster runtime and example license |
| [Esri/geometry-api-java](https://github.com/Esri/geometry-api-java) | Geometry union/intersection/buffer/topology in Java | `gis-development` / geometry | Java library; measure geometry units and geodesic/planar distinction |
| [Esri/lerc](https://github.com/Esri/lerc) | Efficient raster compression, tiles, elevation/imagery decoding | `gis-development` / raster | Codec only; does not provide a DEM or raster georeferencing |
| [Esri/arcgis-maps-sdk-dotnet-samples](https://github.com/Esri/arcgis-maps-sdk-dotnet-samples) | WPF/WinUI/.NET UI patterns and map interaction | `gis-development` / desktop | ArcGIS Maps SDK runtime and terms must be separately reviewed |
| [Esri/arcgis-maps-sdk-dotnet-toolkit](https://github.com/Esri/arcgis-maps-sdk-dotnet-toolkit) | Viewpoint, scale-bar, legend, compass and toolkit patterns | `gis-development` / desktop | Toolkit source license ≠ unrestricted ArcGIS service use |
| [Esri/esri-leaflet](https://github.com/Esri/esri-leaflet) | ArcGIS feature/service layers with Leaflet | `geospatial-web-mapping` | Respect Leaflet and backend service licensing |
| [Esri/maplibre-arcgis](https://github.com/Esri/maplibre-arcgis) | ArcGIS feature/map/vector tile services with MapLibre | `geospatial-web-mapping` | MapLibre renderer ≠ entitlement to Esri basemaps |
| [Esri/arcgis-rest-js](https://github.com/Esri/arcgis-rest-js) | Typed REST/service interaction and auth patterns | `geospatial-web-mapping` | Service terms, tokens, quotas remain separate |
| [Esri/cim-spec](https://github.com/Esri/cim-spec) | Cartographic Information Model, renderer/layout metadata | `gis-development` / cartography | Specification is not a full cartographic renderer |
| [Esri/i3s-spec](https://github.com/Esri/i3s-spec) | Integrated Mesh, 3D scene layers and scene cache architecture | `gis-development` / 3D | I3S file format ≠ server or scene rendering engine |
| [Esri/awesome-arcgis-developers](https://github.com/Esri/awesome-arcgis-developers) | Discovery catalog for further examples/API references | `gis-development` | Catalog links only; inspect each underlying source |
| [OSGeo/PROJ](https://github.com/OSGeo/PROJ) | Actual horizontal/vertical transformation operations | `coordinate-reference-systems` | Grid availability/licensing and runtime deployment |
| [OSGeo/PROJ-CRS-Explorer](https://github.com/OSGeo/PROJ-CRS-Explorer) | Interactive CRS filters and map/extent discovery patterns | `coordinate-reference-systems` | Verify repository maintenance, actual source and license before reuse |

## Functional reuse matrix

| Goal | Source patterns | What must be tested |
|---|---|---|
| ArcGIS Pro add-in/ribbon | community-samples | Every button wired, ArcGIS Pro version/API, threaded editing, deployment |
| Python web GIS operations | arcgis-python-api / arcgis-rest-js | Token handling, paging, errors, edit permissions, feature coordinates |
| Raster/elevation | raster-functions / lerc | NoData, grids, georeferencing, datatype, compression error thresholds |
| Geometry tools | geometry-api-java | Topological edge cases, spatial reference of inputs, algorithm dimensionality |
| Standalone 2D/3D GIS | MapLibre, esri-leaflet, dotnet-toolkit patterns, I3S | Tiles/services/license; missing terrain and offline fallback; 2D/3D integration |
| CRS dialog and geodesy | PE database, SpatialReference.org, PROJ | Official folder labels, WKID aliases, area, axis, datum shifts/grids, round-trip |
| Map symbols and layouts | cim-spec | Exact versioned schema, parser round-trip, supported symbol types |

## Adoption rules for AI GIS agents

- Before repairing ArcPy installations, compare the intended Pro Python release with local package versions using [the offline compatibility checker](esri-python-environment-compatibility.md); do not silently modify the managed Python environment.
- ArcGIS Pro SDK C# snippets only target Pro-hosted add-ins; do not treat them as Python toolboxes or independent WPF/Cesium application code. Check Pro/.NET version, edit scopes and QueuedTask.
- Select Arcade profiles explicitly; apply [profile-specific validation](arcgis-arcade-profiles.md) for labeling, popup, attribute rules and visualization.
- Network analysis, CAD/BIM, elevation and raster inference examples require relevant licenses, services, data and sometimes model access. Offer an independent free alternative when applicable.
- Geoportal metadata can be untrusted: verify the original host, access, actual STAC assets and CRS; discovery does not grant redistribution.
- Original root source licenses of the ten new official Esri repositories were inspected as Apache-2.0 on 2026-10-25. This does not include ArcGIS product or service licenses.

## Deliberate non-claims

- No Esri proprietary binaries, ArcGIS SDK runtimes, commercial imagery, elevation/tiles or access tokens are copied into this repository.
- GitHub project availability and a sample compile do not prove host-level runtime acceptance.
- `Favorites` and `Layers` are ArcGIS Pro user/document state, not entries in the PE factory object CSV files.
- `ESRI:WKID` and `EPSG:WKID` must not be silently equated; maintain exact authority and versions.
- Adding a project to `references/project-catalog.json` makes it searchable on the project site; it does not make a third-party code dependency bundled or validated.

## Update procedure for the project

1. Verify repository, last release, README, license, security/archival status and runtime dependencies.
2. Add/update metadata in `references/project-catalog.json`, avoiding duplicate canonical repository URLs.
3. Route it to relevant skill + reference, keeping code rights separate from public documentation.
4. Implement smallest non-destructive example with real automated tests, then perform host integration tests.
5. Update both English and Arabic docs when introducing new public-facing features.
