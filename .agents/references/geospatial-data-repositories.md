# Geospatial data repositories and catalogs — curated discovery guide

**Review: 2026-10-25.** Machine-readable [source directory](geospatial-data-repositories.json) is independent of the [software catalog](project-catalog.json) and the [real services/API inventory](free-geospatial-apis.json). Entries are **references**, not mirrored datasets or proof of live APIs.

## Architecture: use one source of truth for each purpose

| Purpose | Read | Action |
| --- | --- | --- |
| Broad GIS directories and country/theme search | [GIS Data Repositories Professional 2026](https://github.com/ahmadibrahim4geo/gis-data-repositories-professional-2026) | Use its original bilingual explorer; do not clone 2,660+ entries into this skills pack |
| OpenGeoS curated discovery | [OpenGeoS geospatial-data-catalogs](https://github.com/opengeos/geospatial-data-catalogs) | Identify a promising upstream provider and verify it |
| STAC source discovery | [STAC Index](https://github.com/opengeos/stac-index-catalogs), [AWS STAC list](https://github.com/opengeos/aws-open-data-stac) | Inspect collection `links`, `extent`, `license`, pagination and item assets |
| Satellite metadata | [NASA CMR](https://github.com/opengeos/NASA-CMR-STAC), [Planetary Computer](https://github.com/opengeos/Planetary-Computer-Catalog), [GEE](https://github.com/opengeos/Earth-Engine-Catalog) | Authenticate as applicable and confirm specific asset availability |
| Official authoritative access | [NASA Earthdata](https://search.earthdata.nasa.gov/), [Copernicus](https://dataspace.copernicus.eu/), [GEBCO](https://www.gebco.net/data-products/gridded-bathymetry-data) | Read current licenses, dataset versions and official provider documentation |
| Executable HTTP integration | [GIS API catalog](free-geospatial-apis.json) | Use only a verified endpoint, documented quotas and retries |

## Correctness and duplicate policy

1. Distinguish `directory` (links), `catalog-index` (metadata), `portal` (source-site search), `data-reference` (research dataset), `STAC API` (machine query), `downloadable asset` (actual bytes), `map tiles` (visualization).
2. Start from the user's area of interest (Egypt / region / global), scientific variable, period, scale, intended output and commercial/noncommercial use. The list itself is global, not evidence of Egyptian coverage.
3. Verify original publisher, dataset collection, DOI/item ID, licence, attribution, current endpoint, access method, resolution, coverage, data date and version. Never infer 'free' from an MIT licence on a *catalog script*.
4. For STAC, retrieve `/collections` metadata and a bounded `/search` sample, then verify requested assets. COG thumbnails, preview WMS, quicklooks, and scientific downloadable rasters are not interchangeable.
5. Review CRS/datum (including vertical datum for DEM), nodata, band scales, units, temporal alignment, raster window reads, geometry validity and privacy restrictions.
6. De-duplicate canonical repository URL and provider/collection IDs across the guide and software catalog; reference, don't duplicate, the large Ahmad Ibrahim master index.
7. Keep explicit `status: unknown`, `last_checked: null` until a separate network/provider test is recorded. Missing catalog entries are not evidence of unavailable datasets.

## Recommended discovery adapter flow

```text
User data goal -> source directory -> candidate original provider
              -> license/access check -> STAC or provider metadata
              -> sample item & asset validation -> reproducible GIS output
              -> provenance + test tier (documentation != production)
```

An [offline deterministic directory search example](../examples/data-catalogs/README.md) demonstrates filtering without scraping providers. For live STAC access, see [Remote Sensing Pipelines](../skills/remote-sensing-pipelines/SKILL.md).

**Attribution:** OpenGeoS and Dr. Qiusheng Wu upstream projects are independently linked and credited; this project is not endorsed by them.

## OpenGeoHub / OpenLandMap

See the [OpenGeoHub dataset, licensing, STAC/COG and Earth-view guide](opengeohub-openlandmap-integration.md). Confirm actual epochs and layer access independently; static STAC is not automatically a map tile API.
