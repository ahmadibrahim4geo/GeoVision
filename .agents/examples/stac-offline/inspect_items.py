#!/usr/bin/env python3
"""Offline STAC item metadata preview using synthetic fixtures; never download assets."""
import argparse
import datetime as dt
import json
import math
from pathlib import Path


def bbox_filter(bbox):
    """West,south,east,north degrees; antimeridian crossings not supported here."""
    if len(bbox) != 4 or any(not math.isfinite(v) for v in bbox):
        raise ValueError("bbox must have four finite coordinates")
    w,s,e,n = bbox
    if not (-180 <= w < e <= 180 and -90 <= s < n <= 90):
        raise ValueError("Invalid lon/lat bbox; antimeridian crossing is unsupported")
    return (w,s,e,n)


def overlaps(a,b):
    return a[0] <= b[2] and a[2] >= b[0] and a[1] <= b[3] and a[3] >= b[1]


def utc_time(value):
    if not isinstance(value,str):
        raise ValueError("STAC datetime must be ISO-8601 string")
    try:
        x=dt.datetime.fromisoformat(value.replace("Z","+00:00"))
    except ValueError as exc:
        raise ValueError("Invalid ISO-8601 UTC datetime") from exc
    if x.tzinfo is None:
        raise ValueError("Naive STAC datetimes are not accepted")
    return x.astimezone(dt.timezone.utc)


def inspect(document, search_bbox=None, since=None, until=None, max_cloud=None):
    if document.get("type")!="FeatureCollection" or not isinstance(document.get("features"),list):
        raise ValueError("Expected STAC ItemCollection-like FeatureCollection")
    result=[]
    ids=set()
    for idx, item in enumerate(document["features"]):
        if not isinstance(item,dict) or item.get("type")!="Feature":
            raise ValueError("Item {} is not a Feature".format(idx))
        ident=item.get("id")
        if not isinstance(ident,str) or not ident or ident in ids:
            raise ValueError("Item has absent or duplicated identifier")
        ids.add(ident)
        bbox=bbox_filter(item.get("bbox",[]))
        props=item.get("properties",{})
        when=utc_time(props.get("datetime"))
        cloud=props.get("eo:cloud_cover")
        if cloud is not None:
            if not isinstance(cloud,(int,float)) or not math.isfinite(cloud) or not 0<=cloud<=100:
                raise ValueError("Invalid cloud cover percentage")
        if search_bbox and not overlaps(bbox,search_bbox):
            continue
        if since and when<since:
            continue
        if until and when>until:
            continue
        if max_cloud is not None and (cloud is None or cloud>max_cloud):
            continue
        assets=item.get("assets")
        if not isinstance(assets,dict):
            raise ValueError("STAC assets must be an object")
        result.append({"id":ident,"datetime_utc":when.isoformat(),"cloud_cover_percent":cloud,
                       "bbox":bbox,"asset_keys":sorted(assets.keys())})
    return result


def main(argv=None):
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input",type=Path,required=True)
    parser.add_argument("--bbox",help="west,south,east,north EPSG:4326; no crossing antimeridian")
    parser.add_argument("--after",help="inclusive UTC ISO-8601 datetime")
    parser.add_argument("--before",help="inclusive UTC ISO-8601 datetime")
    parser.add_argument("--max-cloud",type=float)
    args=parser.parse_args(argv)
    try:
        search_bbox=bbox_filter([float(x) for x in args.bbox.split(",")]) if args.bbox else None
        after=utc_time(args.after) if args.after else None
        before=utc_time(args.before) if args.before else None
        if after and before and after>before:
            raise ValueError("after must not exceed before")
        if args.max_cloud is not None and not 0<=args.max_cloud<=100:
            raise ValueError("max-cloud must be 0-100")
        matches=inspect(json.loads(args.input.read_text(encoding="utf-8")),search_bbox,
                        after,before,args.max_cloud)
    except (OSError,ValueError,KeyError,TypeError) as exc:
        parser.error(str(exc))
    print(json.dumps({"count":len(matches),"items":matches,
                      "warning":"Bounding boxes are only coarse spatial filters; STAC assets not downloaded or verified."},
                     indent=2))
    return 0


if __name__=="__main__":
    raise SystemExit(main())
