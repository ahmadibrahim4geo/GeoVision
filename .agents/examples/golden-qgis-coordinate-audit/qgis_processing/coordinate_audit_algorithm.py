"""QGIS Processing adapter for the deterministic coordinate-audit core.

Native status is NOT RUN until this algorithm is loaded and executed inside a
named QGIS runtime.  Importing or compiling this file is only T0/T1 evidence.
"""

from pathlib import Path
import sys

from qgis.core import (
    QgsProcessingAlgorithm,
    QgsProcessingException,
    QgsProcessingOutputFile,
    QgsProcessingParameterFile,
    QgsProcessingParameterString,
)

EXAMPLE_ROOT = Path(__file__).resolve().parents[1]
if str(EXAMPLE_ROOT) not in sys.path:
    sys.path.insert(0, str(EXAMPLE_ROOT))

from audit_core import AuditError, run_audit  # noqa: E402


class CoordinateAuditAlgorithm(QgsProcessingAlgorithm):
    INPUT = "INPUT"
    OUTPUT_DIRECTORY = "OUTPUT_DIRECTORY"
    ARTIFACT = "ARTIFACT"
    SUMMARY = "SUMMARY"

    def name(self):
        return "verified_coordinate_audit"

    def displayName(self):
        return "Verified coordinate audit"

    def group(self):
        return "Verification examples"

    def groupId(self):
        return "verification_examples"

    def shortHelpString(self):
        return (
            "Audits an explicitly declared EPSG:4326 GeoJSON Point collection. "
            "The source is preserved and the output directory must be new."
        )

    def createInstance(self):
        return CoordinateAuditAlgorithm()

    def initAlgorithm(self, config=None):
        self.addParameter(
            QgsProcessingParameterFile(
                self.INPUT,
                "EPSG:4326 GeoJSON FeatureCollection",
                behavior=QgsProcessingParameterFile.File,
                fileFilter="GeoJSON (*.geojson *.json)",
            )
        )
        self.addParameter(
            QgsProcessingParameterString(
                self.OUTPUT_DIRECTORY,
                "New output directory",
            )
        )
        self.addOutput(QgsProcessingOutputFile(self.ARTIFACT, "Audited GeoJSON"))
        self.addOutput(QgsProcessingOutputFile(self.SUMMARY, "Audit summary"))

    def processAlgorithm(self, parameters, context, feedback):
        input_path = self.parameterAsFile(parameters, self.INPUT, context)
        output_directory = self.parameterAsString(parameters, self.OUTPUT_DIRECTORY, context)
        try:
            summary = run_audit(input_path, output_directory)
        except AuditError as exc:
            raise QgsProcessingException(str(exc)) from exc
        feedback.pushInfo(
            "Accepted {} and rejected {} features".format(
                summary["accepted_feature_count"], summary["rejected_feature_count"]
            )
        )
        output = Path(output_directory)
        return {
            self.ARTIFACT: str(output / "audited_points.geojson"),
            self.SUMMARY: str(output / "summary.json"),
        }
