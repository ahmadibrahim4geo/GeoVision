#!/usr/bin/env python3
"""ArcGIS Pro layout-to-PDF batch export. Requires actual ArcGIS Pro / arcpy."""
import argparse
import re
from pathlib import Path


def safe_file_stem(label):
    return re.sub(r"[^a-zA-Z0-9_.-]+", "_", label).strip("._") or "layout"


def export(project, folder, dpi=200, overwrite=False, dry_run=False):
    import arcpy  # installed ArcGIS Pro Python environment only
    aprx = arcpy.mp.ArcGISProject(str(project))
    layouts = aprx.listLayouts()
    if not layouts:
        raise ValueError("No layouts in the ArcGIS Pro project")
    targets = []
    reserved = set()
    for i, layout in enumerate(layouts, 1):
        stem = safe_file_stem(layout.name)
        filename = stem + ".pdf"
        if filename.lower() in reserved:
            filename = "{}_{}.pdf".format(stem, i)
        reserved.add(filename.lower())
        destination = folder / filename
        if destination.exists() and not overwrite:
            raise FileExistsError("Refusing to overwrite {}".format(destination))
        targets.append((layout, destination))
    if not dry_run:
        folder.mkdir(parents=True, exist_ok=True)
        for layout, destination in targets:
            # Verify this method against the installed ArcGIS Pro version;
            # newer versions may prefer Layout.export(PDFFormat).
            layout.exportToPDF(str(destination), resolution=dpi)
    return [str(destination) for _, destination in targets]


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--project", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--dpi", type=int, default=200)
    parser.add_argument("--overwrite", action="store_true")
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args(argv)
    if not args.project.is_file() or args.project.suffix.lower() != ".aprx":
        parser.error("Pass an existing .aprx file")
    if not 72 <= args.dpi <= 600:
        parser.error("dpi must be 72–600")
    for target in export(args.project, args.output, args.dpi, args.overwrite, args.dry_run):
        print(("PLAN: " if args.dry_run else "EXPORTED: ") + target)


if __name__ == "__main__":
    main()
