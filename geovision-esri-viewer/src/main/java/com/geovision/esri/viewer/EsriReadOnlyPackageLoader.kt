package com.geovision.esri.viewer

import android.content.Context
import android.util.Log
import com.arcgismaps.data.Geodatabase
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.MobileMapPackage
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.layers.Layer
import java.io.File

class EsriReadOnlyPackageLoader {
    data class LoadedPackage(
        val report: EsriPackageReport,
        val maps: List<ArcGISMap> = emptyList(),
        val layers: List<Layer> = emptyList()
    )

    suspend fun open(context: Context, file: File, licenseString: String? = null): LoadedPackage {
        EsriLicenseGuard.configure(context)
        EsriLicenseGuard.applyLiteLicense(licenseString)
        require(EsriLicenseGuard.requireAllowed(EsriLicenseGuard.Capability.READ_ONLY_LOCAL_VIEW)) {
            "Esri read-only local viewing is disabled"
        }

        return when (detectType(file)) {
            EsriPackageType.MMPK -> openMobileMapPackage(file)
            EsriPackageType.MOBILE_GEODATABASE -> openMobileGeodatabase(file)
            EsriPackageType.UNSUPPORTED -> LoadedPackage(
                EsriPackageReport(
                    packageName = file.name,
                    packageType = EsriPackageType.UNSUPPORTED,
                    errors = listOf(
                        "Unsupported Esri package type. Use a single .geodatabase file here. FileGDB folders (.gdb) must be opened from the FileGDB button."
                    )
                )
            )
        }
    }

    private suspend fun openMobileMapPackage(file: File): LoadedPackage {
        val layers = mutableListOf<EsriLayerReport>()
        val maps = mutableListOf<ArcGISMap>()
        return try {
            val packageFile = MobileMapPackage(file.absolutePath)
            packageFile.load().onSuccess {
                maps.addAll(packageFile.maps)
                packageFile.maps.forEach { map ->
                    map.load().onFailure { error ->
                        Log.w(TAG, "MMPK map failed to load: ${error.message}")
                    }
                    map.utilityNetworks.forEach { utilityNetwork ->
                        layers.add(
                            skippedLayerReport(
                                name = utilityNetwork.name.ifBlank { "Utility Network" },
                                type = "UtilityNetwork",
                                message = "Unsupported in Esri Local Viewer read-only Lite mode"
                            )
                        )
                    }
                    map.transportationNetworks.forEach { network ->
                        layers.add(
                            skippedLayerReport(
                                name = network.name.ifBlank { "Transportation Network" },
                                type = "TransportationNetworkDataset",
                                message = "Routing and network analysis are disabled"
                            )
                        )
                    }
                    map.operationalLayers.forEach { layer ->
                        layers.add(loadReadOnlyLayer(layer))
                    }
                }
            }.onFailure { error ->
                return LoadedPackage(
                    EsriPackageReport(
                        packageName = file.name,
                        packageType = EsriPackageType.MMPK,
                        errors = listOfNotNull(error.message ?: error.toString())
                    )
                )
            }

            LoadedPackage(
                report = EsriPackageReport(
                    packageName = file.name,
                    packageType = EsriPackageType.MMPK,
                    packageVersion = packageFile.version,
                    totalMaps = maps.size,
                    totalLayers = layers.size,
                    layers = layers,
                    warnings = listOf(EsriLicenseGuard.readOnlyModeMessage())
                ),
                maps = maps
            )
        } catch (e: Exception) {
            LoadedPackage(
                EsriPackageReport(
                    packageName = file.name,
                    packageType = EsriPackageType.MMPK,
                    errors = listOf(e.message ?: e.toString())
                )
            )
        }
    }

    private suspend fun openMobileGeodatabase(file: File): LoadedPackage {
        val layerReports = mutableListOf<EsriLayerReport>()
        val featureLayers = mutableListOf<Layer>()
        return try {
            val geodatabase = Geodatabase(file.absolutePath)
            geodatabase.load().onSuccess {
                geodatabase.utilityNetworks.forEach { utilityNetwork ->
                    layerReports.add(
                        skippedLayerReport(
                            name = utilityNetwork.name.ifBlank { "Utility Network" },
                            type = "UtilityNetwork",
                            message = "Unsupported in Esri Local Viewer read-only Lite mode"
                        )
                    )
                }
                geodatabase.featureTables.forEach { table ->
                    try {
                        val tableLoad = table.load()
                        if (tableLoad.isSuccess) {
                            val featureLayer = FeatureLayer.createWithFeatureTable(table)
                            val report = loadReadOnlyLayer(featureLayer)
                            layerReports.add(report)
                            if (report.status == EsriLayerLoadStatus.LOADED) {
                                featureLayers.add(featureLayer)
                            }
                        } else {
                            val error = tableLoad.exceptionOrNull()
                            layerReports.add(
                                failedLayerReport(
                                    name = table.tableName,
                                    type = "GeodatabaseFeatureTable",
                                    errorMessage = error?.message ?: error.toString()
                                )
                            )
                        }
                    } catch (e: Exception) {
                        layerReports.add(
                            failedLayerReport(
                                name = table.tableName,
                                type = "GeodatabaseFeatureTable",
                                errorMessage = e.message ?: e.toString()
                            )
                        )
                    }
                }
            }.onFailure { error ->
                return LoadedPackage(
                    EsriPackageReport(
                        packageName = file.name,
                        packageType = EsriPackageType.MOBILE_GEODATABASE,
                        errors = listOf(error.message ?: error.toString())
                    )
                )
            }

            LoadedPackage(
                report = EsriPackageReport(
                    packageName = file.name,
                    packageType = EsriPackageType.MOBILE_GEODATABASE,
                    totalLayers = layerReports.size,
                    layers = layerReports,
                    warnings = listOf(EsriLicenseGuard.readOnlyModeMessage())
                ),
                layers = featureLayers
            )
        } catch (e: Exception) {
            LoadedPackage(
                EsriPackageReport(
                    packageName = file.name,
                    packageType = EsriPackageType.MOBILE_GEODATABASE,
                    errors = listOf(e.message ?: e.toString())
                )
            )
        }
    }

    private suspend fun loadReadOnlyLayer(layer: Layer): EsriLayerReport {
        return try {
            layer.load().onSuccess {
                return EsriLayerReport(
                    name = layer.name.ifBlank { layer::class.simpleName ?: "Layer" },
                    type = layer::class.simpleName ?: "Layer",
                    status = EsriLayerLoadStatus.LOADED
                )
            }.onFailure { error ->
                return failedLayerReport(
                    name = layer.name.ifBlank { layer::class.simpleName ?: "Layer" },
                    type = layer::class.simpleName ?: "Layer",
                    errorMessage = error.message ?: error.toString()
                )
            }
            failedLayerReport(
                name = layer.name.ifBlank { layer::class.simpleName ?: "Layer" },
                type = layer::class.simpleName ?: "Layer",
                errorMessage = "Layer did not load"
            )
        } catch (e: Exception) {
            failedLayerReport(
                name = layer.name.ifBlank { layer::class.simpleName ?: "Layer" },
                type = layer::class.simpleName ?: "Layer",
                errorMessage = e.message ?: e.toString()
            )
        }
    }

    private fun failedLayerReport(name: String, type: String, errorMessage: String): EsriLayerReport {
        return EsriLayerReport(
            name = name,
            type = type,
            status = EsriLayerLoadStatus.FAILED,
            errorMessage = errorMessage,
            licenseWarning = EsriLicenseGuard.licenseWarningFor(errorMessage)
        )
    }

    private fun skippedLayerReport(name: String, type: String, message: String): EsriLayerReport {
        return EsriLayerReport(
            name = name,
            type = type,
            status = EsriLayerLoadStatus.SKIPPED,
            errorMessage = message,
            licenseWarning = null,
            isDisplayed = false
        )
    }

    private fun detectType(file: File): EsriPackageType {
        val lower = file.name.lowercase()
        return when {
            lower.endsWith(".mmpk") -> EsriPackageType.MMPK
            lower.endsWith(".geodatabase") -> EsriPackageType.MOBILE_GEODATABASE
            else -> EsriPackageType.UNSUPPORTED
        }
    }

    companion object {
        private const val TAG = "EsriReadOnlyLoader"
    }
}
