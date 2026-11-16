# Open-Meteo hourly forecast response → GIS point features

**Offline synthetic test fixture** (Python 3.11+, no dependencies or API keys):
```bash
python examples/api-ingestion/weather_to_geojson.py --output weather-sample.geojson
```
Expect 2 features, with one null observation omitted. Coordinates are WGS84 `[longitude,latitude]`, timestamps are UTC, temperature values are Celsius and source is recorded. The default point is illustrative only, **not a weather station**.

**Optional live mode**, after checking the provider's current [documentation and use conditions](https://open-meteo.com/en/docs):
```bash
python examples/api-ingestion/weather_to_geojson.py --live --latitude 30 --longitude 31 --output weather-live.geojson
```
Open-Meteo forecast values are modeled estimates, not ground observations. Production use needs retry/backoff, rate handling, stable provenance, provider terms and data-availability safeguards. Generic CI verifies the offline transformation only, not provider uptime.
