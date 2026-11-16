# GIS source directory search — deterministic offline example

This is a local **metadata search**, not a data downloader, live STAC client, nor confirmation that a source is reachable. Python 3.11+ stdlib only.

Example commands:

- `python examples/data-catalogs/search_sources.py --query STAC --json`
- `python examples/data-catalogs/search_sources.py --type catalog-index --limit 15`
- `python examples/data-catalogs/search_sources.py --category bathymetry-dem`
- `python scripts/validate_data_repositories.py`

The [versioned data-source index](../../references/geospatial-data-repositories.json) stores coverage, access and **unverified** metadata. The large [GIS Data Repositories Professional 2026](https://github.com/ahmadibrahim4geo/gis-data-repositories-professional-2026) remains authoritative for its own 2,660+ historical links. Do not duplicate it here.

For actual downloads, verify original provider, credentials, quotas, legal terms, item-level metadata, COG range requests, CRS/datum/nodata and a real downloadable asset.
