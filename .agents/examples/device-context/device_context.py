#!/usr/bin/env python3
"""Deterministic GIS time/coordinate context; never accesses device geolocation."""
import argparse
import json
import math
from datetime import datetime, timezone
from zoneinfo import ZoneInfo


def build_context(latitude, longitude, timezone_name, at=None):
    """Return a privacy-safe context using explicitly provided WGS84 coordinates."""
    if not all(isinstance(v, (float, int)) and math.isfinite(v) for v in (latitude, longitude)):
        raise ValueError("Latitude and longitude must be finite numbers")
    if not -90 <= latitude <= 90 or not -180 <= longitude <= 180:
        raise ValueError("WGS84 coordinates outside valid degree ranges")
    zone = ZoneInfo(timezone_name)
    if at is None:
        instant = datetime.now(timezone.utc)
        mode = "live"
    else:
        provided = datetime.fromisoformat(at.replace("Z", "+00:00"))
        if provided.tzinfo is None or provided.utcoffset() is None:
            raise ValueError("Simulation timestamp must include UTC offset or Z")
        local = provided.astimezone(zone)
        if local.utcoffset() != provided.utcoffset():
            raise ValueError("Provided UTC offset disagrees with named timezone at that instant")
        instant = provided.astimezone(timezone.utc)
        mode = "simulated"
    local = instant.astimezone(zone)
    return {
        "latitude": latitude, "longitude": longitude, "coordinate_reference_system": "EPSG:4326",
        "location_source": "manual", "accuracy_m": None,
        "location_timestamp": None, "permission_status": "not_requested",
        "time_utc": instant.isoformat(timespec="seconds"),
        "display_time": local.isoformat(timespec="seconds"),
        "timezone_id": timezone_name, "timezone_source": "explicit",
        "time_mode": mode, "location_freshness": "not_applicable_manual",
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--latitude", type=float, required=True)
    parser.add_argument("--longitude", type=float, required=True)
    parser.add_argument("--timezone", required=True, help="IANA name such as Africa/Cairo")
    parser.add_argument("--at", default=None, help="ISO 8601 timestamp including offset or Z")
    args = parser.parse_args()
    print(json.dumps(build_context(args.latitude, args.longitude, args.timezone, args.at),
                     indent=2, ensure_ascii=False))


if __name__ == "__main__":
    main()
