#!/usr/bin/env python3
"""Read-only, standard-library candidate routing for declared MCP/native tools.

This is NOT an MCP client, connection test, authorization service, or executor.
Only a trusted hosting agent can enumerate authentic tools and enforce grants.
"""
import argparse
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REGISTRY = ROOT / "references" / "mcp-capability-registry.json"
TOOL_ID = re.compile(r"^[a-zA-Z][a-zA-Z0-9_.:-]{0,127}$")
EFFECTS = {"read", "write", "network", "execute", "destructive"}
PROTOCOLS = {"mcp", "native"}

class ContractError(ValueError):
    pass

def _nonblank(x):
    return isinstance(x, str) and bool(x.strip())

def _unique_strings(values, label):
    if not isinstance(values, list) or any(not _nonblank(v) for v in values):
        raise ContractError(label + " must be a string array")
    if len(values) != len(set(values)):
        raise ContractError(label + " contains duplicates")
    return values

def _registry_ids(registry):
    if not isinstance(registry, dict) or registry.get("schema_version") != "1.0":
        raise ContractError("Invalid capability registry")
    rows = registry.get("capabilities")
    if not isinstance(rows, list) or not rows:
        raise ContractError("Registry requires capabilities")
    ids = {}
    for c in rows:
        if not isinstance(c, dict) or not _nonblank(c.get("id")) or c["id"] in ids:
            raise ContractError("Missing/duplicate registry capability ID")
        effects = _unique_strings(c.get("side_effects"), "capability side_effects")
        if not set(effects).issubset(EFFECTS):
            raise ContractError("Unknown capability side effect")
        ids[c["id"]] = set(effects)
    return ids

def validate_inventory(inventory, registry):
    """Inventory is an *unverified declaration*; never treat it as discovery."""
    ids = _registry_ids(registry)
    if not isinstance(inventory, dict) or inventory.get("schema_version") != "1.0":
        raise ContractError("Inventory must use schema_version 1.0")
    tools = inventory.get("tools")
    if not isinstance(tools, list):
        raise ContractError("Inventory must contain tools array")
    seen = set()
    for tool in tools:
        if not isinstance(tool, dict) or not _nonblank(tool.get("tool_id")):
            raise ContractError("Tool requires tool_id")
        identifier = tool["tool_id"]
        if not TOOL_ID.fullmatch(identifier) or identifier in seen:
            raise ContractError("Unsafe or duplicate tool_id: " + identifier)
        seen.add(identifier)
        if tool.get("protocol") not in PROTOCOLS:
            raise ContractError("Tool protocol must be mcp or native")
        if type(tool.get("available")) is not bool:
            raise ContractError("Tool available must be boolean")
        capabilities = _unique_strings(tool.get("capabilities"), "tool capabilities")
        effects = _unique_strings(tool.get("side_effects"), "tool side_effects")
        if not set(capabilities).issubset(ids) or not set(effects).issubset(EFFECTS):
            raise ContractError("Tool declares unknown capability/side effect")
        for cap in capabilities:
            if not ids[cap].issubset(set(effects)):
                raise ContractError("Tool cannot satisfy advertised capability side effect: " + cap)
    return tools

def route_plan(plan, inventory, registry=None):
    """Return a non-executable candidate map ordered as plan tasks appear."""
    if registry is None:
        registry = json.loads(REGISTRY.read_text(encoding="utf-8"))
    allowed = _registry_ids(registry)
    tools = validate_inventory(inventory, registry)
    if not isinstance(plan, dict) or not isinstance(plan.get("tasks"), list):
        raise ContractError("Plan needs tasks array")
    routes = []
    for i, task in enumerate(plan["tasks"]):
        if not isinstance(task, dict) or not _nonblank(task.get("id")):
            raise ContractError("tasks[%s] must have id" % i)
        requests = task.get("capability_requests", [])
        if not isinstance(requests, list):
            raise ContractError("capability_requests must be array")
        selected = []
        for n, request in enumerate(requests):
            if not isinstance(request, dict):
                raise ContractError("capability request must be object")
            capability = request.get("capability")
            effect = request.get("side_effect")
            if capability not in allowed or effect not in EFFECTS or effect not in allowed[capability]:
                raise ContractError("Unknown/mismatched capability or side effect at task %s request %s" % (task["id"], n))
            matches = [t for t in tools if t["available"] and capability in t["capabilities"]
                       and effect in t["side_effects"]]
            # Prefer MCP when the host reports one, but never silently discard
            # an available native fallback or elevate permissions.
            matches.sort(key=lambda t: (0 if t["protocol"] == "mcp" else 1, t["tool_id"]))
            preferred = matches[0] if matches else None
            selected.append({
                "capability": capability, "side_effect": effect,
                "status": "CANDIDATE_ONLY" if preferred else "BLOCKED_NO_TOOL",
                "candidate_tool_id": preferred["tool_id"] if preferred else None,
                "candidate_protocol": preferred["protocol"] if preferred else None,
                "fallback_tool_ids": [t["tool_id"] for t in matches[1:]],
                "host_discovery_verified": False,
                "host_permission_verified": False,
                "can_execute": False
            })
        routes.append({"task_id": task["id"], "requirements": selected,
                       "status": "NEEDS_HOST_PREFLIGHT" if all(x["status"] == "CANDIDATE_ONLY" for x in selected)
                       else "BLOCKED_NO_TOOL"})
    return {
        "schema_version": "1.0", "kind": "mcp_native_candidate_preview",
        "inventory_authenticated": False, "server_connections_tested": False,
        "host_permissions_verified": False, "can_execute": False,
        "task_routes": routes,
        "blocked_requirements": sum(1 for row in routes for x in row["requirements"]
                                    if x["status"] == "BLOCKED_NO_TOOL"),
        "note": "Declared tool inventory only. The host must discover and verify every tool, consent, scope, credentials, side effect and observed result before any execution."
    }

def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--plan", type=Path, required=True)
    parser.add_argument("--inventory", type=Path, required=True)
    parser.add_argument("--registry", type=Path, default=REGISTRY)
    args = parser.parse_args(argv)
    try:
        plan = json.loads(args.plan.read_text(encoding="utf-8"))
        inventory = json.loads(args.inventory.read_text(encoding="utf-8"))
        registry = json.loads(args.registry.read_text(encoding="utf-8"))
        output = route_plan(plan, inventory, registry)
    except (OSError, ValueError, TypeError) as exc:
        print("Invalid MCP candidate request: " + str(exc), file=sys.stderr)
        return 2
    print(json.dumps(output, indent=2, ensure_ascii=False))
    return 1 if output["blocked_requirements"] else 0

if __name__ == "__main__":
    raise SystemExit(main())
