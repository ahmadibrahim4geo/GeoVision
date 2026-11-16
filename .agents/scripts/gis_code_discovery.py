#!/usr/bin/env python3
"""GIS Code Discovery & Reuse Engine.
Searches, analyzes, and evaluates GIS code snippets, SDK examples, and recipes.
Deterministic, offline-first, no external network dependencies.
"""
import argparse
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCES_PATH = ROOT / "references" / "gis-code-sources.json"
RECIPES_PATH = ROOT / "references" / "gis-code-recipes.json"

PLATFORM_PATTERNS = {
    "arcgis-pro-sdk": [
        r"\barcgis\s+pro\s+sdk\b", r"\badd-?in\b", r"\bqueuedtask\b",
        r"\bribbon\b", r"\bdockpane\b", r"\bc#\b", r"\b\.net\b", r"\bbutton\b"
    ],
    "arcpy": [
        r"\barcpy\b", r"\bgeoprocessing\b", r"\bfeature\s+class\b",
        r"\bspatial\s+analyst\b", r"\bgdb\b", r"\bgeodatabase\b"
    ],
    "pyqgis": [
        r"\bpyqgis\b", r"\bqgis\b", r"\bprocessing\s+algorithm\b",
        r"\bqgsprocessing\b", r"\bplugin\b", r"\bإضافة\s+qgis\b"
    ],
    "gdal-rasterio": [
        r"\bgdal\b", r"\brasterio\b", r"\bdem\b", r"\belevation\b",
        r"\bsample\s+raster\b", r"\bgeotiff\b", r"\bnodata\b", r"\bارتفاع\b"
    ],
    "web-gis": [
        r"\bweb\s*gis\b", r"\bmaplibre\b", r"\bleaflet\b", r"\bopenlayers\b",
        r"\bvector\s+tiles\b", r"\bpmtiles\b", r"\bخريطة\s+تفاعلية\b"
    ],
    "kml-kmz": [
        r"\bkml\b", r"\bkmz\b", r"\bplacemark\b", r"\bstyle\b", r"\baabbggrr\b"
    ],
    "spatial-modeling": [
        r"\bspatial\s+prediction\b", r"\bspatial\s+machine\s+learning\b",
        r"\bscikit-map\b", r"\bensemble\b", r"\bتنبؤ\s+مكاني\b", r"\bتعلم\s+آلي\s+مكاني\b"
    ]
}


def load_registry(path):
    path = Path(path)
    if not path.is_file():
        return {}
    return json.loads(path.read_text(encoding="utf-8"))


def detect_platforms(query_text):
    text = query_text.lower()
    matches = []
    for platform, patterns in PLATFORM_PATTERNS.items():
        if any(re.search(pat, text, re.I) for pat in patterns):
            matches.append(platform)
    return matches or ["generic-gis"]


def evaluate_candidate(item, query_text, target_platform=None):
    """Score candidate recipe or source across 4 dimensions: fit, platform, license, spatial."""
    score = 0
    issues = []
    text = query_text.lower()
    item_title = item.get("title") or item.get("source_name", "")
    item_domain = item.get("domain") or item.get("source_type", "")
    item_lang = item.get("language") or ", ".join(item.get("programming_languages", []))
    item_lic = item.get("license", "unknown").lower()

    # 1. Functional Fit
    tokens = [t for t in re.split(r"\W+", text) if len(t) > 2]
    matched_tokens = [t for t in tokens if t in (item_title + " " + item_domain + " " + json.dumps(item)).lower()]
    token_ratio = len(matched_tokens) / max(1, len(tokens))
    score += int(token_ratio * 40)

    # 2. Platform & Language Match
    detected = detect_platforms(query_text)
    if target_platform:
        if target_platform.lower() in (item_domain + " " + item_lang).lower():
            score += 30
        else:
            issues.append(f"Platform mismatch: expected {target_platform}")
    elif any(d in (item_domain + " " + item_lang).lower() for d in detected):
        score += 30
    else:
        score += 10

    # 3. License Permissibility
    if any(p in item_lic for p in ["mit", "apache", "bsd", "cc0"]):
        score += 20
    elif "gpl" in item_lic:
        score += 15
        issues.append("Copyleft license (GPL): requires architectural isolation for proprietary targets")
    elif "unknown" in item_lic or "unverified" in item_lic:
        issues.append("Unverified license: direct copy prohibited, study conceptual logic only")
    else:
        score += 10

    # 4. Spatial Fidelity
    if "spatial_constraints" in item:
        score += 10
    else:
        issues.append("No explicit spatial constraints declared")

    return {
        "score": min(100, score),
        "issues": issues,
        "is_safe_to_adapt": "Unverified license" not in " ".join(issues)
    }


def search_knowledge_base(query, platform=None, sources_data=None, recipes_data=None):
    if sources_data is None:
        sources_data = load_registry(SOURCES_PATH)
    if recipes_data is None:
        recipes_data = load_registry(RECIPES_PATH)

    sources = sources_data.get("sources", [])
    recipes = recipes_data.get("recipes", [])
    detected = detect_platforms(query)

    ranked_recipes = []
    for r in recipes:
        eval_res = evaluate_candidate(r, query, target_platform=platform)
        ranked_recipes.append({
            "type": "recipe",
            "id": r.get("recipe_id"),
            "title": r.get("title"),
            "domain": r.get("domain"),
            "language": r.get("language"),
            "state": r.get("state"),
            "license": r.get("license"),
            "code_snippet": r.get("code_snippet"),
            "score": eval_res["score"],
            "issues": eval_res["issues"],
            "is_safe_to_adapt": eval_res["is_safe_to_adapt"]
        })

    ranked_sources = []
    for s in sources:
        eval_res = evaluate_candidate(s, query, target_platform=platform)
        ranked_sources.append({
            "type": "source",
            "id": s.get("source_id"),
            "name": s.get("source_name"),
            "url": s.get("source_url"),
            "languages": s.get("programming_languages"),
            "license": s.get("license"),
            "score": eval_res["score"],
            "issues": eval_res["issues"],
            "limitations": s.get("limitations")
        })

    ranked_recipes.sort(key=lambda x: x["score"], reverse=True)
    ranked_sources.sort(key=lambda x: x["score"], reverse=True)

    return {
        "query": query,
        "detected_platforms": detected,
        "matching_recipes": ranked_recipes[:5],
        "matching_sources": ranked_sources[:5]
    }


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--query", type=str, required=True, help="Natural language GIS code query")
    parser.add_argument("--platform", type=str, default=None, help="Target platform (arcgis-pro-sdk, arcpy, pyqgis, gdal, web-gis)")
    parser.add_argument("--format", choices=["text", "json"], default="text", help="Output format")
    args = parser.parse_args(argv)

    results = search_knowledge_base(args.query, platform=args.platform)

    if args.format == "json":
        print(json.dumps(results, indent=2, ensure_ascii=False))
    else:
        print(f"\n{'='*70}")
        print(f"GIS Code Discovery: '{args.query}'")
        print(f"Detected Platforms: {', '.join(results['detected_platforms'])}")
        print(f"{'='*70}\n")
        print("Matching Code Recipes:")
        for r in results["matching_recipes"]:
            print(f"  [{r['score']}%] {r['title']} ({r['language']}, State: {r['state']}, License: {r['license']})")
            if r["issues"]:
                print(f"       Note: {'; '.join(r['issues'])}")
        print("\nMatching Authoritative Sources:")
        for s in results["matching_sources"]:
            print(f"  [{s['score']}%] {s['name']} ({s['url']})")
        print()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
