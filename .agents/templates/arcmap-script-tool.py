# -*- coding: utf-8 -*-
"""ArcMap 10.x Python 2.7 compatible illustrative Script Tool.

In ArcToolbox configure two parameters: GPFeatureLayer (Input) and DEFeatureClass (Output).
Test on a supported legacy ArcMap environment; do not run with modern interpreter alone.
"""
import arcpy


def main():
    input_features = arcpy.GetParameterAsText(0)
    output_features = arcpy.GetParameterAsText(1)
    if not input_features or not output_features:
        raise ValueError("Input and output are required")
    if input_features == output_features:
        raise ValueError("Cannot overwrite source")
    if arcpy.Exists(output_features):
        raise ValueError("Output already exists")
    arcpy.CopyFeatures_management(input_features, output_features)
    arcpy.AddMessage("Copied features to: {0}".format(output_features))


if __name__ == '__main__':
    main()