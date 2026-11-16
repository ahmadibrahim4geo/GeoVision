#!/usr/bin/env python3
"""Offline validation of installable Agent Skills, local links and site counters.

No third-party packages and no network access. Does not certify GIS host runtimes.
"""
import argparse
import json
import re
from html.parser import HTMLParser
from pathlib import Path
from urllib.parse import unquote, urlsplit

ROOT = Path(__file__).resolve().parents[1]
SKILL_NAME = re.compile(r"^[a-z0-9]+(?:-[a-z0-9]+)*$")
MD_LINK = re.compile(r"(?<!!)\[[^\]]+\]\(([^)]+)\)")
CODE_PATH = re.compile(r"`(\.\.?/[^\s`]+)`")
FIELD = re.compile(r"^([a-z][a-z0-9-]*):\s*(.*)$")


class LocalLinks(HTMLParser):
    def __init__(self):
        super().__init__()
        self.refs = []

    def handle_starttag(self, tag, attrs):
        attributes = dict(attrs)
        for name in ("src", "href"):
            if name in attributes and tag in ("img", "script", "link", "a"):
                self.refs.append(attributes[name])


def local_target(src, referring_file, root):
    raw = src.strip().strip("<>").strip("'\"")
    if not raw or raw.startswith("#") or raw.startswith(("http://", "https://", "mailto:", "data:")):
        return None
    if raw.startswith(("javascript:", "//")):
        raise ValueError("Disallowed inline/browser reference: {}".format(raw))
    path = unquote(urlsplit(raw).path)
    if not path or path.startswith("/"):
        return None
    dest = (referring_file.parent / path).resolve()
    if not dest.is_relative_to(root.resolve()):
        raise ValueError("Local reference escapes repository: {}".format(raw))
    return dest


def validate_frontmatter(skill_path):
    errors = []
    text = skill_path.read_text(encoding="utf-8-sig")
    lines = text.splitlines()
    if len(lines) < 5 or lines[0] != "---":
        return ["{}: missing leading YAML frontmatter".format(skill_path)]
    try:
        end = lines.index("---", 1)
    except ValueError:
        return ["{}: frontmatter closing --- missing".format(skill_path)]
    fields = {}
    for line in lines[1:end]:
        if not line.strip() or line.startswith((" ", "#")):
            continue
        match = FIELD.match(line)
        if not match:
            errors.append("{}: unsupported frontmatter line {!r}".format(skill_path, line))
            continue
        key, value = match.groups()
        if key in fields:
            errors.append("{}: duplicate key {}".format(skill_path, key))
        fields[key] = value.strip().strip("'\"")
    name = fields.get("name", "")
    if not SKILL_NAME.fullmatch(name) or len(name) > 64 or name != skill_path.parent.name:
        errors.append("{}: invalid name / directory mismatch".format(skill_path))
    description = fields.get("description", "")
    if not description or len(description) > 1024:
        errors.append("{}: description must be 1-1024 characters".format(skill_path))
    if not "".join(lines[end + 1:]).strip():
        errors.append("{}: skill instructions are empty".format(skill_path))
    if len(fields.get("compatibility", "")) > 500:
        errors.append("{}: compatibility exceeds 500 characters".format(skill_path))
    return errors


def check_links(path, root, include_code=False):
    errors = []
    content = path.read_text(encoding="utf-8")
    refs = MD_LINK.findall(content)
    if include_code:
        refs.extend(CODE_PATH.findall(content))
    for ref in refs:
        try:
            target = local_target(ref, path, root)
            if target is not None and not target.exists():
                errors.append("{}: missing local target {}".format(path.relative_to(root), ref))
        except ValueError as exc:
            errors.append("{}: {}".format(path.relative_to(root), exc))
    return errors


def inspect_repository(root=ROOT):
    root = Path(root).resolve()
    errors = []
    skills_dir = root / "skills"
    skill_files = sorted(skills_dir.glob("*/SKILL.md"))
    if not skill_files:
        return ["No installable skills found"]
    for skill in skill_files:
        errors.extend(validate_frontmatter(skill))
        errors.extend(check_links(skill, root, include_code=True))
    for markdown in [root / "README.md", root / "README.ar.md"]:
        if markdown.is_file():
            errors.extend(check_links(markdown, root))
    for guide in (root / "references").glob("*.md"):
        errors.extend(check_links(guide, root))
    html = root / "explorer.html"
    if html.is_file():
        page = html.read_text(encoding="utf-8")
        parser = LocalLinks()
        parser.feed(page)
        for ref in parser.refs:
            try:
                target = local_target(ref, html, root)
                if target is not None and not target.exists():
                    errors.append("index.html: missing local target {}".format(ref))
            except ValueError as exc:
                errors.append("index.html: {}".format(exc))
        sources = root / "references" / "project-catalog.json"
        apis = root / "references" / "free-geospatial-apis.json"
        if sources.is_file():
            expected = len(json.loads(sources.read_text(encoding="utf-8"))["projects"])
            actual = re.search(r'id="projectsCount">([0-9]+)<', page)
            if actual is None or int(actual.group(1)) != expected:
                errors.append("index.html: project counter differs from catalog ({})".format(expected))
        if apis.is_file():
            expected = len(json.loads(apis.read_text(encoding="utf-8"))["services"])
            actual = re.search(r'id="apisCount">([0-9]+)<', page)
            if actual is None or int(actual.group(1)) != expected:
                errors.append("index.html: API counter differs from catalog ({})".format(expected))
        shown = re.search(r'<div class="stat"><strong>([0-9]+)</strong><span data-i18n="statSkill">', page)
        if shown is None or int(shown.group(1)) != len(skill_files):
            errors.append("index.html: skill counter differs from discoverable skills ({})".format(len(skill_files)))

    # Interactive homepage is a separate entry point. Validate its local
    # assets and links without weakening the complete Explorer inventory checks.
    homepage = root / "index.html"
    if homepage.is_file():
        home_parser = LocalLinks()
        home_parser.feed(homepage.read_text(encoding="utf-8"))
        for ref in home_parser.refs:
            try:
                target = local_target(ref, homepage, root)
                if target is not None and not target.exists():
                    errors.append("index.html: missing local target {}".format(ref))
            except ValueError as exc:
                errors.append("index.html: {}".format(exc))
    return errors


def main(argv=None):
    cli = argparse.ArgumentParser(description=__doc__)
    cli.add_argument("--root", type=Path, default=ROOT)
    args = cli.parse_args(argv)
    issues = inspect_repository(args.root)
    if issues:
        for issue in issues:
            print("ERROR:", issue)
        return 1
    print("Skill pack valid: {} skills; README, reference and website paths checked".format(
        len(list((args.root / "skills").glob("*/SKILL.md")))))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
