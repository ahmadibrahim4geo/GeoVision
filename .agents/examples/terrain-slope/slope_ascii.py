#!/usr/bin/env python3
"""Compute Horn 3x3 slope from a small projected ESRI ASCII DEM.

Free, dependency-free educational reference. Not a replacement for GDAL/GRASS
on large grids or terrain with geographic degrees / nonuniform grid spacing.
"""
import argparse
import json
import math
from pathlib import Path

MAX_CELLS = 2_000_000
UNIT_M = {"m": 1.0, "ft": 0.3048}
NODATA_OUTPUT = -9999.0


def read_grid(path):
    """Parse a simple six-line ESRI ASCII grid, requiring explicit NoData."""
    with Path(path).open("r", encoding="utf-8-sig") as handle:
        header = {}
        for _ in range(6):
            line = handle.readline()
            parts = line.split()
            if len(parts) != 2:
                raise ValueError("Expected six ESRI ASCII Grid header lines")
            key = parts[0].casefold()
            if key in header:
                raise ValueError("Duplicate ASCII grid header key: " + key)
            header[key] = float(parts[1])
        for required in ("ncols", "nrows", "cellsize", "nodata_value"):
            if required not in header:
                raise ValueError("Missing header: " + required)
        for x in ("xllcorner", "xllcenter"):
            if x in header:
                xkey = x
                break
        else:
            raise ValueError("Missing xllcorner or xllcenter")
        for y in ("yllcorner", "yllcenter"):
            if y in header:
                ykey = y
                break
        else:
            raise ValueError("Missing yllcorner or yllcenter")
        if ("center" in xkey) != ("center" in ykey):
            raise ValueError("X/Y origin definitions must use matching corner or center")
        if not all(math.isfinite(header[k]) for k in header):
            raise ValueError("Non-finite grid metadata")
        cols, rows = header["ncols"], header["nrows"]
        if cols != int(cols) or rows != int(rows) or cols < 3 or rows < 3:
            raise ValueError("Slope requires >=3 by >=3 integer grid dimensions")
        cols, rows = int(cols), int(rows)
        if cols * rows > MAX_CELLS:
            raise ValueError("Example limit exceeded: use tiled GDAL/GRASS for large rasters")
        if header["cellsize"] <= 0:
            raise ValueError("cellsize must be positive")
        values = []
        for r in range(rows):
            line = handle.readline()
            tokens = line.split()
            if len(tokens) != cols:
                raise ValueError("Row {} has {} values, expected {}".format(r, len(tokens), cols))
            row = [float(x) for x in tokens]
            if any(not math.isfinite(x) for x in row):
                raise ValueError("Nonfinite sample in row {}".format(r))
            values.append(row)
        if handle.read().strip():
            raise ValueError("Unexpected data after last grid row")
    return header, xkey, ykey, values


def horn_slope(values, nodata, cell_size, horizontal_unit="m", vertical_unit="m"):
    """Horn 3x3 slope degrees; leave edges or windows touching NoData as NoData."""
    factor = UNIT_M[vertical_unit] / UNIT_M[horizontal_unit]
    if cell_size <= 0:
        raise ValueError("cell_size must be > 0")
    rows, cols = len(values), len(values[0])
    output = [[NODATA_OUTPUT] * cols for _ in range(rows)]
    for y in range(1, rows - 1):
        for x in range(1, cols - 1):
            block = [values[r][c] for r in (y-1,y,y+1) for c in (x-1,x,x+1)]
            if any(z == nodata for z in block):
                continue
            a,b,c,d,e,f,g,h,i = [z * factor for z in block]
            dx = ((c + 2*f + i) - (a + 2*d + g)) / (8 * cell_size)
            dy = ((g + 2*h + i) - (a + 2*b + c)) / (8 * cell_size)
            output[y][x] = math.degrees(math.atan(math.hypot(dx, dy)))
    return output


def write_result(input_path, output_path, header, xkey, ykey, data, horizontal_unit, vertical_unit):
    output_path = Path(output_path)
    if output_path.resolve() == Path(input_path).resolve():
        raise ValueError("Output path cannot be input path")
    metadata_path = output_path.with_name(output_path.name + ".provenance.json")
    if output_path.exists() or metadata_path.exists():
        raise FileExistsError("Output or provenance already exists; never overwrite input/results")
    output_path.parent.mkdir(parents=True, exist_ok=True)
    ncols, nrows = int(header["ncols"]), int(header["nrows"])
    contents = [
        "ncols {}".format(ncols),
        "nrows {}".format(nrows),
        "{} {}".format(xkey, header[xkey]),
        "{} {}".format(ykey, header[ykey]),
        "cellsize {}".format(header["cellsize"]),
        "NODATA_value -9999",
    ]
    contents.extend(" ".join("-9999" if z == NODATA_OUTPUT else "{:.8f}".format(z) for z in row) for row in data)
    metadata = {
        "algorithm": "Horn 3x3 finite difference slope in degrees",
        "input": str(Path(input_path).resolve()),
        "output": str(output_path.resolve()),
        "horizontal_unit": horizontal_unit,
        "vertical_unit": vertical_unit,
        "projected_grid_accepted_by_user": True,
        "crs_validated_by_tool": False,
        "edge_behavior": "1-cell NoData border; interior touching input NoData is NoData",
        "source_nodata_value": header["nodata_value"],
        "output_nodata_value": NODATA_OUTPUT,
        "valid_slope_cells": sum(z != NODATA_OUTPUT for row in data for z in row),
        "warning": "This example cannot infer/verify CRS or vertical datum from ASCII Grid; verify projected coordinates independently.",
    }
    output_path.write_text("\n".join(contents) + "\n", encoding="utf-8")
    metadata_path.write_text(json.dumps(metadata, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    return metadata


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--horizontal-unit", choices=("m","ft"), required=True)
    parser.add_argument("--vertical-unit", choices=("m","ft"), required=True)
    parser.add_argument("--ack-projected", action="store_true", required=True,
                        help="Confirm the input cellsize is in projected linear units, NOT geographic degrees")
    args = parser.parse_args(argv)
    if not args.ack_projected:
        parser.error("--ack-projected is required; geographic-degree grids need reprojection first")
    try:
        header, xkey, ykey, values = read_grid(args.input)
        slope = horn_slope(values, header["nodata_value"], header["cellsize"],
                           args.horizontal_unit, args.vertical_unit)
        metadata = write_result(args.input,args.output,header,xkey,ykey,slope,
                                args.horizontal_unit,args.vertical_unit)
    except (OSError, ValueError, OverflowError) as exc:
        parser.error(str(exc))
    print("Created {} ({} computed cells); provenance recorded".format(args.output,metadata["valid_slope_cells"]))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
