#!/usr/bin/env python3
"""Requires installed PyQGIS; no network, source mutation or output overwrite."""
import argparse
import json
from pathlib import Path
import sys
import tempfile


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, help="New GeoPackage path; must not exist")
    parser.add_argument("--prefix", help="Installed QGIS prefix path, if needed")
    args = parser.parse_args(argv)
    if args.output is not None and args.output.exists():
        parser.error("Refusing to overwrite an existing output")
    try:
        from qgis.core import (QgsApplication, QgsFeature, QgsGeometry, QgsPointXY,
                               QgsVectorFileWriter, QgsVectorLayer, QgsProject)
    except ImportError:
        print("PyQGIS runtime missing; use the Python environment supplied with QGIS", file=sys.stderr)
        return 2
    if args.prefix:
        QgsApplication.setPrefixPath(args.prefix, True)
    app = QgsApplication([], False)
    app.initQgis()
    try:
        with tempfile.TemporaryDirectory(prefix="qgis_engine_") as temp:
            path = args.output or Path(temp) / "roundtrip.gpkg"
            if path.exists() or not path.parent.is_dir():
                raise RuntimeError("Output must be new and parent directory must exist")
            layer = QgsVectorLayer("Point?crs=EPSG:4326&field=place:string", "source", "memory")
            if not layer.isValid():
                raise RuntimeError("Memory provider unavailable")
            for name, lon, lat in (("Cairo", 31.2357, 30.0444),
                                   ("Alexandria", 29.9187, 31.2001)):
                feat = QgsFeature(layer.fields())
                feat.setAttributes([name])
                feat.setGeometry(QgsGeometry.fromPointXY(QgsPointXY(lon, lat)))
                if not layer.dataProvider().addFeatures([feat])[0]:
                    raise RuntimeError("Failed to add point")
            options = QgsVectorFileWriter.SaveVectorOptions()
            options.driverName = "GPKG"
            code, *_ = QgsVectorFileWriter.writeAsVectorFormatV3(
                layer, str(path), QgsProject.instance().transformContext(), options)
            if code != QgsVectorFileWriter.NoError:
                raise RuntimeError("GeoPackage write failed: {}".format(code))
            check = QgsVectorLayer(str(path), "readback", "ogr")
            if not check.isValid() or check.featureCount() != 2 or check.crs().authid() != "EPSG:4326":
                raise RuntimeError("GeoPackage readback validation failed")
            coords = {f["place"]: (f.geometry().asPoint().x(), f.geometry().asPoint().y())
                      for f in check.getFeatures()}
            expected = {"Cairo": (31.2357, 30.0444), "Alexandria": (29.9187, 31.2001)}
            if coords.keys() != expected.keys() or any(
                    abs(coords[name][i] - xy[i]) > 1e-6
                    for name, xy in expected.items() for i in (0, 1)):
                raise RuntimeError("Coordinate / attributes changed in round trip")
            print(json.dumps({"ok": True, "features": 2, "crs": "EPSG:4326",
                              "output": str(path)}, ensure_ascii=False))
            check = None
            layer = None
    finally:
        app.exitQgis()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
