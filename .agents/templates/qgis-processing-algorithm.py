"""Illustrative QGIS Processing algorithm class; not a packaged plugin.

Load/register using a QGIS plugin provider in the installed QGIS Python environment.
Uses native:fixgeometries as a child algorithm; verify behavior in your QGIS version.
"""
from qgis.core import (
    QgsProcessingAlgorithm,
    QgsProcessingParameterFeatureSource,
    QgsProcessingParameterFeatureSink,
)
import processing


class FixGeometriesExample(QgsProcessingAlgorithm):
    INPUT = 'INPUT'
    OUTPUT = 'OUTPUT'

    def name(self):
        return 'fix_geometries_example'

    def displayName(self):
        return 'Fix Geometries (Example)'

    def group(self):
        return 'Examples'

    def groupId(self):
        return 'examples'

    def shortHelpString(self):
        return 'Writes a separate output. Uses QGIS native:fixgeometries.'

    def initAlgorithm(self, config=None):
        self.addParameter(QgsProcessingParameterFeatureSource(self.INPUT, 'Input layer'))
        self.addParameter(QgsProcessingParameterFeatureSink(self.OUTPUT, 'Fixed features'))

    def processAlgorithm(self, parameters, context, feedback):
        result = processing.run(
            'native:fixgeometries',
            {'INPUT': parameters[self.INPUT], 'OUTPUT': parameters[self.OUTPUT]},
            context=context, feedback=feedback, is_child_algorithm=True)
        return {self.OUTPUT: result['OUTPUT']}

    def createInstance(self):
        return FixGeometriesExample()