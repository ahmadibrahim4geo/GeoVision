#!/usr/bin/env python3
"""Explicit (never typeahead) place-name search demo for three provider APIs.

No live calls occur on import or during tests. Run only with your own terms/quotas.
Python 3 standard library; does not use OSMF-hosted Nominatim.
"""
import argparse
import json
import os
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import Request, urlopen

ROOT = Path(__file__).resolve().parents[2]
CATALOG = ROOT / "references" / "free-geospatial-apis.json"
SUPPORTED = ("open-meteo-geocoding", "geonames", "geoapify-geocoding")


class GeocodingError(RuntimeError):
    """Recoverable provider/search failure (never substitute fabricated matches)."""


def provider_config(provider, query, country="EG", language="ar", limit=5, environ=None):
    """Return (endpoint, params) with explicit provider-specific semantics."""
    if provider not in SUPPORTED:
        raise ValueError("Select one of: " + ", ".join(SUPPORTED))
    query = query.strip()
    if len(query) < 2:
        raise ValueError("Enter at least two characters")
    if not country.isalpha() or len(country) != 2:
        raise ValueError("Country must be a two-letter ISO code")
    if not 1 <= limit <= 20:
        raise ValueError("limit must be in range 1..20")
    env = os.environ if environ is None else environ
    catalog = json.loads(CATALOG.read_text(encoding="utf-8"))
    source = next(s for s in catalog["services"] if s["id"] == provider)
    if provider == "open-meteo-geocoding":
        params = {"name": query, "count": limit, "language": language, "countryCode": country.upper()}
    elif provider == "geonames":
        username = env.get("GEONAMES_USERNAME", "")
        if not username:
            raise GeocodingError("Set GEONAMES_USERNAME in your environment")
        params = {"q": query, "country": country.upper(), "lang": language,
                  "maxRows": limit, "username": username}
    else:
        key = env.get("GEOAPIFY_API_KEY", "")
        if not key:
            raise GeocodingError("Set GEOAPIFY_API_KEY in your environment")
        params = {"text": query, "filter": "countrycode:" + country.lower(),
                  "limit": limit, "format": "json", "apiKey": key}
    return source["service_url"], params


def normalize(provider, payload):
    """Normalize real response geometry to WGS84; never infer address accuracy."""
    if not isinstance(payload, dict):
        raise GeocodingError("Provider response is not a JSON object")
    if "error" in payload or "status" in payload and provider == "geonames":
        raise GeocodingError("Provider reported an error (review account/quota)")
    if provider == "open-meteo-geocoding":
        rows = payload.get("results", [])
    elif provider == "geonames":
        rows = payload.get("geonames", [])
    elif provider == "geoapify-geocoding":
        rows = payload.get("results", [])
    else:
        raise ValueError("Unsupported provider")
    if not isinstance(rows, list):
        raise GeocodingError("Unexpected result list")
    result = []
    for r in rows:
        if not isinstance(r, dict):
            continue
        try:
            lat = float(r.get("latitude", r.get("lat")))
            lon = float(r.get("longitude", r.get("lng", r.get("lon"))))
        except (TypeError, ValueError):
            continue
        if not (-90 <= lat <= 90 and -180 <= lon <= 180):
            continue
        display = (r.get("formatted") or r.get("toponymName") or r.get("name") or "").strip()
        result.append({
            "name": display, "latitude": lat, "longitude": lon,
            "coordinates": [lon, lat], "crs": "EPSG:4326",
            "provider_id": provider,
            "provider_place_id": str(r.get("id") or r.get("place_id") or ""),
            "country_code": r.get("country_code") or r.get("countryCode") or "",
        })
    return result


def search_places(provider, query, country="EG", language="ar", limit=5,
                  environ=None, opener=urlopen):
    """Make ONE user-triggered GET to the chosen provider with bounded timeout."""
    endpoint, params = provider_config(provider, query, country, language, limit, environ)
    request = Request(endpoint + "?" + urlencode(params),
                      headers={"User-Agent": "GIS-Development-Skills-Example/1.0",
                               "Accept": "application/json"})
    try:
        with opener(request, timeout=12) as reply:
            data = json.load(reply)
    except HTTPError as exc:
        raise GeocodingError("Provider returned HTTP " + str(exc.code)) from exc
    except (URLError, TimeoutError, ValueError) as exc:
        raise GeocodingError("Unable to retrieve/parse provider response") from exc
    return normalize(provider, data)


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--provider", choices=SUPPORTED, default="open-meteo-geocoding")
    parser.add_argument("--query", required=True)
    parser.add_argument("--country", default="EG")
    parser.add_argument("--language", default="ar")
    parser.add_argument("--limit", type=int, default=5)
    args = parser.parse_args(argv)
    try:
        result = search_places(args.provider, args.query, args.country, args.language, args.limit)
    except (GeocodingError, ValueError) as exc:
        parser.exit(1, "Geocoding search failed: " + str(exc) + "\n")
    print(json.dumps(result, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
