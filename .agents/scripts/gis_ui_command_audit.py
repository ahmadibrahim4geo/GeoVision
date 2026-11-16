#!/usr/bin/env python3
"""Read-only declaration inventory for basic HTML and XAML GIS UI controls.

This scanner is intentionally conservative. It does not execute the target app,
resolve dynamic/event-delegated listeners, verify GIS outcomes or prove that a
declared command handler exists or works. Native UI testing is separate.
"""
import argparse
import json
import sys
import xml.etree.ElementTree as ET
from collections import Counter
from html.parser import HTMLParser
from pathlib import Path
from urllib.parse import urlsplit, unquote

XAML_TAGS = {
    "Button", "ToggleButton", "RadioButton", "MenuItem", "RibbonButton",
    "RibbonToggleButton", "ToolBarButton", "CheckBox",
}
HTML_TAGS = {"button", "input", "a"}
MAX_FILES = 2000
MAX_BYTES = 1_000_000


def local_name(value):
    return value.rsplit("}", 1)[-1].rsplit(":", 1)[-1]


def attr(attributes, key):
    for k, v in attributes.items():
        if local_name(k) == key:
            return v
    return ""


def normalize_entry(path, kind, tag, index, element_id="", label="", icon="",
                    binding="", binding_kind="", classification="UNINSPECTED"):
    return {
        "command_id": element_id or f"candidate:{path}:{tag}:{index}",
        "id_is_candidate": not bool(element_id),
        "source_file": path,
        "ui_element_id": element_id or None,
        "surface": kind,
        "element_type": tag,
        "label": label or None,
        "icon_ref": icon or None,
        "binding_kind": binding_kind or None,
        "binding_target": binding or None,
        "static_status": classification,
        "runtime_status": "NOT RUN",
    }


class HTMLControls(HTMLParser):
    def __init__(self, path):
        super().__init__(convert_charrefs=True)
        self.path = path
        self.entries = []
        self.stack = []
        self.counter = 0

    def handle_starttag(self, tag, pairs):
        # Nested spans/images can label an interactive control.
        attributes = dict(pairs)
        if tag == "img" and self.stack and attributes.get("src"):
            self.stack[-1][1]["icon_ref"] = self.stack[-1][1]["icon_ref"] or attributes["src"]
        interactive = (
            tag == "button"
            or (tag == "input" and attributes.get("type", "").lower() in {"button", "submit", "reset", "image"})
            or (tag == "a" and attributes.get("role", "").lower() == "button")
        )
        if not interactive:
            return
        self.counter += 1
        ident = attributes.get("data-command-id") or attributes.get("id", "")
        handler = attributes.get("onclick") or attributes.get("data-command") or ""
        kind = "inline-onclick" if attributes.get("onclick") else "data-command" if attributes.get("data-command") else ""
        status = "DECLARED_BINDING" if handler else "UNINSPECTED"
        label = attributes.get("aria-label") or attributes.get("title") or attributes.get("value") or ""
        entry = normalize_entry(self.path, "html", tag, self.counter, ident, label,
                                attributes.get("data-icon", "") or attributes.get("src", ""),
                                handler, kind, status)
        self.entries.append(entry)
        if tag != "input":
            self.stack.append((tag, entry, []))

    def handle_data(self, data):
        for _, _, content in self.stack:
            content.append(data)

    def handle_endtag(self, tag):
        # Close only the last matching interactive ancestor.
        for i in range(len(self.stack) - 1, -1, -1):
            if self.stack[i][0] == tag:
                _, entry, content = self.stack.pop(i)
                if not entry["label"]:
                    entry["label"] = " ".join(" ".join(content).split()) or None
                return


def scan_html(text, path):
    p = HTMLControls(path)
    p.feed(text)
    p.close()
    return p.entries


def scan_xaml(text, path):
    if "<!DOCTYPE" in text.upper() or "<!ENTITY" in text.upper():
        raise ValueError("DTD and entity declarations are not accepted")
    root = ET.fromstring(text)
    entries = []
    for element in root.iter():
        tag = local_name(element.tag)
        if tag not in XAML_TAGS:
            continue
        attributes = element.attrib
        ident = attr(attributes, "AutomationId") or attr(attributes, "Name")
        handler = attr(attributes, "Command") or attr(attributes, "Click")
        # A submenu container with no action is structural UI, not a command.
        if tag == "MenuItem" and not handler and any(
                local_name(child.tag) == "MenuItem" for child in element):
            continue
        binding_kind = "Command" if attr(attributes, "Command") else "Click" if attr(attributes, "Click") else ""
        label = attr(attributes, "Name") if tag in {"CheckBox", "RadioButton"} else ""
        label = attr(attributes, "Content") or attr(attributes, "Header") or attr(attributes, "ToolTip") or label
        icon = attr(attributes, "Icon")
        if not icon:
            icon = next((attr(node.attrib, "Source") for node in element.iter()
                         if local_name(node.tag) == "Image" and attr(node.attrib, "Source")), "")
        status = "DECLARED_BINDING" if handler else "NO_BINDING"
        entries.append(normalize_entry(path, "xaml", tag, len(entries) + 1,
                                       ident, label, icon, handler, binding_kind, status))
    return entries


def inspect_local_icon(root, file_path, icon):
    """Only resolve explicit local image paths, never network/resources/dynamic."""
    if not icon or icon.startswith(("{", "@", "#", "data:", "pack:", "ms-appx:", "qrc:", "//")):
        return None
    value = urlsplit(icon)
    if value.scheme or not value.path:
        return None
    candidate = Path(unquote(value.path))
    if candidate.suffix.casefold() not in {".svg", ".png", ".jpg", ".jpeg", ".webp", ".gif", ".ico"}:
        return None
    if candidate.is_absolute():
        return None
    path = (root / file_path).parent / candidate
    if not path.resolve().is_relative_to(root.resolve()):
        return {"source_file": file_path, "icon_ref": icon, "reason": "icon path escapes scan root"}
    if not path.is_file():
        return {"source_file": file_path, "icon_ref": icon, "reason": "icon asset missing"}
    return None


def audit_tree(directory):
    directory = Path(directory)
    if not directory.is_dir():
        raise ValueError("Target root is not a directory")
    results, errors, scanned = [], [], 0
    # Sorted for deterministic stable fixture results. Ignore symlinked files.
    files = sorted(p for p in directory.rglob("*") if p.is_file() and not p.is_symlink()
                   and p.suffix.lower() in {".html", ".htm", ".xaml"})
    if len(files) > MAX_FILES:
        raise ValueError(f"More than {MAX_FILES} UI sources; narrow scan root")
    for file in files:
        rel = file.relative_to(directory).as_posix()
        scanned += 1
        try:
            if file.stat().st_size > MAX_BYTES:
                raise ValueError(f"File exceeds {MAX_BYTES} byte safety cap")
            source = file.read_text(encoding="utf-8-sig")
            entries = scan_xaml(source, rel) if file.suffix.lower() == ".xaml" else scan_html(source, rel)
            results.extend(entries)
        except (UnicodeError, OSError, ET.ParseError, ValueError) as exc:
            errors.append({"source_file": rel, "error": str(exc)})
    missing_icons = []
    for item in results:
        issue = inspect_local_icon(directory, item["source_file"], item["icon_ref"])
        if issue:
            missing_icons.append(issue)
    groups = Counter(item["static_status"] for item in results)
    # DOM IDs and XAML names are scoped to their view, not the entire application.
    # Shared command handlers/IDs across surfaces can be intentional.
    identifiers = [(item["source_file"], item["ui_element_id"])
                   for item in results if item["ui_element_id"]]
    duplicates = sorted(f"{path}#{ident}" for (path, ident), count
                        in Counter(identifiers).items() if count > 1)
    return {
        "kind": "static_ui_declaration_inventory",
        "engine_execution": False,
        "native_ui_tested": False,
        "files_scanned": scanned,
        "controls_found": len(results),
        "status_counts": {status: groups.get(status, 0)
                          for status in ("DECLARED_BINDING", "NO_BINDING", "UNINSPECTED")},
        "duplicate_declared_ids": duplicates,
        "missing_icon_assets": missing_icons,
        "entries": results,
        "errors": errors,
        "limitations": ["HTML dynamic/delegated listeners are not resolved",
                        "XAML handlers and templated controls are not semantically resolved",
                        "No claim of working GIS commands without native E2E evidence"],
    }


def main(argv=None):
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--root", required=True, help="Folder of local HTML/XAML UI sources")
    p.add_argument("--format", choices=("json", "text"), default="json")
    args = p.parse_args(argv)
    try:
        report = audit_tree(args.root)
    except ValueError as exc:
        p.error(str(exc))
    if args.format == "json":
        print(json.dumps(report, ensure_ascii=False, indent=2))
    else:
        print(f"Scanned {report['files_scanned']} files; found {report['controls_found']} declared UI controls")
        for name, count in report["status_counts"].items():
            print(f"  {name}: {count}")
        for entry in report["entries"]:
            print(f"  {entry['source_file']} / {entry['command_id']} : {entry['static_status']}")
        for issue in report["errors"]:
            print(f"  ERROR {issue['source_file']}: {issue['error']}", file=sys.stderr)
    return 1 if report["errors"] or report["duplicate_declared_ids"] or report["missing_icon_assets"] else 0


if __name__ == "__main__":
    raise SystemExit(main())
