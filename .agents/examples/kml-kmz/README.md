# KML/KMZ style-fidelity contract fixture

This dependency-free artificial fixture exercises the **document structure**, not an actual Earth globe renderer. It contains a nested folder, several geometries, normal/highlight `StyleMap`, `aabbggrr` colors, polygon interior ring, TimeStamp, a gx:Track, ExtendedData and an explicit camera. It does not bundle proprietary icons or follow remote URLs.

- Input: [styled-roundtrip.kml](styled-roundtrip.kml)
- Test: `python -m unittest tests.test_kml_preservation -v`
- Guidance: [Google KML/KMZ interoperability](../../references/google-kml-kmz-compatibility.md).

Test the fixture manually in Google Earth, ZEarth and other host GIS systems separately. For host acceptance, compare rendered colors/opacity/labels and opening/re-saving KML/KMZ, record screenshots and unsupported-element warnings. The fixture intentionally has no external or KMZ asset dependencies; archive-based round trips need additional user-supplied fixture archives and threat-model tests.

**Non-destructive:** never overwrite the original fixture. Use a copy for edits. **Evidence:** offline XML assertions (T1/T2 contract checks) are not a native 3D viewer execution (T3/T4).
