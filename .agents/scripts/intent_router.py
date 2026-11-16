#!/usr/bin/env python3
"""Offline intent route hints and structural plan/approval declaration validation.

NO LLM, remote call, execution, authorization, file writes or OS isolation.
Run with --text for conservative bilingual keyword hints or --plan for a JSON
draft from a human/AI. --gate verifies declared fields, not user identity.
"""
import argparse
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SKILLS = ROOT / "skills"
DOMAINS = (
    ("gis-code-discovery", ("code discovery", "gis code", "code snippet", "code snippets", "sdk snippet", "arcgis sdk", "pyqgis snippet", "gdal sample", "gis recipe", "كود جغرافي", "خوارزمية جغرافية", "مقتطفات برمجية", "وصفات برمجية", "أمثلة برمجية", "كود python", "python code", "تنبؤ مكاني", "خوارزمية تنبؤ", "spatial prediction", "spatial machine learning", "تعلم آلي مكاني", "كود بحثي", "كودًا بحثيًا", "كودا بحثيا", "أكواد بحثية", "research code", "scikit-map")),
    ("visual-asset-discovery-integration", ("thumbnail", "visual asset", "image search", "image sourcing", "official logo", "logos", "brand asset", "سامبنيل", "سنبنيل", "ثَمبنيل", "صورة مصغرة", "صورة لكل اختيار", "صور توضيحية", "شعار", "شعارات")),

("gis-architecture-visualization", ("archify", "architecture diagram", "workflow diagram", "draw a diagram", "interactive diagram", "visualize architecture", "diagram the", "ارسملي", "ارسم لي", "مخطط", "مين مسؤول عن مين", "وضحلي بنية", "وضّحلي بنية")),
    ("clean-room-reference-implementation", ("clean room", "clean-room", "reference software", "reference implementation", "feature parity", "reverse engineering", "الهندسة النظيفة", "الإطار المرجعي", "تحليل برنامج مرجعي", "دراسة برنامج مرجعي", "مضاهاة جوجل إيرث")),
    ("kml-kmz-engineering", ("kml", "kmz", "placemark", "كي ام ال", "كى ام ال", "ستايل km", "ملفات الكي")),
    ("gis-desktop-application-development", ("desktop", "exe", "wpf", "زيرث", "zearth", "سطح المكتب", "برنامج مستقل")),
    ("gis-mobile-application-development", ("android", "ios app", "iphone", "gnss", "offline sync", "mobile gis", "موبايل", "هاتف", "تطبيق جوال", "تطبيق أندرويد", "تطبيق اندرويد")),
    ("terrain-hydrology-lidar", ("terrain", "dem", "elevation", "lidar", "تضاريس", "ارتفاع", "المبالغة", "الترين")),
    ("coordinate-reference-systems", ("crs", "epsg", "projection", "grid", "graticule", "خطوط الطول", "دوائر العرض", "الإحداثي", "مسقط")),
    ("gis-cartography-symbology", ("symbol", "style", "icon", "symbology", "أيقون", "الرموز", "ألوان", "ملصق")),
    ("arcgis-toolbox-engineering", ("arcgis", "arcpy", "arcmap", "pyt", "أرك جي", "بايثون تول")),
    ("qgis-plugin-development", ("qgis plugin", "qgis processing", "processing provider", "qgis extension", "إضافة qgis", "بلجن كيو جي")),
    ("qgis-engine-integration", ("qgis engine", "qgis core", "qgis libraries", "qgis standalone", "qgis runtime", "محرك qgis", "مكتبات qgis", "محرك كيو جي")),
    ("geospatial-web-mapping", ("cesium", "maplibre", "web map", "globe", "ثلاثية الأبعاد", "كرة أرضية")),
    ("remote-sensing-pipelines", ("sentinel", "landsat", "gee", "satellite", "الاستشعار", "مرئيات", "cog", "cloud optimized geotiff", "طبقة cog", "تغطية زمنية", "temporal coverage")),
    ("geospatial-data-processing", ("gdal", "geopandas", "raster", "vector", "راستر", "فيكتور")),
    ("spatial-databases", ("postgis", "spatialite", "spatial sql", "قواعد البيانات المكانية")),
    ("geospatial-api-integration", ("api", "geocod", "gazetteer", "أسماء الأماكن", "بحث عن الأماكن", "خدمة stac", "stac api", "واجهة برمجة")),
    ("geospatial-data-discovery", ("download data", "stac", "مصادر البيانات", "تنزيل بيانات", "خصائص التربة", "soil", "التربة", "land cover", "الغطاء الأرضي", "غطاء أرضي", "خرائط خصائص")),
    ("geospatial-ai-agents", ("agent", "geoai", "automation", "وكيل", "الذكاء الاصطناعي")),
    ("mcp-integration-and-tool-orchestration", ("mcp", "model context protocol", "mcp server", "mcp tools", "سيرفرات mcp", "سيرفر mcp", "توجيه الأدوات", "تكامل الأدوات")),
    ("geospatial-data-catalogs", ("data catalog", "data repository", "gis catalog", "stac catalog", "كتالوج البيانات", "فهرس البيانات", "مستودعات البيانات", "بيانات بيئية", "environmental data", "مستودعات البيانات البيئية", "مستودع بيانات", "opengeohub", "openlandmap", "مصدر بيانات مفتوح")),
    ("gis-internationalization-arabic", ("i18n", "localization", "arabic ui", "bilingual", "تعريب", "ثنائي اللغة", "تعدد اللغات")),
    ("system-capabilities-and-permissions", ("device permissions", "system permissions", "camera permission", "microphone permission", "bluetooth permission", "صلاحيات الجهاز", "أذونات الجهاز", "إذن الكاميرا")),
    ("gis-device-context", ("sunlight", "device location", "device time", "time zone", "solar position", "ضوء الشمس", "موقع الجهاز", "المنطقة الزمنية")),
    ("gis-ui-command-audit", ("command audit", "unwired command", "dead button", "ribbon audit", "audit icons", "فحص الأوامر", "جرد الأوامر", "أيقونات غير مربوطة", "الأزرار مش شغالة")),
    ("gis-desktop-e2e-testing", ("end-to-end desktop", "desktop e2e", "ui automation", "uiautomation", "flaUI", "webview2 testing", "اختبارات التفاعل", "اختبار تشغيل الواجهة", "اختبار تطبيق ويندوز")),
    ("gis-data-flow-diagnostics", ("data flow", "data lineage", "gis diagnostics", "trace id", "تدفق البيانات", "تتبع البيانات", "رحلة البيانات", "تشخيص تدفق")),
    ("gis-ui-ux-design-testing", ("ui/ux", "ui ux", "ux design", "usability testing", "accessibility audit", "تصميم واجهة", "تصميم الواجهة", "تجربة المستخدم", "اختبار قابلية الاستخدام")),
    ("global-mapper-development", ("global mapper", "جلوبال مابر", "جلوبال مابَر")),
)
RISKS = {"low", "medium", "high", "critical"}
SIDE_EFFECTS = {"read", "write", "network", "destructive"}
STATUSES = {"draft", "approved"}
VISUALIZATION_SKILL = "gis-architecture-visualization"
VISUALIZATION_FIELDS = {
    "purpose": {"architecture-explanation", "feature-design", "cross-component-diagnosis", "structural-change"},
    "diagram_type": {"architecture", "workflow", "sequence", "dataflow", "lifecycle", "erd", "class", "tree", "timeline", "waterfall"},
    "evidence_basis": {"proposed", "source-reviewed", "runtime-observed"},
    "language": {"en", "ar"},
}


def available_skills(root=SKILLS):
    return {p.parent.name for p in Path(root).glob("*/SKILL.md")}


def route_hints(request, known=None):
    """Lexical hints only; do NOT treat as semantic extraction."""
    if known is None:
        known = available_skills()
    lowered = request.casefold()
    hits = [name for name, tokens in DOMAINS
            if name in known and any(token in lowered for token in tokens)]
    if "gis-security-guardian" in known:
        hits.append("gis-security-guardian")
    return {
        "schema_version": "1.0",
        "stage": "draft_hints_only",
        "original_request": request,
        "goal": None,
        "target": None,
        "desired_outcomes": [],
        "next_step": "Hosting AI must infer and show the user goal, deliverables, constraints and measurable success criteria; no execution from keywords.",
        "language_hint": "ar" if re.search(r"[\u0600-\u06ff]", request) else "en",
        "suggested_skills": list(dict.fromkeys(hits)),
        "requires_semantic_review": True,
        "approval": "not_requested",
        "note": "Keyword matches only. A human/LLM must identify tasks, uncertainties, acceptance tests and obtain approval before tool dispatch."
    }


def _text(value):
    return isinstance(value, str) and bool(value.strip())


def validate_visualization(task, prefix):
    """Validate an optional declared handoff, never infer or execute a diagram."""
    selected = task.get("skills")
    has_skill = isinstance(selected, list) and VISUALIZATION_SKILL in selected
    if not has_skill and "visualization" not in task:
        return []
    errors = []
    if not has_skill:
        errors.append(prefix + ".visualization requires selecting " + VISUALIZATION_SKILL)
    declaration = task.get("visualization")
    if not isinstance(declaration, dict):
        return errors + [prefix + ".visualization must declare purpose, diagram_type, evidence_basis and language"]
    for key, choices in VISUALIZATION_FIELDS.items():
        value = declaration.get(key)
        if not isinstance(value, str) or value not in choices:
            errors.append(prefix + ".visualization." + key + " is missing or invalid")
    if set(declaration) - set(VISUALIZATION_FIELDS):
        errors.append(prefix + ".visualization contains unknown fields")
    return errors


def validate_plan(plan, known=None, require_approved=False):
    """Return structural errors, never execute the plan."""
    if known is None:
        known = available_skills()
    errors = []
    if not isinstance(plan, dict):
        return ["Plan must be a JSON object"]
    if plan.get("schema_version") != "1.0":
        errors.append("schema_version must be 1.0")
    for field in ("request_id", "revision", "original_request"):
        if not _text(plan.get(field)):
            errors.append(field + " must be a nonempty string")
    if plan.get("language") not in ("ar", "en"):
        errors.append("language must be ar or en")
    status = plan.get("status")
    if not isinstance(status, str) or status not in STATUSES:
        errors.append("status must be draft or approved")
    for field in ("assumptions", "open_questions"):
        val = plan.get(field)
        if not isinstance(val, list) or any(not _text(x) for x in val):
            errors.append(field + " must be a list of strings")
    approval = plan.get("approval")
    if not isinstance(approval, dict):
        approval = {}
        errors.append("approval must be an object")
    for field in ("approved_by_user", "high_risk_approved"):
        if type(approval.get(field)) is not bool:
            errors.append("approval." + field + " must be boolean")
    if not isinstance(approval.get("approved_revision"), (str, type(None))):
        errors.append("approval.approved_revision must be string or null")
    tasks = plan.get("tasks")
    if not isinstance(tasks, list) or not tasks:
        return errors + ["tasks must be a nonempty array"]
    ids = set()
    depends = {}
    high_risk = False
    for n, task in enumerate(tasks):
        pref = "tasks[" + str(n) + "]"
        if not isinstance(task, dict):
            errors.append(pref + " must be an object")
            continue
        ident = task.get("id")
        if not _text(ident) or not re.fullmatch(r"[A-Za-z][A-Za-z0-9_-]{0,39}", ident):
            errors.append(pref + ".id is invalid")
            continue
        if ident in ids:
            errors.append("duplicate task ID: " + ident)
        ids.add(ident)
        for field in ("title", "description", "kind"):
            if not _text(task.get(field)):
                errors.append(pref + "." + field + " must be a nonempty string")
        if task.get("priority") not in ("must", "should", "could"):
            errors.append(pref + ".priority invalid")
        risk = task.get("risk")
        if not isinstance(risk, str) or risk not in RISKS:
            errors.append(pref + ".risk invalid")
        if risk in ("high", "critical"):
            high_risk = True
        selected = task.get("skills")
        if not isinstance(selected, list) or not selected or any(not _text(x) or x not in known for x in selected):
            errors.append(pref + ".skills contain unknown or missing skill")
        if isinstance(selected, list) and "gis-security-guardian" not in selected:
            errors.append(pref + ".skills must include gis-security-guardian")
        errors.extend(validate_visualization(task, pref))
        deps = task.get("depends_on")
        if not isinstance(deps, list) or any(not _text(x) for x in deps):
            errors.append(pref + ".depends_on must be string array")
            deps = []
        if ident in deps:
            errors.append(pref + " cannot depend on itself")
        if len(deps) != len(set(deps)):
            errors.append(pref + " contains duplicate dependencies")
        depends[ident] = deps
        criteria = task.get("acceptance_criteria")
        if not isinstance(criteria, list) or not criteria or any(not _text(x) for x in criteria):
            errors.append(pref + ".acceptance_criteria must be nonempty string array")
        tools = task.get("tools", [])
        if not isinstance(tools, list):
            errors.append(pref + ".tools must be array")
            continue
        for item in tools:
            if not isinstance(item, dict) or not _text(item.get("tool_id")) or not isinstance(item.get("side_effect"), str) or item["side_effect"] not in SIDE_EFFECTS:
                errors.append(pref + ".tools entries need tool_id and valid side_effect")
            elif item["side_effect"] == "destructive":
                high_risk = True
    for ident, deps in depends.items():
        for dep in deps:
            if dep not in ids:
                errors.append(ident + " depends on missing task " + dep)
    visiting, visited = set(), set()
    def walk(node):
        if node in visiting:
            errors.append("dependency cycle at " + node)
            return
        if node in visited:
            return
        visiting.add(node)
        for dep in depends.get(node, []):
            if dep in depends:
                walk(dep)
        visiting.remove(node)
        visited.add(node)
    for ident in depends:
        walk(ident)
    if require_approved or status == "approved":
        if status != "approved" or approval.get("approved_by_user") is not True:
            errors.append("execution gate: plan is not user-approved")
        if not _text(plan.get("revision")) or approval.get("approved_revision") != plan.get("revision"):
            errors.append("execution gate: approval revision mismatch")
        if high_risk and approval.get("high_risk_approved") is not True:
            errors.append("execution gate: high-risk permission not declared")
    return errors



# A deterministic plan compiler: it organizes an agent-reviewed goal and its
# specialist tasks; it does NOT interpret natural language or execute tools.
WORKFLOW_PHASES = (
    "RECEIVED", "INTERPRETED", "DRAFTED", "AWAITING_REVIEW",
    "APPROVED", "ROUTED", "PREFLIGHT", "EXECUTING",
    "VERIFYING", "REPORTED",
)


def validate_goal_brief(plan):
    """Require a human/AI-authored and reviewable user outcome before routing."""
    if not isinstance(plan, dict):
        return ["Goal brief must be a JSON object"]
    errors = []
    if not _text(plan.get("goal")):
        errors.append("goal must describe the intended user outcome")
    if not _text(plan.get("target")):
        errors.append("target must describe the deliverable or project")
    for key in ("desired_outcomes", "success_criteria"):
        values = plan.get(key)
        if not isinstance(values, list) or not values or any(not _text(v) for v in values):
            errors.append(key + " must be a nonempty string array")
    # Existing plan v1 fields remain compatible; the goal-first workflow
    # requires these additional fields only when compiling a workflow.
    return errors


def ordered_tasks(tasks):
    """Stable topological ordering for a plan already validated against cycles."""
    tasks_by_id = {task["id"]: task for task in tasks}
    resolved, order = set(), []
    while len(resolved) < len(tasks):
        ready = [task for task in tasks
                 if task["id"] not in resolved
                 and all(dep in resolved for dep in task["depends_on"])]
        if not ready:
            raise ValueError("No valid task order (cycle or missing dependency)")
        for task in ready:
            order.append(tasks_by_id[task["id"]])
            resolved.add(task["id"])
    return order


def compile_workflow(plan, known=None, require_approved=False):
    """Emit an execution *blueprint* only. No shell, I/O writes or auth check.

    An AI hosting agent must semantically review the goal, verify the user's
    actual authorization, check installed tools and enforce side-effect limits.
    """
    errors = validate_plan(plan, known=known, require_approved=require_approved)
    errors.extend(validate_goal_brief(plan))
    if isinstance(plan, dict):
        for i, task in enumerate(plan.get("tasks", []) if isinstance(plan.get("tasks"), list) else []):
            if not isinstance(task, dict):
                continue
            selected = task.get("skills")
            owner = task.get("owner_skill")
            if not isinstance(owner, str) or not isinstance(selected, list) or owner not in selected or owner in (
                    "gis-security-guardian", "completion-evidence", "agent-development-journal",
                    "prompt-intent-orchestrator", "gis-development"):
                errors.append("tasks[{}].owner_skill must be a selected accountable domain specialist".format(i))
            if isinstance(selected, list) and selected and all(
                    skill in ("gis-security-guardian", "agent-development-journal",
                              "completion-evidence", "prompt-intent-orchestrator", "gis-development") for skill in selected):
                errors.append("tasks[{}] has no accountable domain specialist".format(i))
    result = {
        "schema_version": "1.0",
        "kind": "workflow_preview_only",
        "valid": not errors,
        "request_id": plan.get("request_id") if isinstance(plan, dict) else None,
        "revision": plan.get("revision") if isinstance(plan, dict) else None,
        "original_request": plan.get("original_request") if isinstance(plan, dict) else None,
        "goal": plan.get("goal") if isinstance(plan, dict) else None,
        "target": plan.get("target") if isinstance(plan, dict) else None,
        "desired_outcomes": plan.get("desired_outcomes") if isinstance(plan, dict) else None,
        "success_criteria": plan.get("success_criteria") if isinstance(plan, dict) else None,
        "phases": list(WORKFLOW_PHASES),
        "status": "BLOCKED" if errors else "AWAITING_HOST_AUTHORIZATION" if require_approved else "AWAITING_REVIEW",
        "approval_declarations_checked": bool(require_approved and not errors),
        "authenticated_user_approval": False,
        "host_permissions_verified": False,
        "can_execute": False,
        "note": "Reviewable, ordered blueprint only. An authenticated host must check approval, tool availability, repository scope and security at dispatch.",
        "errors": errors,
        "steps": [],
        "handoffs": [],
    }
    if errors:
        return result
    task_order = ordered_tasks(plan["tasks"])
    for step_number, task in enumerate(task_order, start=1):
        owner = task["owner_skill"]
        step = {
            "step": step_number,
            "task_id": task["id"],
            "title": task["title"],
            "owner_skill": owner,
            "skills": task["skills"],
            "depends_on": task["depends_on"],
            "risk": task["risk"],
            "candidate_tools": task.get("tools", []),
            "acceptance_criteria": task["acceptance_criteria"],
            "status": "NOT_STARTED",
            "preflight": ["host tool availability", "host authorization", "GIS/data/license compatibility",
                          "security guardian", "data preservation and rollback"],
            "verification": "T0/T1 where possible; T2-T4 only on real target runtimes",
            "completion_gate": "completion-evidence (DoneProof); validate each acceptance criterion against fresh results",
            "journal_owner": "agent-development-journal",
        }
        if "visualization" in task:
            step["visualization"] = {
                **task["visualization"],
                "renderer": "archify",
                "artifact_owner": VISUALIZATION_SKILL,
                "status": "NOT_STARTED",
                "runtime_verified": False,
                "gis_execution_proven": False,
            }
            step["preflight"].append("actual Node.js, Archify runtime and browser availability; local output and source privacy")
        result["steps"].append(step)
        result["handoffs"].extend(
            {"from_task": dep, "to_task": task["id"], "data_contract": "approved_plan_task_dependency"}
            for dep in task["depends_on"])
    return result


def main(argv=None):
    p = argparse.ArgumentParser(description=__doc__)
    action = p.add_mutually_exclusive_group(required=True)
    action.add_argument("--text", help="Unstructured input; get lexical skill hints, NOT AI understanding")
    action.add_argument("--plan", type=Path, help="Structured JSON plan drafted by an agent or human")
    p.add_argument("--format", choices=("text", "json"), default="text")
    p.add_argument("--gate", action="store_true", help="Check declared revision-scoped approval; never execute")
    p.add_argument("--workflow", action="store_true", help="Compile a goal-first dependency-ordered workflow preview; requires --plan, never executes")
    p.add_argument("--mcp-inventory", type=Path, help="Optional unverified MCP/native tool inventory for read-only capability preview; requires --workflow")
    args = p.parse_args(argv)
    if args.mcp_inventory and (not args.workflow or args.plan is None):
        p.error("--mcp-inventory requires --plan and --workflow")
    if args.text is not None:
        if args.gate:
            p.error("--gate requires --plan")
        if args.workflow:
            p.error("--workflow requires --plan")
        out = route_hints(args.text)
        if args.format == "json":
            print(json.dumps(out, ensure_ascii=False, indent=2))
        else:
            print("Possible skills (keyword hints only):", ", ".join(out["suggested_skills"]))
            print(out["note"])
        return 0
    try:
        plan = json.loads(args.plan.read_text(encoding="utf-8"))
    except (OSError, ValueError) as exc:
        print("Invalid plan file: " + str(exc), file=sys.stderr)
        return 2
    if args.workflow:
        blueprint = compile_workflow(plan, require_approved=args.gate)
        if args.mcp_inventory and blueprint["valid"]:
            try:
                from mcp_tool_router import route_plan
                inventory = json.loads(args.mcp_inventory.read_text(encoding="utf-8"))
                blueprint["tool_route_preview"] = route_plan(plan, inventory)
                blueprint["tool_routing_note"] = "Candidate capabilities only; no live MCP discovery, authorization or invocation."
            except (OSError, ValueError, TypeError) as exc:
                blueprint["valid"] = False
                blueprint["status"] = "BLOCKED"
                blueprint["errors"].append("MCP inventory preview invalid: " + str(exc))
        if args.format == "json":
            print(json.dumps(blueprint, ensure_ascii=False, indent=2))
        else:
            print("Goal:", blueprint["goal"])
            print("Target:", blueprint["target"])
            print("Status:", blueprint["status"])
            for item in blueprint["steps"]:
                print("{step}. {task_id} [{owner_skill}] after {depends_on} - {title}".format(**item))
            for issue in blueprint["errors"]:
                print("ERROR:", issue)
            print(blueprint["note"])
        return 0 if blueprint["valid"] else 1
    errors = validate_plan(plan, require_approved=args.gate)
    if args.format == "json":
        print(json.dumps({"valid": not errors, "approved_gate_declared": bool(args.gate and not errors),
                          "errors": errors, "note": "No execution or authenticated approval is performed."}, indent=2))
    else:
        for issue in errors:
            print("ERROR: " + issue)
        if not errors:
            print("Plan structure valid" + ("; approval declarations present (NOT authenticated)" if args.gate else "; review still required"))
    return 1 if errors else 0


if __name__ == "__main__":
    raise SystemExit(main())
