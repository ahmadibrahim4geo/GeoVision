# GeoAI, satellite–map translation and imagery services

Use for georeferenced imagery analysis, model inference, visual segmentation, satellite scene catalogs and derived spatial outputs.

## Software/code to evaluate

- **GeoAI**: https://github.com/opengeos/geoai — applied Python geospatial AI and QGIS integration.
- **SAM 2**: https://github.com/facebookresearch/sam2 — image segmentation. Licensed code/model weights may have separate terms and runtime requirements.
- **ONNX Runtime**: https://github.com/microsoft/onnxruntime — local inference when the ONNX model is compatible.
- **PySTAC Client**: https://github.com/stac-utils/pystac-client — satellite catalog API search, not a raster renderer.
- **TiTiler**: https://github.com/developmentseed/titiler — dynamic tile service for raster assets; operating a server has deployment requirements.
- **Leafmap**: https://github.com/opengeos/leafmap — Python interactive geospatial data analysis and visualization.
- **HIT-SIRS EarthMapper**: https://github.com/HIT-SIRS/EarthMapper — *research paper/reference only* in the inspected repository; no runnable source.

## Real-world GIS acceptance tests

1. Record source, date, sensor, asset URL and permissible usage for every input image.
2. Preserve affine transform, CRS, pixel size, band order, reflectance scale and nodata masks through every preprocessing stage.
3. Split validation data spatially (not solely random pixels) to avoid leakage and inflated model metrics.
4. Map detections to geographic coordinates; validate edges, overlaps, confidence thresholds and geometry validity.
5. Distinguish generated cartographic *images* from authoritative surveyed or classified *GIS data*.
6. For satellite–map synthesis, assess the difference between believable renderings and true topological consistency.
7. Run with/without GPU as required; document memory/model-size limits and provenance.

Do not claim any specific pretrained model/weight is downloadable or commercially reusable without inspecting its own distribution and license.