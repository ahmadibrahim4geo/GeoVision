#!/usr/bin/env python3
"""Read-only audit of GIS skill reachability and documented task ownership.

No agent, native GIS application, external service, command or permission is invoked.
"""
import argparse
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SHARED = {
    "gis-development": "router",
    "prompt-intent-orchestrator": "intake",
    "gis-security-guardian": "security",
    "completion-evidence": "completion",
    "agent-development-journal": "journal",
}
COMMAND_DATA = {
    "gis-architecture-visualization": "architecture and command/data explanation (conditional)",
    "gis-ui-command-audit": "command inventory (conditional)",
    "gis-data-flow-diagnostics": "import-to-readback lineage (conditional)",
    "gis-desktop-e2e-testing": "native UI (conditional)",
    "gis-ui-ux-design-testing": "design and accessibility (conditional)",
    "system-capabilities-and-permissions": "device permissions (conditional)",
}
STAGES = [
    ("intake", "prompt-intent-orchestrator", "original prompt, outcome and reviewable plan"),
    ("route", "gis-development", "selected domain owner and dependency order"),
    ("preflight", "gis-security-guardian", "host tools, permissions, inputs and licensing"),
    ("execution", "selected domain specialist", "authorized changes and artifacts"),
    ("verification", "completion-evidence", "criteria, observed evidence and T0-T4 limits"),
    ("journal", "agent-development-journal", "verified history, handoff and unresolved gaps"),
]


def audit(root=ROOT):
    root = Path(root)
    issues = []
    inventory = json.loads((root / "references/repository-manifest.json").read_text("utf-8"))
    ids = sorted(p.parent.name for p in (root / "skills").glob("*/SKILL.md"))
    if inventory.get("skill_ids") != ids or inventory.get("skill_count") != len(ids):
        issues.append("Manifest does not match discovered skill files")
    router = (root / "skills/gis-development/SKILL.md").read_text("utf-8")
    routed = set(re.findall(r"\.\./([a-z0-9-]+)/SKILL\.md", router))
    expected = set(ids) - {"gis-development", "agent-development-journal"}
    for missing in sorted(expected - routed):
        issues.append("Unreachable from main router: " + missing)
    for missing in sorted(routed - set(ids)):
        issues.append("Router references missing skill: " + missing)
    policy = (root / "references/free-first-policy.md").read_text("utf-8")
    for skill in ids:
        if not re.search(r"\| `" + re.escape(skill) + r"` \|", policy):
            issues.append("Missing free-first policy entry: " + skill)
    intent = (root / "scripts/intent_router.py").read_text("utf-8")
    hinted = set(re.findall(r'\("([a-z0-9-]+)",\s*\(', intent))
    for skill in sorted(set(ids) - set(SHARED) - hinted):
        issues.append("Domain is missing lexical route hints: " + skill)
    for skill in sorted(hinted - set(ids)):
        issues.append("Lexical router points to missing skill: " + skill)
    entry = (root / "AGENTS.md").read_text("utf-8")
    workflow = (root / "references/repository-wide-skill-orchestration.md").read_text("utf-8")
    for skill in SHARED:
        if skill not in entry and skill != "gis-development":
            issues.append("Shared skill absent from AGENTS.md: " + skill)
        if skill not in workflow and skill != "gis-development":
            issues.append("Shared skill absent from workflow contract: " + skill)
    for skill in COMMAND_DATA:
        if skill not in routed:
            issues.append("Conditional command/data owner not routed: " + skill)
    return {
        "kind": "read_only_skill_flow_audit",
        "valid": not issues, "skills": len(ids), "routed": len(routed),
        "lexical_domains": len(hinted & set(ids)),
        "shared_owners": SHARED, "conditional_owners": COMMAND_DATA,
        "flow": [{"stage": stage, "owner": owner, "responsibility": role}
                 for stage, owner, role in STAGES],
        "issues": issues,
        "limitations": "Static metadata and document references only. No real host execution or approval.",
    }


def main(argv=None):
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--root", type=Path, default=ROOT)
    p.add_argument("--json", action="store_true")
    args = p.parse_args(argv)
    result = audit(args.root)
    if args.json:
        print(json.dumps(result, indent=2, ensure_ascii=False))
    else:
        print("Skill flow audit: {} | skills={} | routed={} | lexical domains={}".format(
            "PASS" if result["valid"] else "FAIL",
            result["skills"], result["routed"], result["lexical_domains"]))
        for issue in result["issues"]:
            print("ERROR: " + issue)
        print("NOTE: " + result["limitations"])
    return 0 if result["valid"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
