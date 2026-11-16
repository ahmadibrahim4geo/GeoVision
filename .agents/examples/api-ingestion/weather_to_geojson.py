#!/usr/bin/env python3
"""Convert an offline Open-Meteo-style response to GeoJSON points.

Live Open-Meteo modeled forecast retrieval is explicitly opt-in. No API key
is embedded, and CI does not contact the service.
"""
import argparse
import json
import math
from pathlib import Path
from urllib.parse import urlencode
from urllib.request import Request, urlopen


def convert(payload, lat, lon, source="synthetic-fixture"):
    lat, lon = float(lat), float(lon)
    if not (math.isfinite(lat) and math.isfinite(lon) and -90 <= lat <= 90 and -180 <= lon <= 180):
        raise ValueError("Invalid WGS84 coordinates")
    if payload.get("timezone") not in ("UTC", "GMT"):
        raise ValueError("Only UTC data is accepted")
    hourly = payload.get("hourly", {})
    times = hourly.get("time", [])
    values = hourly.get("temperature_2m", [])
    if payload.get("hourly_units", {}).get("temperature_2m") not in ("°C", "C"):
        raise ValueError("Expected degrees Celsius")
    if not times or len(times) != len(values):
        raise ValueError("Missing or unequal time/temperature arrays")
    features = []
    for stamp, raw in zip(times, values):
        if not isinstance(stamp, str) or len(stamp) != 16 or stamp[10] != "T":
            raise ValueError("Expected YYYY-MM-DDTHH:MM UTC timestamps")
        if raw is None:
            continue
        value = float(raw)
        if not math.isfinite(value):
            raise ValueError("Non-finite temperature")
        features.append({"type": "Feature",
                         "geometry": {"type": "Point", "coordinates": [lon, lat]},
                         "properties": {"timestamp_utc": stamp + "Z",
                                        "temperature_c": value, "source": source}})
    return {"type": "FeatureCollection", "features": features}


def fetch_live(lat, lon):
    params = urlencode({"latitude": lat, "longitude": lon,
                        "hourly": "temperature_2m", "timezone": "UTC", "forecast_days": 1})
    request = Request("https://api.open-meteo.com/v1/forecast?" + params,
                      headers={"User-Agent": "GIS-Development-Skills-example/1.0"})
    with urlopen(request, timeout=20) as response:
        return json.load(response)


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--fixture", type=Path,
                        default=Path(__file__).parent / "fixtures/open_meteo_hourly.json")
    parser.add_argument("--live", action="store_true", help="explicitly enable internet retrieval")
    parser.add_argument("--latitude", type=float, default=30.0)
    parser.add_argument("--longitude", type=float, default=31.0)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args(argv)
    if not args.live and args.fixture.resolve() == args.output.resolve():
        parser.error("Output must not overwrite fixture")
    payload = fetch_live(args.latitude, args.longitude) if args.live else json.loads(
        args.fixture.read_text(encoding="utf-8"))
    data = convert(payload, args.latitude, args.longitude,
                   "open-meteo-modeled-forecast" if args.live else "synthetic-fixture")
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print("Saved {} point observations".format(len(data["features"])))


if __name__ == "__main__":
    main()
