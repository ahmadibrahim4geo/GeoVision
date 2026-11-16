# Safe recurring GIS service availability checks

The separate [GitHub Actions workflow](../.github/workflows/monitor-services.yml) runs **weekly** or on-demand. It does **not** block normal code CI when an external provider is temporarily unavailable.

- Uses one HTTPS `HEAD` request at most per **39 catalog entries**, restricted to a separately committed [approved host list](../references/approved-service-hosts.json), no redirects, no API tokens and bounded timeouts.
- Rejects loopback/non-public IP addresses resolved from provider hostnames and cannot accept an arbitrary URL from the command line. The checks are not a substitute for a proxy with address-level egress enforcement.
- Produces an artifact `service-health-report.json` with `reachable`, `auth_required`, `rate_limited`, `deprecated`, `unknown`. `policy_changed` is reserved for **manual, confirmed** license/terms review, never inferred from HTTP 200.
- Documentation-site reachability is **not** proof that live API GET returns valid JSON, that a public service is free, or that a particular data product is current. A `405` response to HEAD is `unknown`, not a provider failure.
- Do not automatically edit the 39 catalog `verified_on` timestamps based on this lightweight reachability test.

Offline local run:
```bash
python scripts/monitor_public_services.py --output service-health-report.json
```
For live calls use `--network` only with reviewed hosts and provider permission. Discuss endpoint auth/quota/policy manually before production integration. Statuses are conservative and historical reports remain artifacts for 30 days.
