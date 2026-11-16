#!/usr/bin/env python3
"""Build and audit a self-contained GIS Development Skills distribution.

The source repository intentionally keeps shared references, examples and
scripts outside individual skill directories.  Installing one nested skill
directory therefore cannot be assumed to retain those dependencies.  This
builder creates a bounded, content-addressed suite with a root SKILL.md entry,
copies only reviewed repository surfaces, and verifies local Markdown links.

The builder never overwrites an existing bundle or archive.  It does not run
an agent, invoke a GIS host, publish a release or authenticate user approval.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
import urllib.parse
import zipfile
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
BUNDLE_NAME = "gis-development-suite"
SCHEMA_VERSION = "1.0"
FIXED_ZIP_TIME = (1980, 1, 1, 0, 0, 0)
MAX_FILE_BYTES = 10 * 1024 * 1024
MAX_BUNDLE_BYTES = 25 * 1024 * 1024

ALLOWED_ROOT_DIRECTORIES = (
    ".github",
    "assets",
    "docs",
    "examples",
    "fixtures",
    "native_tests",
    "references",
    "scripts",
    "skills",
    "templates",
    "tests",
)
ALLOWED_ROOT_FILES = (
    ".gitignore",
    "AGENTS.md",
    "CHANGELOG.md",
    "CONTRIBUTING.md",
    "LICENSE",
    "PUBLISHING.md",
    "README.md",
    "README.ar.md",
    "SECURITY.md",
    "explorer.html",
    "index.html",
    "project-wizard.html",
)
EXCLUDED_DIRECTORY_NAMES = {
    ".agent-development-log",
    ".git",
    ".idea",
    ".pytest_cache",
    ".venv",
    ".vscode",
    "__pycache__",
    "__pypackages__",
    "build",
    "dist",
    "node_modules",
    "venv",
}
EXCLUDED_FILE_SUFFIXES = {".pyc", ".pyo", ".tmp"}
SENSITIVE_FILE_NAMES = {
    "credentials.json",
    "id_dsa",
    "id_ecdsa",
    "id_ed25519",
    "id_rsa",
    "secrets.json",
}
SENSITIVE_FILE_SUFFIXES = {".key", ".p12", ".pem", ".pfx"}
REQUIRED_PATHS = (
    "SKILL.md",
    "LICENSE",
    "skills/gis-development/SKILL.md",
    "skills/prompt-intent-orchestrator/SKILL.md",
    "skills/gis-security-guardian/SKILL.md",
    "skills/completion-evidence/SKILL.md",
    "skills/agent-development-journal/SKILL.md",
    "references/repository-manifest.json",
    "references/repository-wide-skill-orchestration.md",
    "scripts/intent_router.py",
    "scripts/security_audit.py",
    "scripts/verify_completion.py",
    "templates/GIS_ACCEPTANCE_REPORT.md",
)
MARKDOWN_LINK = re.compile(r"!?\[[^\]]*\]\(([^)]+)\)")
SCHEME = re.compile(r"^[A-Za-z][A-Za-z0-9+.-]*:")


class BundleError(RuntimeError):
    """A deterministic, user-actionable bundle validation failure."""


def sha256_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def is_sensitive(path: Path) -> bool:
    name = path.name.casefold()
    return (
        name == ".env"
        or name.startswith(".env.")
        or name in SENSITIVE_FILE_NAMES
        or path.suffix.casefold() in SENSITIVE_FILE_SUFFIXES
    )


def is_excluded_relative(path: Path) -> bool:
    return (
        any(part in EXCLUDED_DIRECTORY_NAMES for part in path.parts[:-1])
        or path.suffix.casefold() in EXCLUDED_FILE_SUFFIXES
        or path.name.casefold() in {"thumbs.db", ".ds_store"}
    )


def iter_source_files(root: Path = ROOT):
    candidates = [root / name for name in ALLOWED_ROOT_FILES]
    for directory_name in ALLOWED_ROOT_DIRECTORIES:
        directory = root / directory_name
        if directory.exists():
            candidates.extend(directory.rglob("*"))

    seen = set()
    for path in sorted(candidates, key=lambda item: item.as_posix().casefold()):
        if not path.exists() or path.is_dir():
            continue
        relative = path.relative_to(root)
        if relative.as_posix() in seen or is_excluded_relative(relative):
            continue
        seen.add(relative.as_posix())
        if path.is_symlink():
            raise BundleError(f"Refusing symlink in bundle source: {relative.as_posix()}")
        if is_sensitive(relative):
            raise BundleError(f"Refusing sensitive-looking file: {relative.as_posix()}")
        size = path.stat().st_size
        if size > MAX_FILE_BYTES:
            raise BundleError(
                f"File exceeds {MAX_FILE_BYTES} byte bundle limit: {relative.as_posix()}"
            )
        yield relative, path


def root_skill_text() -> str:
    return """---
name: gis-development-suite
description: Route and support verifiable GIS software development across QGIS, ArcGIS, desktop, web, data and automation using the bundled specialist skills, safety gates, examples and evidence contracts.
license: MIT
---

# GIS Development Skills — self-contained suite

This distribution preserves the repository's shared files so its relative
references remain available after packaging.  It contains instructions and
deterministic helpers; it does not install GIS software, grant licenses or
prove native execution.

1. Read [the core GIS router](skills/gis-development/SKILL.md).
2. For complex requests, start with [Prompt Intent Orchestrator](skills/prompt-intent-orchestrator/SKILL.md) and obtain revision-scoped approval before consequential changes.
3. Always apply [GIS Security Guardian](skills/gis-security-guardian/SKILL.md).
4. Load only the specialist selected by the router and the references needed for the current task.
5. Before claiming completion, apply [DoneProof](skills/completion-evidence/SKILL.md) and record honest T0–T4 evidence.

Use [the bilingual How to Use guide](docs/HOW_TO_USE.md) and
[Arabic guide](docs/HOW_TO_USE.ar.md) for practical requests and limitations.
"""


def source_commit(root: Path = ROOT) -> str:
    try:
        result = subprocess.run(
            ["git", "rev-parse", "HEAD"],
            cwd=root,
            check=True,
            capture_output=True,
            text=True,
            timeout=10,
        )
        value = result.stdout.strip()
        return value if re.fullmatch(r"[0-9a-fA-F]{40}", value) else "unknown"
    except (OSError, subprocess.SubprocessError):
        return "unknown"


def inventory(bundle_root: Path) -> dict:
    manifest_path = bundle_root / "references" / "repository-manifest.json"
    if not manifest_path.is_file():
        raise BundleError("Missing references/repository-manifest.json")
    try:
        repository_manifest = json.loads(manifest_path.read_text("utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        raise BundleError(f"Invalid repository manifest: {exc}") from exc
    discovered = sorted(
        path.parent.name
        for path in (bundle_root / "skills").glob("*/SKILL.md")
    )
    declared = sorted(repository_manifest.get("skill_ids", []))
    if discovered != declared:
        raise BundleError("Bundled skills do not match repository-manifest skill_ids")
    return {
        "documentation_version": repository_manifest.get("documentation_version"),
        "skill_count": len(discovered),
        "skill_ids": discovered,
        "reference_count": repository_manifest.get("reference_count"),
        "api_count": repository_manifest.get("api_count"),
        "data_repository_count": repository_manifest.get("data_repository_count"),
    }


def manifest_entries(bundle_root: Path):
    for path in sorted(bundle_root.rglob("*"), key=lambda item: item.as_posix()):
        if not path.is_file() or path.name == "bundle-manifest.json":
            continue
        relative = path.relative_to(bundle_root).as_posix()
        yield {
            "path": relative,
            "sha256": sha256_file(path),
            "size": path.stat().st_size,
        }


def write_manifest(bundle_root: Path, commit: str) -> dict:
    entries = list(manifest_entries(bundle_root))
    total_bytes = sum(entry["size"] for entry in entries)
    if total_bytes > MAX_BUNDLE_BYTES:
        raise BundleError(
            f"Bundle exceeds {MAX_BUNDLE_BYTES} byte expanded-content limit"
        )
    data = {
        "schema_version": SCHEMA_VERSION,
        "bundle_name": BUNDLE_NAME,
        "entrypoint": "SKILL.md",
        "manifest_scope": "all bundle files except bundle-manifest.json",
        "source_commit": commit,
        "source_roots": list(ALLOWED_ROOT_DIRECTORIES),
        "excluded_directories": sorted(EXCLUDED_DIRECTORY_NAMES),
        "inventory": inventory(bundle_root),
        "file_count": len(entries),
        "expanded_bytes": total_bytes,
        "files": entries,
    }
    payload = json.dumps(data, ensure_ascii=False, indent=2, sort_keys=True) + "\n"
    (bundle_root / "bundle-manifest.json").write_text(payload, encoding="utf-8", newline="\n")
    return data


def local_markdown_target(raw: str) -> str | None:
    value = raw.strip()
    if not value or value.startswith("#"):
        return None
    if value.startswith("<") and ">" in value:
        value = value[1 : value.index(">")]
    else:
        value = value.split(maxsplit=1)[0]
    value = urllib.parse.unquote(value)
    if not value or value.startswith("#") or value.startswith("//") or SCHEME.match(value):
        return None
    return value.split("#", 1)[0].split("?", 1)[0]


def audit_markdown_links(bundle_root: Path) -> list[dict]:
    missing = []
    resolved_root = bundle_root.resolve()
    for document in sorted(bundle_root.rglob("*.md")):
        text = document.read_text("utf-8")
        for line_number, line in enumerate(text.splitlines(), 1):
            for match in MARKDOWN_LINK.finditer(line):
                target = local_markdown_target(match.group(1))
                if not target:
                    continue
                destination = (document.parent / target).resolve()
                try:
                    destination.relative_to(resolved_root)
                except ValueError:
                    missing.append(
                        {
                            "document": document.relative_to(bundle_root).as_posix(),
                            "line": line_number,
                            "target": target,
                            "reason": "escapes-bundle",
                        }
                    )
                    continue
                if not destination.exists():
                    missing.append(
                        {
                            "document": document.relative_to(bundle_root).as_posix(),
                            "line": line_number,
                            "target": target,
                            "reason": "missing",
                        }
                    )
    return missing


def audit_directory(bundle_root: Path) -> dict:
    manifest_path = bundle_root / "bundle-manifest.json"
    if not manifest_path.is_file():
        raise BundleError("bundle-manifest.json is missing")
    try:
        manifest = json.loads(manifest_path.read_text("utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        raise BundleError(f"Invalid bundle manifest: {exc}") from exc
    if manifest.get("schema_version") != SCHEMA_VERSION:
        raise BundleError("Unsupported bundle manifest schema")
    if manifest.get("bundle_name") != BUNDLE_NAME:
        raise BundleError("Unexpected bundle name")

    expected = {entry["path"]: entry for entry in manifest.get("files", [])}
    actual = {
        path.relative_to(bundle_root).as_posix(): path
        for path in bundle_root.rglob("*")
        if path.is_file() and path.name != "bundle-manifest.json"
    }
    missing_files = sorted(set(expected) - set(actual))
    unexpected_files = sorted(set(actual) - set(expected))
    mismatched = []
    for relative in sorted(set(expected) & set(actual)):
        path = actual[relative]
        record = expected[relative]
        if path.stat().st_size != record.get("size") or sha256_file(path) != record.get("sha256"):
            mismatched.append(relative)
    required_missing = [path for path in REQUIRED_PATHS if not (bundle_root / path).exists()]
    broken_links = audit_markdown_links(bundle_root)
    status = "PASS" if not any(
        (missing_files, unexpected_files, mismatched, required_missing, broken_links)
    ) else "FAIL"
    result = {
        "schema_version": SCHEMA_VERSION,
        "bundle_name": BUNDLE_NAME,
        "status": status,
        "file_count": len(actual),
        "missing_files": missing_files,
        "unexpected_files": unexpected_files,
        "hash_or_size_mismatches": mismatched,
        "required_paths_missing": required_missing,
        "broken_local_markdown_links": broken_links,
        "native_gis_execution": "NOT_RUN",
        "agent_client_installation": "NOT_RUN",
    }
    if status != "PASS":
        raise BundleError(json.dumps(result, ensure_ascii=False, indent=2))
    return result


def write_deterministic_zip(bundle_root: Path, archive_path: Path) -> None:
    with zipfile.ZipFile(
        archive_path,
        "w",
        compression=zipfile.ZIP_DEFLATED,
        compresslevel=9,
    ) as archive:
        for path in sorted(bundle_root.rglob("*"), key=lambda item: item.as_posix()):
            if not path.is_file():
                continue
            relative = path.relative_to(bundle_root).as_posix()
            info = zipfile.ZipInfo(f"{BUNDLE_NAME}/{relative}", FIXED_ZIP_TIME)
            info.compress_type = zipfile.ZIP_DEFLATED
            info.create_system = 3
            info.external_attr = 0o100644 << 16
            archive.writestr(info, path.read_bytes(), compresslevel=9)


def build(output_dir: Path, root: Path = ROOT) -> dict:
    output_dir = output_dir.resolve()
    bundle_root = output_dir / BUNDLE_NAME
    archive_path = output_dir / f"{BUNDLE_NAME}.zip"
    checksum_path = output_dir / f"{BUNDLE_NAME}.zip.sha256"
    existing = [path for path in (bundle_root, archive_path, checksum_path) if path.exists()]
    if existing:
        names = ", ".join(str(path) for path in existing)
        raise BundleError(f"Refusing to overwrite existing bundle output: {names}")
    if output_dir == root.resolve() or root.resolve() in output_dir.parents:
        pass
    elif output_dir in root.resolve().parents:
        raise BundleError("Output directory cannot contain the source repository")

    output_dir.mkdir(parents=True, exist_ok=True)
    bundle_root.mkdir()
    try:
        total = 0
        for relative, source in iter_source_files(root):
            destination = bundle_root / relative
            destination.parent.mkdir(parents=True, exist_ok=True)
            data = source.read_bytes()
            total += len(data)
            if total > MAX_BUNDLE_BYTES:
                raise BundleError(
                    f"Bundle exceeds {MAX_BUNDLE_BYTES} byte expanded-content limit"
                )
            destination.write_bytes(data)
        (bundle_root / "SKILL.md").write_text(
            root_skill_text(), encoding="utf-8", newline="\n"
        )
        write_manifest(bundle_root, source_commit(root))
        audit = audit_directory(bundle_root)
        write_deterministic_zip(bundle_root, archive_path)
        archive_hash = sha256_file(archive_path)
        checksum_path.write_text(
            f"{archive_hash}  {archive_path.name}\n", encoding="ascii", newline="\n"
        )
    except Exception:
        if bundle_root.exists():
            shutil.rmtree(bundle_root)
        archive_path.unlink(missing_ok=True)
        checksum_path.unlink(missing_ok=True)
        raise
    return {
        "status": "PASS",
        "bundle_directory": str(bundle_root),
        "archive": str(archive_path),
        "archive_sha256": archive_hash,
        "checksum_file": str(checksum_path),
        "audit": audit,
    }


def audit_path(path: Path) -> dict:
    path = path.resolve()
    if path.is_dir():
        return audit_directory(path)
    if path.suffix.casefold() != ".zip" or not path.is_file():
        raise BundleError("--check expects a bundle directory or .zip archive")
    # Extract beside the archive so hosts that scope writable temporary paths
    # to the authorized workspace do not need a broader OS-temp permission.
    with tempfile.TemporaryDirectory(
        prefix="gis-skill-bundle-audit-", dir=path.parent
    ) as temporary:
        temporary_root = Path(temporary)
        with zipfile.ZipFile(path, "r") as archive:
            members = archive.infolist()
            for member in members:
                member_path = Path(member.filename)
                if member.is_dir():
                    continue
                if member_path.is_absolute() or ".." in member_path.parts:
                    raise BundleError(f"Unsafe archive member: {member.filename}")
            archive.extractall(temporary_root)
        roots = [item for item in temporary_root.iterdir() if item.is_dir()]
        if len(roots) != 1 or roots[0].name != BUNDLE_NAME:
            raise BundleError("Archive must contain one gis-development-suite root")
        return audit_directory(roots[0])


def parse_args(argv=None):
    parser = argparse.ArgumentParser(
        description="Build or audit the self-contained GIS Development Skills suite"
    )
    action = parser.add_mutually_exclusive_group(required=True)
    action.add_argument("--output-dir", type=Path, help="New parent directory for bundle outputs")
    action.add_argument("--check", type=Path, help="Existing bundle directory or ZIP to audit")
    parser.add_argument("--format", choices=("text", "json"), default="text")
    return parser.parse_args(argv)


def main(argv=None) -> int:
    args = parse_args(argv)
    try:
        result = build(args.output_dir) if args.output_dir else audit_path(args.check)
    except (BundleError, OSError, zipfile.BadZipFile) as exc:
        if args.format == "json":
            print(json.dumps({"status": "FAIL", "error": str(exc)}, ensure_ascii=False, indent=2))
        else:
            print(f"GIS skill bundle: FAIL — {exc}", file=sys.stderr)
        return 1
    if args.format == "json":
        print(json.dumps(result, ensure_ascii=False, indent=2))
    else:
        print(f"GIS skill bundle: {result['status']}")
        if "archive" in result:
            print(f"Archive: {result['archive']}")
            print(f"SHA-256: {result['archive_sha256']}")
        print("Native GIS execution: NOT RUN")
        print("Agent client installation: NOT RUN")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
