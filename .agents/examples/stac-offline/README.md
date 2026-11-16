# Free offline STAC Item metadata inspection

This reference uses **synthetic** STAC-like ItemCollection JSON and Python standard library only. It exercises basic metadata filtering by ISO UTC datetime, coarse bounding-box intersection and cloud-cover percentage without contacting any remote API.

```sh
python examples/stac-offline/inspect_items.py \
  --input examples/stac-offline/fixtures/stac_items.json \
  --bbox 30,25,32,27 --after 2026-01-01T00:00:00Z --max-cloud 10
python -m unittest tests.test_stac_offline -v
```

Expected output contains **one synthetic clear item** and its asset *names*, **not a downloaded satellite image**. All fixture URLs use `example.invalid` intentionally.

Scope and caveats:
- Real STAC API paging, OAuth, signed assets, collection conformance, actual availability, cloud masks, geometry intersection and COG analysis **are not implemented**. A bbox overlap is **not** a polygon intersection. Crossing the antimeridian is rejected rather than silently mishandled.
- The tool validates item IDs, bbox order/ranges, timezone-aware acquisition datetimes, cloud percentage and the `assets` object of included items; it never accesses an asset link or asserts its license.
- This offline test is **T1 fixture evidence** only. T2 requires an observed integration with a named live STAC provider/runtime. For real imagery, use a verified provider endpoint, its actual STAC Search API, pagination, item/source lineage, and licensed raster reads after credentials and legal rights are checked.
- No billed model, cloud account, proprietary GIS host, or API key is needed.

See [Remote Sensing Pipelines](../../skills/remote-sensing-pipelines/SKILL.md), [Data Catalogs](../../skills/geospatial-data-catalogs/SKILL.md), and [free-first policy](../../references/free-first-policy.md).
