# GIS Security Standard

**Companion execution skill:** [GIS Security Guardian](../skills/gis-security-guardian/SKILL.md), [Agent trust boundaries](security/agent-safety.md), [GIS file threat scenarios](security/gis-input-threats.md), [free-first scanning tools](security/tooling-and-ci.md). Security rules are not equivalent to OS process/network isolation.

Applies across all agent skills, APIs, GIS uploads and databases. Treat GeoJSON, Shapefile ZIP, KMZ/KML, GPKG, SVG, GeoTIFF, STAC metadata and remote URLs as untrusted.

## Files and archives
- Normalize destination paths and reject absolute paths, '..', traversal, symlinks, and unexpected archive extensions. Apply member-count, expanded-size and compression-ratio caps to prevent ZIP bombs.
- Validate Shapefile sidecars (.shp, .shx, .dbf, .prj, .cpg) and field encodings; don't trust file headers and record sizes.
- Bound raster dimensions, bands, memory, file sizes and processing time. Prefer windowed GDAL/Rasterio reads to unrestricted in-memory loading.
- Default to new outputs. Explicit consent + backup and rollback are prerequisites for modifying source layers.

## Network and credentials
- Only call verified, authorized HTTPS provider endpoints using an audited host allowlist. Reject loopback, link-local, private, cloud metadata or redirect targets; limit request volume and timeouts.
- Do not fetch arbitrary URLs embedded in GIS files, external styles, STAC assets or WMS/WFS metadata without applying the destination policy.
- Never commit API keys, signed URLs, Authorization headers or connection strings. Use env-based secrets and redact logs. Review free-tier limits and provider terms separately from availability.
- A successful HTTP response does not prove a service is legally reusable, scientifically trustworthy or alive for every geographic request.

## Spatial SQL and plugins
- Parameterize SQL values, allowlist SQL identifiers and use read-only database roles for audits. Approved migrations require transactions, review, backups and rollback plans.
- Review version, licensing and source of any downloaded plugin or processing script before execution; do not evaluate executable content from untrusted maps.

For each deliverable document file/network threat boundaries, max sizes, permission requirements, failure behavior and validation in [the acceptance report](../templates/GIS_ACCEPTANCE_REPORT.md). Review [before/after examples](gis-antipatterns.md).
