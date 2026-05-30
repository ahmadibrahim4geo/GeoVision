package com.geovision.mobile.data.geo

import kotlin.math.*
import java.util.Locale

object CoordinateConverter {

    private const val A = 6378137.0
    private const val F = 1.0 / 298.257223563
    private const val E2 = 2.0 * F - F * F
    private const val E4 = E2 * E2
    private const val E6 = E4 * E2
    private const val EP2 = E2 / (1.0 - E2)
    private const val K0 = 0.9996
    private const val FALSE_EASTING = 500000.0
    private const val FALSE_NORTHING_S = 10000000.0

    data class UtmResult(val zone: Int, val hemisphere: Char, val easting: Double, val northing: Double)

    fun ddToDms(dd: Double, isLat: Boolean): String {
        val absolute = abs(dd)
        val deg = absolute.toInt()
        val minTotal = (absolute - deg) * 60.0
        val min = minTotal.toInt()
        val sec = (minTotal - min) * 60.0
        val dir = if (isLat) (if (dd >= 0) 'N' else 'S') else (if (dd >= 0) 'E' else 'W')
        return "${deg}°${min.toString().padStart(2, '0')}'${String.format(Locale.US, "%.2f", sec).padStart(5, '0')}\"$dir"
    }

    fun dmsToDd(deg: Int, min: Int, sec: Double, dir: Char): Double {
        var result = abs(deg).toDouble() + min / 60.0 + sec / 3600.0
        if (dir.uppercaseChar() in "SW") result = -result
        return result
    }

    fun ddToDdm(dd: Double, isLat: Boolean): String {
        val absolute = abs(dd)
        val deg = absolute.toInt()
        val minutes = (absolute - deg) * 60.0
        val dir = if (isLat) (if (dd >= 0) 'N' else 'S') else (if (dd >= 0) 'E' else 'W')
        return "${deg}°${String.format(Locale.US, "%.4f", minutes)}$dir"
    }

    fun ddmToDd(deg: Int, minutes: Double, dir: Char): Double {
        var result = abs(deg).toDouble() + minutes / 60.0
        if (dir.uppercaseChar() in "SW") result = -result
        return result
    }

    fun ddToUtm(lat: Double, lon: Double): UtmResult {
        val zone = if (lat >= 56.0 && lat < 64.0 && lon >= 3.0 && lon < 12.0) 32
            else if (lat in 72.0..83.999 && lon in 0.0..41.999) {
                if (lon < 9.0) 31 else if (lon < 21.0) 33 else if (lon < 33.0) 35 else 37
            } else ((lon + 180) / 6.0).toInt() + 1
        val centralMeridian = ((zone - 1) * 6 - 180 + 3).toDouble()
        return ddToUtmWithZone(lat, lon, zone, centralMeridian)
    }

    private fun ddToUtmWithZone(lat: Double, lon: Double, zone: Int, cm: Double): UtmResult {
        val phi = Math.toRadians(lat)
        val lam = Math.toRadians(lon)
        val lam0 = Math.toRadians(cm)
        val dLam = lam - lam0

        val sPhi = sin(phi)
        val cPhi = cos(phi)
        val tPhi = tan(phi)

        val m = A * (
            (1.0 - E2/4.0 - 3.0*E4/64.0 - 5.0*E6/256.0) * phi
            - (3.0*E2/8.0 + 3.0*E4/32.0 + 45.0*E6/1024.0) * sin(2.0*phi)
            + (15.0*E4/256.0 + 45.0*E6/1024.0) * sin(4.0*phi)
            - (35.0*E6/3072.0) * sin(6.0*phi)
        )

        val n = A / sqrt(1.0 - E2 * sPhi * sPhi)
        val t = tPhi * tPhi
        val cVal = EP2 * cPhi * cPhi
        val aVal = cPhi * dLam
        val a2 = aVal * aVal
        val a3 = a2 * aVal
        val a4 = a2 * a2
        val a5 = a4 * aVal
        val a6 = a3 * a3

        val easting = K0 * n * (
            aVal + (1.0 - t + cVal) * a3 / 6.0
            + (5.0 - 18.0*t + t*t + 72.0*cVal - 58.0*EP2) * a5 / 120.0
        ) + FALSE_EASTING

        val northingRaw = K0 * (
            m + n * tPhi * (
                a2 / 2.0
                + (5.0 - t + 9.0*cVal + 4.0*cVal*cVal) * a4 / 24.0
                + (61.0 - 58.0*t + t*t + 600.0*cVal - 330.0*EP2) * a6 / 720.0
            )
        )

        val hemisphere = if (lat >= 0) 'N' else 'S'
        val northing = if (lat >= 0) northingRaw else northingRaw + FALSE_NORTHING_S

        return UtmResult(zone, hemisphere, easting, northing)
    }

    fun utmToDd(zone: Int, hemisphere: Char, easting: Double, northing: Double): Pair<Double, Double> {
        val cm = ((zone - 1) * 6 - 180 + 3).toDouble()
        val lam0 = Math.toRadians(cm)
        val x = easting - FALSE_EASTING
        val fn = if (hemisphere.uppercaseChar() == 'N') 0.0 else FALSE_NORTHING_S
        val y = northing - fn

        val e1 = (1.0 - sqrt(1.0 - E2)) / (1.0 + sqrt(1.0 - E2))
        val m = y / K0
        val mu = m / (A * (1.0 - E2/4.0 - 3.0*E4/64.0 - 5.0*E6/256.0))

        val phi1 = mu
            + (3.0*e1/2.0 - 27.0*e1*e1*e1/32.0) * sin(2.0*mu)
            + (21.0*e1*e1/16.0 - 55.0*e1*e1*e1*e1/32.0) * sin(4.0*mu)
            + (151.0*e1*e1*e1/96.0) * sin(6.0*mu)

        val sPhi1 = sin(phi1)
        val cPhi1 = cos(phi1)
        val tPhi1 = tan(phi1)
        val n1 = A / sqrt(1.0 - E2 * sPhi1 * sPhi1)
        val r1 = A * (1.0 - E2) / sqrt((1.0 - E2 * sPhi1 * sPhi1).pow(3.0))
        val d = x / (n1 * K0)
        val d2 = d * d
        val d4 = d2 * d2
        val d6 = d2 * d4

        val phi = phi1 - (n1 * tPhi1 / r1) * (
            d2 / 2.0
            - (5.0 + 3.0*tPhi1*tPhi1 + 10.0*cPhi1*cPhi1 - 4.0*cPhi1*cPhi1*cPhi1*cPhi1 - 9.0*EP2) * d4 / 24.0
            + (61.0 + 90.0*tPhi1*tPhi1 + 298.0*cPhi1*cPhi1 + 45.0*tPhi1*tPhi1*tPhi1*tPhi1 - 252.0*EP2 - 3.0*cPhi1*cPhi1*cPhi1*cPhi1) * d6 / 720.0
        )

        val lam = lam0 + (
            d
            - (1.0 + 2.0*tPhi1*tPhi1 + cPhi1*cPhi1) * d * d2 / 6.0
            + (5.0 - 2.0*cPhi1*cPhi1 + 28.0*tPhi1*tPhi1 - 3.0*cPhi1*cPhi1*cPhi1*cPhi1 + 8.0*EP2 + 24.0*tPhi1*tPhi1*tPhi1*tPhi1) * d * d4 / 120.0
        ) / cPhi1

        return Pair(Math.toDegrees(phi), Math.toDegrees(lam))
    }

    fun ddToWebMercator(lat: Double, lon: Double): Pair<Double, Double> {
        val x = Math.toRadians(lon) * A
        val y = ln(tan(PI / 4.0 + Math.toRadians(lat) / 2.0)) * A
        return Pair(x, y)
    }

    fun webMercatorToDd(x: Double, y: Double): Pair<Double, Double> {
        val lon = Math.toDegrees(x / A)
        val lat = Math.toDegrees(2.0 * atan(exp(y / A)) - PI / 2.0)
        return Pair(lat, lon)
    }

    fun formatDD(lat: Double, lon: Double, decimals: Int = 6): String =
        "${String.format(Locale.US, "%.${decimals}f", lat)} - ${String.format(Locale.US, "%.${decimals}f", lon)}"

    fun formatUtm(result: UtmResult): String =
        "${result.zone}${result.hemisphere}  ${String.format(Locale.US, "%.3f", result.easting)}  ${String.format(Locale.US, "%.3f", result.northing)}"

    fun formatWebMercator(x: Double, y: Double, decimals: Int = 3): String =
        "${String.format(Locale.US, "%.${decimals}f", x)} - ${String.format(Locale.US, "%.${decimals}f", y)}"
}
