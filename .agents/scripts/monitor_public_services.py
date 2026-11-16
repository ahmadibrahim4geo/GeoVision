#!/usr/bin/env python3
"""Opt-in, bounded, read-only documentation URL health report. Never probes arbitrary user URLs."""
import argparse
from datetime import datetime, timezone
import ipaddress
import json
from pathlib import Path
import socket
import urllib.error
import urllib.request
from urllib.parse import urlsplit

ROOT = Path(__file__).resolve().parents[1]
STATES = ("reachable", "auth_required", "rate_limited", "deprecated", "policy_changed", "unknown")


def valid_url(url, hosts, resolver=socket.getaddrinfo):
    try:
        parts = urlsplit(url)
        if parts.scheme != "https" or not parts.hostname or parts.hostname.lower() not in hosts:
            return False
        if parts.username or parts.password or parts.fragment or (parts.port not in (None, 443)):
            return False
        addresses = resolver(parts.hostname, 443, type=socket.SOCK_STREAM)
        return bool(addresses) and all(ipaddress.ip_address(x[4][0]).is_global for x in addresses)
    except (OSError, ValueError, TypeError):
        return False


def classify(code, manually_confirmed_policy_change=False):
    if manually_confirmed_policy_change:
        return "policy_changed"
    if 200 <= code < 300:
        return "reachable"
    if code in (401, 403):
        return "auth_required"
    if code == 429:
        return "rate_limited"
    if code == 410:
        return "deprecated"
    return "unknown"


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, request, fp, code, msg, headers, newurl):
        return None


def probe(url, timeout):
    request = urllib.request.Request(url, method="HEAD", headers={
        "User-Agent": "GIS-Development-Skills-AvailabilityAudit/1.0"})
    opener = urllib.request.build_opener(NoRedirect())
    try:
        with opener.open(request, timeout=timeout) as response:
            return classify(response.status), "HTTP {}".format(response.status)
    except urllib.error.HTTPError as exc:
        return classify(exc.code), "HTTP {}".format(exc.code)
    except (urllib.error.URLError, TimeoutError, OSError, ValueError) as exc:
        return "unknown", type(exc).__name__


def inspect(items, allow_hosts, network=False, max_checks=39, timeout=4):
    output = []
    used = 0
    for service in items:
        url = service.get("documentation_url", "")
        result = {"id": service.get("id"), "name": service.get("name"), "url": url,
                  "status": "unknown", "reason": "not probed"}
        if not network:
            result["reason"] = "offline-only; no network call"
        elif used >= max_checks:
            result["reason"] = "bounded max checks exceeded"
        elif urlsplit(url).hostname not in allow_hosts:
            result["reason"] = "host not in reviewed allowlist"
        elif not valid_url(url, allow_hosts):
            result["reason"] = "unsafe or unresolvable HTTPS host"
        else:
            used += 1
            result["status"], result["reason"] = probe(url, timeout)
        result["policy_review"] = "human review required; availability does not prove terms"
        output.append(result)
    return output


def main(argv=None):
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--network", action="store_true", help="opt in to safe HTTPS HEAD requests")
    p.add_argument("--max-checks", type=int, default=39)
    p.add_argument("--timeout", type=float, default=4)
    p.add_argument("--output", type=Path, required=True)
    a = p.parse_args(argv)
    if not 0 <= a.max_checks <= 39 or not 1 <= a.timeout <= 10:
        p.error("Use max-checks 0..39 and timeout 1..10 seconds")
    catalog = json.loads((ROOT / "references/free-geospatial-apis.json").read_text(encoding="utf-8"))["services"]
    hosts = set(json.loads((ROOT / "references/approved-service-hosts.json").read_text(encoding="utf-8"))["hosts"])
    rows = inspect(catalog, hosts, a.network, a.max_checks, a.timeout)
    report = {"checked_utc": datetime.now(timezone.utc).isoformat(), "network_opt_in": a.network,
              "method": "HTTPS HEAD on reviewed documentation hosts; NOT actual API JSON/schema or license verification",
              "results": rows}
    a.output.parent.mkdir(parents=True, exist_ok=True)
    a.output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print("Reported {} services: {} probed".format(len(rows), sum(row["reason"].startswith("HTTP") for row in rows)))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
