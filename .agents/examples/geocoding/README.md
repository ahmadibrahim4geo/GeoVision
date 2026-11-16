# Geocoding / place search Python example

A minimal Python 3 standard-library demo for **one explicit user-initiated search** per run. It reads the **live repository catalog** and supports Open-Meteo Geocoding (no key, noncommercial only), GeoNames (individual username), and Geoapify (individual key). Results normalize to WGS84 latitude/longitude and GeoJSON-style `[lon, lat]` coordinates, preserving provider attribution ID.

See [all 15 ranked providers](../../references/geocoding-place-search.md). Other listed APIs are **documented, not implemented as adapters** in this sample. Do not assume a documented provider is tested/available.

```bash
python examples/geocoding/place_search.py --query Cairo --country EG --language ar
# Optional accounts: set GEONAMES_USERNAME or GEOAPIFY_API_KEY
python examples/geocoding/place_search.py --provider geonames --query Aswan
python -m unittest discover -s tests -v
```

**Safety**: Do not use OSMF public Nominatim for autocomplete or generic generated geocoding services. Keys come from the local environment; the sample never prints request URLs or keys, though requests to a provider necessarily transmit those credentials. Respect quotas, attribution, personal data protections, retry limits, and provider storage terms.

The tests use a mock opener and fixture responses; **no network/geocoder/GIS-native tests** are performed. Running this example itself intentionally performs a real API request only when invoked.
