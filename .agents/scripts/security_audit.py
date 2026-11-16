#!/usr/bin/env python3
"""Dependency-free, read-only GIS skill-pack security preflight.

Heuristic checks only. External scanners are NEVER implicitly installed or run.
Reports never contain matching source lines or suspected secrets.
"""
import argparse
import json
import re
from pathlib import Path

from security_guardian import inspect_archive

PATTERNS = (
    ("remote-pipe-to-shell", "high",
     re.compile(r"\b(?:curl|wget)\b[^\n]{0,280}\|\s*(?:sudo\s+)?(?:sh|bash)\b", re.I)),
    ("recursive-root-delete", "high",
     re.compile(r"\brm\s+-[a-zA-Z]*r[a-zA-Z]*f[a-zA-Z]*\s+/(?:\s|$)", re.I)),
    ("disable-tls-verification", "medium",
     re.compile(r"(?:verify\s*=\s*False|--insecure\b|CERT_NONE\b)", re.I)),
    ("agent-instruction-override", "medium",
     re.compile(r"ignore\s+(?:all\s+)?(?:previous|prior)\s+instructions", re.I)),
)
NOT_RUN = ["Cisco Skill Scanner", "Gitleaks", "Bandit", "Semgrep",
           "OSV-Scanner", "zizmor", "MCP Scanner"]


def audit_skills(root):
    findings = []
    skills = root / "skills"
    for path in sorted(skills.glob("*/SKILL.md")):
        if path.is_symlink() or path.parent.is_symlink():
            findings.append({"code": "skill-symlink", "severity": "high",
                             "path": str(path.relative_to(root)), "line": 0})
            continue
        try:
            data = path.read_text(encoding="utf-8")
        except (OSError, UnicodeError):
            findings.append({"code": "unreadable-skill", "severity": "high",
                             "path": str(path.relative_to(root)), "line": 0})
            continue
        for line_no, line in enumerate(data.splitlines(), 1):
            for code, severity, pattern in PATTERNS:
                if pattern.search(line):
                    findings.append({"code": code, "severity": severity,
                                     "path": str(path.relative_to(root)),
                                     "line": line_no})
    return findings


def audit_registry(root):
    path = root / "references" / "security" / "security-tools.json"
    findings = []
    if not path.exists():
        return [{"code": "missing-tool-registry", "severity": "high",
                 "path": str(path.relative_to(root)), "line": 0}]
    try:
        record = json.loads(path.read_text(encoding="utf-8"))
        tools = record["tools"]
        ids = [t["id"] for t in tools]
        if not isinstance(tools, list) or not tools or len(ids) != len(set(ids)):
            raise ValueError("empty or duplicate tool entries")
        for t in tools:
            if not t["url"].startswith("https://github.com/"):
                raise ValueError("unexpected upstream URL")
            if t.get("automatically_executed") is not False or t.get("bundled") is not False:
                raise ValueError("tools must be optional and unbundled")
    except (ValueError, KeyError, TypeError, OSError, UnicodeError):
        findings.append({"code": "invalid-tool-registry", "severity": "high",
                         "path": str(path.relative_to(root)), "line": 0})
    return findings


def audit(root, archives=()):
    root = Path(root).resolve()
    findings = audit_skills(root) + audit_registry(root)
    archive_results = [inspect_archive(p) for p in archives]
    for result in archive_results:
        for code in result["issues"]:
            findings.append({"code": code, "severity": "high",
                             "path": result["archive"], "line": 0})
    blocking = sum(f["severity"] in ("high", "critical") for f in findings)
    return {
        "status": "fail" if blocking else "pass",
        "policy": "Heuristic preflight only; external scanners not executed.",
        "skill_count": len(list((root / "skills").glob("*/SKILL.md"))),
        "finding_count": len(findings),
        "blocking_findings": blocking,
        "findings": findings,
        "archives": archive_results,
        "external_scanners_not_run": NOT_RUN,
    }


def main(argv=None):
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
    p.add_argument("--archive", type=Path, action="append", default=[],
                   help="read-only ZIP/KMZ/Shapefile archive preflight (repeatable)")
    p.add_argument("--format", choices=("text", "json"), default="text")
    args = p.parse_args(argv)
    report = audit(args.root, args.archive)
    if args.format == "json":
        print(json.dumps(report, indent=2, ensure_ascii=False))
    else:
        print("GIS Security Guardian: {} | {} skills | {} findings ({} blocking)".format(
            report["status"], report["skill_count"],
            report["finding_count"], report["blocking_findings"]))
        for item in report["findings"]:
            print("  {} {} {}:{}".format(
                item["severity"].upper(), item["code"], item["path"], item["line"]))
        print("External scanners: NOT RUN (install and run separately if authorized).")
    return 1 if report["blocking_findings"] else 0


if __name__ == "__main__":
    raise SystemExit(main())
