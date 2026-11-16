# Security Tools — Free-first Selection and CI Integration

[Machine-readable registry](security-tools.json) includes upstream projects, functions, recorded license status and whether network access/credentials may be required. Do **not** copy third-party code or claim a license from an unrelated README; re-check upstream LICENSE before reuse. Tools below are **optional binaries**, not vendored dependencies.

## Minimal gates (always)

- Run repository's dependency-free python security_audit.py. It checks skill metadata, known dangerous command patterns and archive guards **heuristically**. It does not detect all prompt injections, credential formats or vulnerabilities.
- Run full tests and skill/manifest validators from existing CI. Use limited build permissions, no repo write tokens on untrusted pull requests.
- Every security decision must report executed commands and indicate skipped/unavailable external scanners.

## Choose scanners as needed

| Task | Recommended upstream project | Usage discipline |
| --- | --- | --- |
| External Agent Skill files | Cisco Skill Scanner | Scan only reviewed local copies; mark LLM-assisted/cloud modes as opt-in and avoid uploading sensitive skills |
| Third-party MCP servers | Cisco MCP Scanner | Inspect tool schemas/permissions; do not auto-connect to unknown servers |
| Python / ArcPy / PyQGIS | Bandit; Semgrep | Pin scanner versions and rules; triage false positives |
| Secrets in code/history | Gitleaks | Scan tracked changes and history before publication; do not print discovered secrets |
| Dependency vulnerabilities | OSV-Scanner; Trivy | Use lockfiles/SBOM, check advisories and provenance; network-dependent scans are not "offline" |
| Workflow permissions | zizmor | Audit GitHub Actions for token and expression injection; pin Actions by full commit SHAs |
| Adversarial agent evaluation | promptfoo; garak | Controlled sandbox with synthetic secrets and zero production credentials |
| Advanced research | Trail of Bits Skills; OSS-Fuzz | Reference methodology; check upstream licenses and execution preconditions |

## Integration guide

Use CI to enforce the pure-stdlib preflight on every PR. Configure supplementary SAST/dependency/secrets workflows only after review of scanner provenance and versions; limit read privileges to contents:read. Never perform an automatic install from a README, latest-tag URL, pipe-to-shell command, or arbitrary unreviewed package.

The provided offline workflow intentionally does **not** claim to have run Gitleaks, Bandit, Semgrep or Skill Scanner. Their absence is not green security evidence; mark them as not run and schedule audited deployment-time scans separately.

Reference security check command:

    python scripts/security_audit.py --root . --format json

The JSON output contains summary, findings and a list of scanners that were not run. It is intentionally free of secrets.
