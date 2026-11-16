# Network topology audit — read-only endpoint screening

```bash
python examples/network-topology/audit_network.py --input examples/network-topology/fixtures/network.geojson --output network-report.json
```

**Expected:** 3 unique lines, 1 reversed duplicate (`reversed-duplicate`) and 4 dangles. Output is a separate JSON report; input is untouched. Fixture coordinates are **synthetic WGS84**, not real Egyptian utility networks.

**Scope:** this checks end-node incidence rounded to longitude/latitude decimal places. It does not node crossings, discover near-miss vertices, prove valid connectivity, validate 3D pipe networks or distinguish intentional terminals. `--decimals` is not a metric snap distance. For actual QA, run GIS-native topological rules in a suitable projected CRS, review endpoint exceptions and obtain consent before repair.
