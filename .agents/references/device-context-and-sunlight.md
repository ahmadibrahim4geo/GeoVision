# Device context, geolocation permissions, time zones and sunlight

## Choosing location and time safely

**Two independent anchors:** geographical computation target and device position. A scene-based Sunlight button should initially use the globe's chosen coordinates (or selected map target) and its chosen date/time. "Locate me" is a separate explicit permission-gated feature. Location must not be inferred from the timezone, language or IP with GPS-like certainty.

Temporal fields must distinguish UTC instant, target IANA timezone (for display), OS/browser timezone, live vs simulated clock and DST. Scene/camera latitude-longitude use geographic WGS84 with explicit coordinate order; heights and DEM vertical datum are separate.

Use solar azimuth/elevation from tested ephemeris and UTC. Rendering shadows on terrain requires more than sun angles: geospatial terrain geometry, horizon, correct 3D scale and engine-specific lighting. Cesium clock/sunlight UI requires native integration and visual checks; documentation does not establish working controls.

## Reference implementations

| Resource | URL | Intended role | Rights/limits |
| --- | --- | --- | --- |
| Browser Geolocation standard | https://www.w3.org/TR/geolocation/ | Permission-controlled geographic position, accuracy and timestamp | Web standard, needs browser/OS grant; HTTPS policy |
| MDN Geolocation | https://developer.mozilla.org/en-US/docs/Web/API/Geolocation_API | Secure-context implementation and permission behavior | Reference documentation |
| Windows Geolocator guide | https://learn.microsoft.com/en-us/windows/apps/develop/maps-and-location/get-location | Foreground UI RequestAccessAsync and GetGeopositionAsync | OS API; manifest and app configuration vary |
| Python tzdata | https://github.com/python/tzdata | IANA database fallback for zoneinfo | Apache-2.0 package; IANA data terms separate |
| Astral | https://github.com/sffjunkie/astral | Sunrise/sunset and solar azimuth/elevation | Apache-2.0; Python 3 compatibility |
| pvlib-python | https://github.com/pvlib/pvlib-python | Solar position / PV calculations | BSD-3-Clause; dependencies and units |
| IANA timezone database | https://www.iana.org/time-zones | Canonical timezone/DST records | Data license/redistribution separately |
| Python datetime / zoneinfo | https://docs.python.org/3/library/zoneinfo.html | Local-to-UTC and timezone-aware conversions | Standard library (Python 3.9+) |

## Operational failure checklist
- [ ] No permission -> no location, manual/place-on-map substitute
- [ ] GPS estimate is not a guaranteed exact location; accuracy, age, source visible
- [ ] Target is not device location; compute on selected scene even if device elsewhere
- [ ] UTC instant uses explicit offset; reject ambiguous/gap local wall times unless disambiguated
- [ ] System clock can be inaccurate; ability to freeze/replay deterministic test time
- [ ] Live mode and historical simulation cannot silently overwrite each other's controls
- [ ] Solar azimuth convention, altitude, polar day/night and target timezone tested
- [ ] Precise location never logged or exported by default; no remote IP geolocation calls without opt-in
- [ ] ArcMap Python 2.7 does not support zoneinfo; adapter must be distinct
- [ ] Native browser/Windows permission and sun renderer tested in actual target application

An offline example provides timezone normalization with explicit coordinates: [device-context](../examples/device-context/README.md).
