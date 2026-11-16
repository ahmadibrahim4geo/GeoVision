#!/usr/bin/env python3
"""Read-only ZIP/KMZ/Shapefile archive safety preflight (Python 3 standard library).

This guards archive *structure*, not member payload semantics. It never extracts.
"""
import argparse
import json
import stat
import zipfile
from pathlib import Path, PurePosixPath


def inspect_archive(path, *, max_files=1000, max_bytes=268435456,
                    max_member_bytes=134217728, max_ratio=200):
    """Return a safe, redacted decision report; no extraction or writes."""
    path = Path(path)
    findings = []
    total = 0
    count = 0
    if min(max_files, max_bytes, max_member_bytes, max_ratio) <= 0:
        raise ValueError("limits must be positive")
    try:
        with zipfile.ZipFile(path, "r", allowZip64=True) as archive:
            members = archive.infolist()
            count = len(members)
            if count > max_files:
                findings.append("member-count-exceeded")
            seen = set()
            for info in members:
                raw = getattr(info, "orig_filename", info.filename)
                name = raw.replace("\\", "/")
                parts = PurePosixPath(name).parts
                if (
                    not raw or "\\" in raw or raw.startswith(("/", "\\"))
                    or "//" in raw
                    or (":" in parts[0] if parts else True)
                ):
                    findings.append("unsafe-path")
                if any(part in ("..", ".") for part in name.split("/")):
                    findings.append("path-traversal")
                if name in seen:
                    findings.append("duplicate-member")
                seen.add(name)
                mode = (info.external_attr >> 16) & 0xFFFF
                if stat.S_ISLNK(mode):
                    findings.append("symlink-member")
                if info.flag_bits & 1:
                    findings.append("encrypted-member")
                if info.file_size < 0 or info.compress_size < 0:
                    findings.append("invalid-size")
                if info.file_size > max_member_bytes:
                    findings.append("member-size-exceeded")
                if info.file_size > max_ratio * max(1, info.compress_size):
                    findings.append("compression-ratio-exceeded")
                total += info.file_size
                if total > max_bytes:
                    findings.append("expanded-size-exceeded")
                    break
    except (OSError, zipfile.BadZipFile, zipfile.LargeZipFile, ValueError) as exc:
        findings.append("invalid-or-unreadable-archive")
    return {
        "status": "pass" if not findings else "fail",
        "archive": path.name,
        "members_checked": count,
        "expanded_bytes_observed": total,
        "issues": sorted(set(findings)),
        "limits": {
            "max_files": max_files, "max_bytes": max_bytes,
            "max_member_bytes": max_member_bytes, "max_ratio": max_ratio,
        },
        "note": "Metadata/path screening only; payloads not parsed or extracted.",
    }


def main(argv=None):
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("archive", type=Path)
    p.add_argument("--max-files", type=int, default=1000)
    p.add_argument("--max-bytes", type=int, default=268435456)
    p.add_argument("--max-member-bytes", type=int, default=134217728)
    p.add_argument("--max-ratio", type=int, default=200)
    args = p.parse_args(argv)
    result = inspect_archive(args.archive, max_files=args.max_files,
                             max_bytes=args.max_bytes,
                             max_member_bytes=args.max_member_bytes,
                             max_ratio=args.max_ratio)
    print(json.dumps(result, indent=2))
    return 0 if result["status"] == "pass" else 1


if __name__ == "__main__":
    raise SystemExit(main())
