package com.geovision.esri.viewer

import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File

class EsriReadOnlyPolicyTest {
    @Test
    fun source_doesNotUseForbiddenEditingOrSyncApis() {
        val sourceRoot = File("src/main/java/com/geovision/esri/viewer")
        val source = sourceRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .joinToString("\n") { it.readText() }

        val forbiddenTokens = listOf(
            "addFeature(",
            "addFeatures(",
            "updateFeature(",
            "updateFeatures(",
            "deleteFeature(",
            "deleteFeatures(",
            "SyncGeodatabase",
            "GenerateGeodatabase",
            "OfflineMapTask",
            "RouteTask",
            "LocatorTask(",
            "GeometryEditor("
        )

        forbiddenTokens.forEach { token ->
            assertFalse("Forbidden Esri API token found: $token", source.contains(token))
        }
    }
}
