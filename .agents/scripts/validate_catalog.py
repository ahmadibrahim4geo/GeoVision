#!/usr/bin/env python3
"""Offline catalog structure checks (no network or third-party dependencies)."""
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CATALOG = ROOT / 'references' / 'project-catalog.json'
REQUIRED = ('name', 'url', 'category', 'role', 'platforms', 'description',
            'reviewed_on', 'license', 'integration')
URL_PATTERN = re.compile(r'^https://github\.com/([A-Za-z0-9_.-]+)/([A-Za-z0-9_.-]+)/?$')


def validate_catalog(path=CATALOG):
    errors = []
    try:
        data = json.loads(Path(path).read_text(encoding='utf-8'))
    except (OSError, ValueError) as exc:
        return ['Cannot read project catalog JSON: {}'.format(exc)]
    if not isinstance(data, dict):
        return ['Catalog root must be a JSON object']
    if data.get('schema_version') != '1.0':
        errors.append('Unsupported catalog schema_version')
    items = data.get('projects')
    if not isinstance(items, list):
        return ['projects must be an array']
    seen_urls = set()
    for i, entry in enumerate(items):
        tag = 'projects[{}]'.format(i)
        if not isinstance(entry, dict):
            errors.append('{} must be object'.format(tag))
            continue
        for field in REQUIRED:
            if field not in entry:
                errors.append('{} missing {}'.format(tag, field))
        url = entry.get('url', '')
        if not isinstance(url, str) or not URL_PATTERN.match(url):
            errors.append('{} invalid canonical GitHub URL'.format(tag))
        elif url.rstrip('/').casefold() in seen_urls:
            errors.append('{} duplicate URL'.format(tag))
        if isinstance(url, str):
            seen_urls.add(url.rstrip('/').casefold())
        if not isinstance(entry.get('platforms'), list) or not entry['platforms'] or not all(isinstance(x,str) and x for x in entry['platforms']):
            errors.append('{} nonempty platforms string array required'.format(tag))
        for field in ('name','category','description','role','license','integration','reviewed_on'):
            if not isinstance(entry.get(field), str) or not entry[field].strip():
                errors.append('{} invalid {}'.format(tag,field))
        # Newly curated source-code entries are links, not build/license certification.
        checked = entry.get('repository_access_checked_on')
        if checked is not None:
            if not isinstance(checked, str) or not re.fullmatch(r'\d{4}-\d{2}-\d{2}', checked):
                errors.append('{} invalid repository_access_checked_on date'.format(tag))
            skill = entry.get('primary_skill')
            if not isinstance(skill, str) or not re.fullmatch(r'[a-z0-9]+(?:-[a-z0-9]+)*', skill):
                errors.append('{} invalid primary_skill'.format(tag))
            elif not (ROOT / 'skills' / skill / 'SKILL.md').is_file():
                errors.append('{} missing primary_skill specialist'.format(tag))
            if not isinstance(entry.get('license_metadata_unverified'), str) or not entry['license_metadata_unverified'].strip():
                errors.append('{} missing preliminary license metadata'.format(tag))
            if entry.get('integration') == 'reference-only' and entry.get('native_runtime_verified') is True:
                errors.append('{} reference-only cannot claim native runtime verified'.format(tag))
            if entry.get('tested_by_repository') is not False:
                errors.append('{} source link cannot claim tested_by_repository'.format(tag))
        flag=entry.get('license_verified')
        if flag is not None and not isinstance(flag,bool):
            errors.append('{} invalid license_verified boolean'.format(tag))
        if flag is True:
            for attr in ('license_scope','license_checked_on','license_evidence_url','license_caveat'):
                if not isinstance(entry.get(attr),str) or not entry[attr].strip():
                    errors.append('{} verified license requires {}'.format(tag,attr))
            evidence=entry.get('license_evidence_url')
            if not isinstance(evidence,str) or not evidence.startswith('https://github.com/'):
                errors.append('{} invalid upstream license evidence URL'.format(tag))
    return errors


if __name__ == '__main__':
    issues = validate_catalog()
    if issues:
        print('Catalog invalid:')
        for issue in issues:
            print(' - ' + issue)
        raise SystemExit(1)
    print('Catalog valid: {} sources'.format(len(json.loads(CATALOG.read_text(encoding='utf-8'))['projects'])))