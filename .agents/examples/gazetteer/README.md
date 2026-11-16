# World Gazetteer — offline reference-label example

Python 3 standard-library proof of concept using **locally downloaded** GeoNames TSV data, SQLite and approximate 2D viewport/collision logic. Guided by [the full reference contract](../../references/world-gazetteer-reference-layer.md). No hosted calls, API keys, bundled datasets, Esri licensed locators or native app integration.

1. Download and extract e.g. [GeoNames EG.zip](https://download.geonames.org/export/dump/) into \`EG.txt\` (CC BY attribution).
2. Run:

\`\`\`sh
python examples/gazetteer/viewport_labels.py --db geonames.sqlite import --tsv EG.txt
python examples/gazetteer/viewport_labels.py --db geonames.sqlite search --query Cairo
python examples/gazetteer/viewport_labels.py --db geonames.sqlite labels --bbox 24 21 37 32 --scale 2000000 --limit 80
python -m unittest discover -s tests -p "test_world_gazetteer.py" -v
\`\`\`

Results preserve \`provider_id\`, \`place_id\`, WGS84 and country/feature codes. This sample draws **no map**, handles primary place names only, and uses a simple B-tree indexed bbox with approximate font-box sizing. For a **production world database**, use RTree/tile indexes, streaming imports, source revisions, tested Arabic/English alternateNamesV2 lookup, text shaping, async/cancelable viewport jobs and Cesium 3D frustum/occlusion. Native ZEarth, ArcGIS and QGIS integration remains to be built and tested.
