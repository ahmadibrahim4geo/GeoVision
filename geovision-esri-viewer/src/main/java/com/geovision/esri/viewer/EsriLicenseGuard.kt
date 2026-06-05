package com.geovision.esri.viewer

import android.content.Context
import android.util.Log
import com.arcgismaps.ArcGISEnvironment

object EsriLicenseGuard {
    private const val TAG = "EsriLicenseGuard"

    enum class Capability {
        READ_ONLY_LOCAL_VIEW,
        EDITING,
        SYNC,
        OFFLINE_GENERATE,
        ONLINE_SERVICE,
        ROUTING,
        GEOCODING,
        UTILITY_NETWORK,
        ADVANCED_ANALYSIS
    }

    private val blockedCapabilities = setOf(
        Capability.EDITING,
        Capability.SYNC,
        Capability.OFFLINE_GENERATE,
        Capability.ONLINE_SERVICE,
        Capability.ROUTING,
        Capability.GEOCODING,
        Capability.UTILITY_NETWORK,
        Capability.ADVANCED_ANALYSIS
    )

    @Volatile
    var settings: EsriViewerSettings = EsriViewerSettings()
        private set

    fun configure(context: Context? = null, settings: EsriViewerSettings = EsriViewerSettings()) {
        this.settings = settings.copy(readOnly = true)
        context?.let { ArcGISEnvironment.applicationContext = it.applicationContext }
    }

    fun applyLiteLicense(licenseString: String?): Boolean {
        if (licenseString.isNullOrBlank()) {
            Log.w(TAG, "No Esri Lite license string supplied. SDK may run in developer mode only.")
            return false
        }
        return try {
            val licenseKeyClass = Class.forName("com.arcgismaps.LicenseKey")
            val environmentClass = Class.forName("com.arcgismaps.ArcGISEnvironment")
            val companion = licenseKeyClass.getField("Companion").get(null)
            val createMethod = companion.javaClass.methods.firstOrNull { method ->
                method.name.startsWith("create") && method.parameterTypes.size == 1
            } ?: error("LicenseKey.create not found")
            val licenseKey = createMethod.invoke(companion, licenseString)
            val environment = environmentClass.getField("INSTANCE").get(null)
            val setLicense = environmentClass.methods.firstOrNull { method ->
                method.name == "setLicense" && method.parameterTypes.isNotEmpty()
            } ?: error("ArcGISEnvironment.setLicense not found")
            val result = if (setLicense.parameterTypes.size == 1) {
                setLicense.invoke(environment, licenseKey)
            } else {
                setLicense.invoke(environment, licenseKey, emptyList<Any>())
            }
            Log.i(TAG, "Applied Esri license result: $result")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to apply Esri Lite license: ${e.message}")
            false
        }
    }

    fun requireAllowed(capability: Capability): Boolean {
        val allowed = capability == Capability.READ_ONLY_LOCAL_VIEW ||
            (capability == Capability.ONLINE_SERVICE && settings.allowOnlineServices)
        if (!allowed || capability in blockedCapabilities) {
            Log.w(TAG, "Blocked Esri capability in read-only viewer: $capability")
            return false
        }
        return true
    }

    fun readOnlyModeMessage(): String {
        return "Esri Local Viewer is read-only. Editing, sync, generated offline areas, and online services are disabled."
    }

    fun licenseWarningFor(errorMessage: String?): String? {
        val message = errorMessage?.lowercase() ?: return null
        return when {
            "license" in message || "licensed" in message || "standard" in message || "advanced" in message ->
                "هذه الطبقة لم يتم عرضها لأن نوع البيانات داخل الحزمة قد يحتاج ترخيص Esri أعلى من Lite."
            else -> null
        }
    }
}
