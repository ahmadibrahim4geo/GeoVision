#!/usr/bin/env python3
"""Search the reviewed GIS projects catalog entirely offline."""
import argparse
import json
from pathlib import Path

CATALOG = Path(__file__).resolve().parents[1] / 'references' / 'project-catalog.json'


def search(items, query='', platform='', category=''):
    q = query.casefold().strip()
    out = []
    for x in items:
        if platform and platform.casefold() not in [v.casefold() for v in x['platforms']]:
            continue
        if category and category.casefold() != x['category'].casefold():
            continue
        haystack = ' '.join([x['name'], x['description'], x['category'], x['role'], ' '.join(x['platforms'])]).casefold()
        if q and q not in haystack:
            continue
        out.append(x)
    return sorted(out, key=lambda x: x['name'].casefold())


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--query', default='', help='Case-insensitive text substring')
    p.add_argument('--platform', default='', help='e.g. arcgis-pro, arcmap, qgis, desktop, rust, 3d')
    p.add_argument('--category', default='', help='e.g. arcgis, qgis, rust, spatial-core')
    p.add_argument('--json', action='store_true', help='Emit JSON instead of human-readable lines')
    args = p.parse_args()
    catalog = json.loads(CATALOG.read_text(encoding='utf-8'))
    result = search(catalog['projects'], args.query, args.platform, args.category)
    if args.json:
        print(json.dumps(result, indent=2, ensure_ascii=False))
    else:
        print('Found {} sources'.format(len(result)))
        for x in result:
            print('- {} [{}]: {}'.format(x['name'], x['category'], x['url']))
            print('  ' + x['description'])