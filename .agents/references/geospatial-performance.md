# GIS Performance and Scaling

No stack is universally fastest. First prove equivalent spatial results (CRS, geometry, raster alignment, NoData and units), then profile with real workload distributions.

| Case | Candidate | What to measure |
| --- | --- | --- |
| Small ad-hoc vector analyses | GeoPandas/Shapely | Operation time, peak RAM, feature fidelity |
| Repeated spatial joins/filters | Spatial index, PostGIS GiST, STRtree | EXPLAIN plans, selectivity, wall time, index write cost |
| Millions of features or parallel users | PostGIS, DuckDB/GeoParquet, GDAL streaming | Memory, I/O, latency, transaction throughput |
| Rasters larger than RAM | GDAL/Rasterio windowed reads or COG ranges | Memory, I/O bytes, pixel/NoData parity |
| Portable columnar data | GeoParquet if supported by reader | CRS metadata, field consistency, compression |
| Legacy GIS interoperability | Shapefile when required | 10-character names, encoding, size constraints |
| Multi-layout cartography | ArcPy/QGIS batch exports | Per-page time, PDF validity, re-openability |

## Benchmark protocol
1. Fix OS, CPU/RAM, input sizes, geometry complexity, CRS, package versions and fixture hashes.
2. Benchmark warm and cold conditions as appropriate; measure multiple times, report median and variability. Record wall time, CPU/memory, IO and output correctness.
3. Compare apples-to-apples algorithms and results before selecting a faster engine. Avoid misleading comparisons of network latency against local compute.
4. Include resource and overflow limits with failure tests; don't claim a miniature fixture demonstrates million-feature readiness.
5. Use [acceptance reports](../templates/GIS_ACCEPTANCE_REPORT.md) and distinguish T1 synthetic benchmarks from T3 native runs.

[Offline benchmark harness](../scripts/benchmark_gis_examples.py) measures a four-feature synthetic GeoJSON example only; not licensed GIS.
