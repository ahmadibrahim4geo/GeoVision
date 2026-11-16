-- Demo SQL for an authorized PostGIS SANDBOX with existing
-- pipes(asset_id, geom) table. Do not run on production without approval.
-- Inspect versions, data size, constraints, SRID and geometry types first.

-- Read-only baseline:
SELECT ST_SRID(geom) AS srid, GeometryType(geom) AS kind,
       COUNT(*) AS features, COUNT(*) FILTER (WHERE NOT ST_IsValid(geom)) AS invalid
FROM pipes GROUP BY 1, 2 ORDER BY 1, 2;

-- Sandbox DDL only: plan index building/locking and rollback beforehand.
CREATE INDEX IF NOT EXISTS pipes_geom_gist_idx ON pipes USING GIST(geom);
ANALYZE pipes;

-- Example: bbox query in the actual geometry SRID, assuming uniform SRID.
-- Replace the synthetic WGS84 envelope and inspect actual index usage.
EXPLAIN (ANALYZE, BUFFERS)
SELECT asset_id
FROM pipes
WHERE geom && ST_Transform(
    ST_MakeEnvelope(30.0, 29.5, 31.0, 30.5, 4326), ST_SRID(geom)
);

-- Preserve EXPLAIN evidence, source properties, timings and permission logs.
-- Do not confuse label reassignment ST_SetSRID with actual ST_Transform.
