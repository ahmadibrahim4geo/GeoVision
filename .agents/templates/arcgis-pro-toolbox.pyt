# -*- coding: utf-8 -*-
"""Illustrative ArcGIS Pro Python Toolbox. Run in a licensed ArcGIS Pro environment.

Creates a NEW feature class. Does not overwrite the source layer.
"""
import arcpy


class Toolbox(object):
    def __init__(self):
        self.label = "GIS Development Sample Tools"
        self.alias = "gisdevsample"
        self.tools = [CopyFeaturesExample]


class CopyFeaturesExample(object):
    def __init__(self):
        self.label = "Copy Features (Example)"
        self.description = "Copy features to a new destination; do not alter the source."
        self.canRunInBackground = False

    def getParameterInfo(self):
        in_features = arcpy.Parameter(
            displayName="Input Features", name="in_features",
            datatype="GPFeatureLayer", parameterType="Required", direction="Input")
        out_features = arcpy.Parameter(
            displayName="Output Feature Class", name="out_features",
            datatype="DEFeatureClass", parameterType="Required", direction="Output")
        return [in_features, out_features]

    def isLicensed(self):
        return True

    def updateParameters(self, parameters):
        return

    def updateMessages(self, parameters):
        if parameters[0].valueAsText and parameters[1].valueAsText:
            if parameters[0].valueAsText == parameters[1].valueAsText:
                parameters[1].setErrorMessage("Source and output cannot be the same path.")

    def execute(self, parameters, messages):
        src = parameters[0].valueAsText
        dst = parameters[1].valueAsText
        if not src or not dst:
            raise arcpy.ExecuteError("Both input and output are required.")
        if src == dst:
            raise arcpy.ExecuteError("Refusing to overwrite the source.")
        if arcpy.Exists(dst):
            raise arcpy.ExecuteError("Output already exists. Choose a new output path.")
        arcpy.management.CopyFeatures(src, dst)
        arcpy.AddMessage("Created: {}".format(dst))