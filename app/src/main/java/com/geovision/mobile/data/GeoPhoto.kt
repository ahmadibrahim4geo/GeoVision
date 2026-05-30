package com.geovision.mobile.data

import android.net.Uri

data class GeoPhoto(
    val uri: Uri,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val timestamp: Long? = null,
    val fileName: String,
    val fileSize: Long = 0,
    val bearing: Float? = null
)
