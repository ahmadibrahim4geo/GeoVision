# Device context: explicit coordinates + timezone-aware time

This offline Python 3.9+ example accepts **manual geographic coordinates**, a named IANA timezone and either a supplied timezone-aware ISO timestamp or the host UTC clock. It reads **no precise device location**, never calls external services and does **not** perform astronomical sun-position calculations; those belong to a separately installed verified library such as Astral/pvlib.

```bash
python examples/device-context/device_context.py --latitude 30.0444 --longitude 31.2357 --timezone Africa/Cairo --at 2026-07-01T12:00:00+03:00
python examples/device-context/device_context.py --latitude 30.0444 --longitude 31.2357 --timezone Africa/Cairo
python -m unittest discover -s tests -v
```

The result identifies the time mode (simulated/live), `manual` coordinate provenance and `not_requested` geolocation permission status. It rejects naive timestamps, invalid coordinates and timestamp offsets inconsistent with the declared zone. If the OS lacks zoneinfo data, install an appropriately licensed `tzdata` fallback in the target environment.

For WPF, Android, web or ArcGIS implementations, build real permission and sensor adapters **separately** and test their live controls before claiming the app can locate the device. See [Device Context skill](../../skills/gis-device-context/SKILL.md).
