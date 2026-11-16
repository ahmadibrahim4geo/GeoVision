# Free & public GIS APIs and web services — provider selection

**Reviewed 2026-10-25.** Machine-readable, filterable entries: [`free-geospatial-apis.json`](free-geospatial-apis.json). "Free" is not synonymous with commercial use, unlimited requests, full-resolution scientific data, offline caching, anonymous download, or permission to distribute imagery.

**Free-first policy:** see [cost tiers and the no-cost default](free-first-policy.md). Records have a structured `cost_tier`; **43** are conditionally free, **1** is self-hosted open-source, **3** are restricted-public, **1** is trial-only, **4** are commercial/metered and **1** is unavailable. These are not live entitlement guarantees. Use `python scripts/search_api_catalog.py --free-only` to exclude trial/paid/restricted/unavailable. Never confuse no-key access with unlimited use.

## Quick reference by domain

| Domain | Primary providers | Access caveat |
|---|---|---|
| Forecast and historical climate | [Open-Meteo](https://open-meteo.com/en/docs), [NASA POWER](https://power.larc.nasa.gov/docs/services/api/temporal/), [Copernicus CDS](https://cds.climate.copernicus.eu/how-to-api) | Open-Meteo free service is non-commercial with explicit quotas; NASA POWER enforces fair use; CDS needs account/token and dataset licence |
| Land + ocean bathymetry | [GEBCO_2026 WMS](https://www.gebco.net/data-products/gebco-web-services/web-map-service), [CEDA OPeNDAP](https://www.gebco.net/data-products/gridded-bathymetry-data) | Free global 15 arc-second terrain; WMS map imagery vs numeric data, TID source classification, no marine navigation |
| Point elevation & DEM | [Open-Meteo Elevation](https://open-meteo.com/en/docs/elevation-api), [OpenTopography](https://opentopography.org/developers), [Open-Elevation](https://www.open-elevation.com/), [USGS EPQS](https://www.usgs.gov/the-national-map-data-delivery/gis-data-download) | Open-Meteo elevation GLO-90 ~90m; OpenTopography requires personal key; Open-Elevation tiny free quotas; USGS primarily US only |
| Satellite imagery catalog | [Earth Search](https://element84.com/earth-search), [Planetary Computer](https://planetarycomputer.microsoft.com/docs/), [Copernicus Data Space](https://dataspace.copernicus.eu/analyse/apis), [OpenGeoHub STAC](https://stac.opengeohub.org/v1/) | STAC metadata access differs from asset download/auth, COG CORS and redistribution terms. OpenGeoHub search uses POST /v1/cat/{catalog_id}/search |
| Satellite image tiles | [NASA GIBS](https://www.earthdata.nasa.gov/engage/open-data-services-software/earthdata-developer-portal/gibs-api) | WMS/WMTS visualize imagery; scientific raster analysis requires real source data |
| Basemaps | [OpenFreeMap](https://openfreemap.org/quick_start/), [OSMF tiles](https://operations.osmfoundation.org/policies/tiles/) | OpenFreeMap hosted maps no key; OSMF tiles are NOT an unlimited production/offline tile service |
| Geocoding & Gazetteers | [15 providers ranked by entitlement](geocoding-place-search.md), including Pelias, Photon, GeoNames, Esri, Google, Mapbox, HERE, TomTom | Distinguish public restrictions, daily credits, test-only trials, commercial storage and autocomplete terms |\n| OSM POIs & geocoding | [Overpass](https://wiki.openstreetmap.org/wiki/Overpass_API), [Nominatim](https://operations.osmfoundation.org/policies/nominatim/) | Volunteer services; restricted usage. Never auto-generate generic Nominatim integration into apps |
| Soil | [ISRIC SoilGrids](https://isric.org/explore/soilgrids) | REST service **temporarily paused** in provider notice; prefer data download/WebDAV or WCS |
| Biodiversity | [GBIF API](https://techdocs.gbif.org/en/openapi/), [iNaturalist](https://www.inaturalist.org/pages/developers) | Different observation licences; avoid API bulk downloading |
| Boundaries, population and names | [geoBoundaries](https://www.geoboundaries.org/api.html), [WorldPop](https://api.worldpop.org/v2/), [GeoNames](https://www.geonames.org/export/) | Check boundary year, estimated populations and username quota |
| Hazards and humanitarian | [NASA FIRMS](https://firms.modaps.eosdis.nasa.gov/api/area), [EONET](https://eonet.gsfc.nasa.gov/docs/v3), [USGS Earthquake](https://earthquake.usgs.gov/fdsnws/event/1/), [HDX HAPI](https://hdx-hapi.readthedocs.io/en/latest/) | Event semantics, quality and coverage differ |
| Routing/OSM history | [HeiGIT](https://api.heigit.org/), [ohsome v2](https://docs.ohsome.org/ohsome-api/v2/) | Keys, OSM attribution and endpoint migrations |
| Water, ocean and air | [Open-Meteo Flood](https://open-meteo.com/en/docs/flood-api), [OpenAQ](https://docs.openaq.org/api), [NOAA NCEI](https://www.ncei.noaa.gov/erddap/rest.html) | Validate model-vs-observation, area of coverage and datums |

## Geocoding & Place Search — 2026-10-25\n\n[Compare 15 geocoding and geographic place-search providers](geocoding-place-search.md), ranked self-hosted/open-source → public/restricted → free quota → free trial → commercial. The [API JSON catalog](free-geospatial-apis.json) now has **53 records** and each `category=geocoding` entry has `pricing_group`. OpenCage trial calls may **not** fund ongoing production; public Nominatim prohibits autocomplete. Documentation reviewed, provider availability NOT live-tested in this change.\n\n## GEBCO_2026: free global bathymetry + land elevation

**Latest official GEBCO release: GEBCO_2026, published April 2026.** Its 15 arc-second regular WGS84 grid covers sea-floor and land elevation, approximately 463 m between north/south samples at the equator; longitude spacing in metres varies with latitude. Cells are pixel-centre registered. Heights are in metres: **negative seafloor depth, positive land elevation**. The grid interval is *not* the measured accuracy of seafloor depth.

- **Visual map API:** no-key [GEBCO WMS 1.3.0](https://www.gebco.net/data-products/gebco-web-services/web-map-service), base `https://wms.gebco.net/mapserv`, with GetCapabilities and GetMap. Includes hillshade, flat maps, sub-ice variants and the TID map. **Rendered WMS imagery is not numeric elevation/DEM input.**
- **Numerical data protocol:** [GEBCO at CEDA with OPeNDAP](https://www.gebco.net/data-products/gridded-bathymetry-data). Official GEBCO_2026 NetCDF archive: `https://dap.ceda.ac.uk/bodc/gebco/global/gebco_2026/ice_surface_elevation/netcdf/GEBCO_2026.nc`. Get the dataset-specific OPeNDAP subsetting URL from CEDA; validate current access, constraint expressions and client compatibility rather than fabricating API parameter names.
- **GeoTIFF, NetCDF and Esri ASCII:** use the [official GEBCO download interface](https://download.gebco.net/). It is a web download application, **not an independently advertised public REST API**. Use user-selected spatial subset rather than downloading multi-GB global coverage unnecessarily.
- **Data-quality companion:** the **Type Identifier (TID) Grid** indicates each underlying cell's source type, including direct observations versus interpolated or predicted bathymetry. Keep TID alongside any science-facing output and in provenance.
- **Polar modes:** differentiate surface-of-ice GEBCO from separately released **under-ice bathymetry/topography**.
- **Datum:** GEBCO aims to reference mean sea level, but includes some nearshore inputs with differing vertical datums. Never present it as surveyed vertical control.
- **Licensing:** public-domain grid, free including commercial reuse, provided source is acknowledged and no GEBCO/IHO/IOC endorsement is implied. Cite **GEBCO Bathymetric Compilation Group 2026 (2026), GEBCO_2026 Grid, doi:10.5285/4f68d5c7-45eb-f999-e063-7086abc036fa**. **Never use GEBCO for navigation or safety at sea**.

Documented example (not a claim of live protocol testing):

```text
https://wms.gebco.net/mapserv?request=getcapabilities&service=wms&version=1.3.0
https://data.ceda.ac.uk/bodc/gebco/global/gebco_2026/ice_surface_elevation/netcdf
```

## Recommended API selection procedure for AI agents

1. Declare use case, output variables and units, spatial/temporal coverage, data resolution and required reproducibility.
2. Read the catalog entry's `access`, `authentication`, `limits`, `attribution`, and `caveat` fields; read provider live docs before integration.
3. Select **the dataset as well as the endpoint**: model reanalysis != ground station; imagery rendering != analysis-ready pixels; elevation raster != precise surveyed point.
4. Determine no-key/keyed, non-commercial/commercial, rate limits and cost; require explicit owner credentials. Store tokens outside Git and UI logs. Never hardcode a personal OpenTopography key in redistributable software.
5. Build adapters with per-provider version, retry only safe temporary errors (429/5xx with exponential backoff), cache where expressly permitted, request timeout, error telemetry, fallback, clear source attribution and license metadata.
6. Verify response shape, CRS, axis order, vertical datum, pixel scale, timezone, missing values, geographic and temporal coverage. Keep downloaded values labelled by exact dataset/source.
7. **Do not** bulk query public Nominatim, OSMF tiles, Overpass or soil services; use legitimate extracts, self-hosting or contracted providers when workloads are large.
8. Mark endpoints temporarily unavailable or unverified when evidence is insufficient. Never claim that a docs example has been live-tested unless it was.

## Worked examples (examples are documented, NOT live service tests)

Open-Meteo noncommercial forecast for Cairo (JSON):

```text
https://api.open-meteo.com/v1/forecast?latitude=30.0444&longitude=31.2357&hourly=temperature_2m,relative_humidity_2m
```

NASA POWER daily point meteorology (JSON):

```text
https://power.larc.nasa.gov/api/temporal/daily/point?parameters=T2M&community=AG&longitude=31.2357&latitude=30.0444&start=20230101&end=20230103&format=JSON
```

OpenFreeMap MapLibre style (map display, **not** a GeoTIFF asset):

```text
https://tiles.openfreemap.org/styles/liberty
```

### Critical current provider warnings

- Open-Meteo [free terms](https://open-meteo.com/en/terms): <10,000 calls/day, <5,000/hour and <600/min; free noncommercial only; its data are CC BY 4.0, server code AGPLv3.
- OpenTopography [API developer terms](https://opentopography.org/developers): personal free keys, academic/nonacademic daily quotas; do **not** embed someone else's key in apps or proxy other users through it.
- OpenFreeMap [offline rules](https://openfreemap.org/quick_start/): offline download caps differ from public map views. OSM contributors' attribution applies.
- ISRIC [SoilGrids](https://isric.org/explore/soilgrids) currently reports its REST API temporarily paused, no restoration ETA.
- Nominatim [acceptable-use policy](https://operations.osmfoundation.org/policies/nominatim/) forbids autocomplete/systematic harvesting and generic embedding in code-generating tools; entries here are **documentation only**, not permission to programmatically generate an integration.


## Expanded free/public API reference (2026-10-24): 19 additions, 37 entries total

This section describes the earlier 19-service expansion (37 at that milestone); the current catalog now has **39 service entries including two GEBCO_2026 access routes**. Search the [machine-readable JSON catalog](free-geospatial-apis.json) rather than hardcoding URLs or access assumptions. All endpoints below are provider-documented; no end-to-end live data ingestion test is implied by this catalog.

| Domain | Added APIs | Required GIS interpretation |
|---|---|---|
| Administrative boundaries and names | [geoBoundaries](https://www.geoboundaries.org/api.html), [GeoNames](https://www.geonames.org/export/) | Preserve boundary edition/year and original layer licence; GeoNames free username with daily credits |
| Population and humanitarian | [WorldPop v2](https://api.worldpop.org/v2/), [World Bank v2](https://datahelpdesk.worldbank.org/knowledgebase/articles/889392), [HDX HAPI](https://hdx-hapi.readthedocs.io/en/latest/getting-started/) | WorldPop modeled population grids vs World Bank country indicators vs humanitarian publisher-specific statistics |
| Hazards | [NASA FIRMS](https://firms.modaps.eosdis.nasa.gov/api/area), [USGS Earthquake](https://earthquake.usgs.gov/fdsnws/event/1/), [NASA EONET v3](https://eonet.gsfc.nasa.gov/docs/v3) | Hotspot detections are not burned perimeters; EONET curated events are not an exhaustive official disaster registry |
| Satellite discovery | [NASA Earthdata CMR](https://cmr.earthdata.nasa.gov/search/site/docs/search/api.html) | Collection/granule metadata access is separate from signed scientific asset download |
| Measured and modeled air quality | [OpenAQ v3](https://docs.openaq.org/api), [Open-Meteo Air Quality](https://open-meteo.com/en/docs/air-quality-api) | Real station readings are not interchangeable with CAMS model outputs |
| Floods and water stations | [Open-Meteo GloFAS](https://open-meteo.com/en/docs/flood-api), [USGS Water Data](https://api.waterdata.usgs.gov/docs/ogcapi/migration/) | Discharge is m³/s, not a flood polygon; USGS gauge services are primarily US-only |
| Routing and historical OSM | [HeiGIT openrouteservice](https://api.heigit.org/), [ohsome v2](https://docs.ohsome.org/ohsome-api/v2/) | Both need free keys; an OSM edit timestamp does not show actual building construction date |
| Climate/coasts/ocean | [NOAA CDO v2](https://www.ncei.noaa.gov/cdo-web/webservices/v2), [NOAA CO-OPS](https://api.tidesandcurrents.noaa.gov/api/dev), [NOAA NCEI ERDDAP](https://www.ncei.noaa.gov/erddap/rest.html) | Different coverage, tidal/vertical datums, observation quality and dataset-level licences |
| Biodiversity | [iNaturalist API](https://www.inaturalist.org/pages/developers) | Citizen observations may obscure protected taxa coordinates; media carry separate licences |

### Operational notices and migrations

- **WorldPop:** public calls without a key are limited to 1,000/day and 50,000 km² per polygon. An all-Egypt large-polygon workflow requires approved higher access, licensed local raster processing or partitioning under policy.
- **NASA FIRMS:** request a personal free MAP_KEY; 5,000 map transactions per 10 minutes, with larger requests consuming multiple transactions. Never publish a MAP_KEY or include it in a shared build.
- **OpenAQ v3:** free key, up to 60/minute and 2,000/hour. Verify sensor identity, timezone, measurement units and averaging windows.
- **NOAA CDO v2:** free personal access token, up to 5 requests/second and 10,000/day.
- **GeoNames:** free account username, 10,000 daily and 1,000 hourly service credits. GeoNames permits commercial use with its CC BY attribution obligations.
- **HeiGIT:** new openrouteservice host is `api.heigit.org`, not old `api.openrouteservice.org`. ohsome uses stable `/ohsome-api/v2/` (not `v2-rc`) and v1 is scheduled for shutdown 2026-11-30.
- **USGS Water:** migrate to modern `api.waterdata.usgs.gov/ogcapi/v1/` collections. The historical `waterservices.usgs.gov` services are expected to retire in early 2027.
- **iNaturalist:** keep requests near one per second and below 10,000/day; use legitimate bulk export paths for large data rather than scraping observation pages.

### Minimal reproducible samples (documentation, not live integration tests)

```text
https://www.geoboundaries.org/api/current/gbOpen/EGY/ADM1/
https://earthquake.usgs.gov/fdsnws/event/1/query?format=geojson&minmagnitude=5&limit=5
https://eonet.gsfc.nasa.gov/api/v3/events/geojson?limit=5
https://api.worldbank.org/v2/country/EG/indicator/SP.POP.TOTL?format=json
```

For implementation, follow [geospatial-api-integration](../skills/geospatial-api-integration/SKILL.md) and [geospatial-data-discovery](../skills/geospatial-data-discovery/SKILL.md): record provider, product/year, source, retrieval time, CRS, vertical/tidal datum, units, resolution, observation/model distinction, NoData, licence and attribution. Handle 401/403/404/429/timeouts, paging and partial results explicitly. Avoid automatically switching providers without disclosing scientific differences.

## Search and validation

```bash
python scripts/validate_api_catalog.py
python scripts/search_api_catalog.py --category terrain-elevation
python scripts/search_api_catalog.py --access free-noncommercial
python scripts/search_api_catalog.py --query sentinel
```

The catalog is an independently maintained curated reference, not an API aggregator, credential provider, service uptime guarantee, or proxy. All third-party services retain their own terms.