package com.geovision.mobile.ui.screens.map

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.MapTileIndex
import com.geovision.mobile.R

enum class BasemapCategory(@StringRes val nameRes: Int) {
    STREET(R.string.map_category_street),
    SATELLITE(R.string.map_category_satellite),
    HYBRID(R.string.map_category_hybrid),
    TERRAIN(R.string.map_category_terrain),
    MARINE(R.string.map_category_marine)
}

data class TileSourceDef(
    val key: String,
    @StringRes val nameRes: Int,
    val source: ITileSource,
    val sourceDesc: String? = null,
    val styleUrl: String? = null,
    @StringRes val groupNameRes: Int? = null,
    val category: BasemapCategory = BasemapCategory.STREET
)

private class EsriTileSource(name: String, zoomMin: Int, zoomMax: Int, ext: String, urls: Array<String>, copyright: String)
    : XYTileSource(name, zoomMin, zoomMax, 256, ext, urls, copyright) {
    override fun getTileURLString(tileIndex: Long): String =
        baseUrl + MapTileIndex.getZoom(tileIndex) + "/" + MapTileIndex.getY(tileIndex) + "/" + MapTileIndex.getX(tileIndex)
}

private fun esri(name: String, url: String) = EsriTileSource(name, 0, 25, "", arrayOf(url), "\u00A9 Esri")

private val ESRI_SAT = esri("EsriSatellite", "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/")

private val ESRI_STREET = esri("EsriTopo", "https://server.arcgisonline.com/ArcGIS/rest/services/World_Topo_Map/MapServer/tile/")

private val ESRI_NATGEO = esri("EsriNatGeo", "https://server.arcgisonline.com/ArcGIS/rest/services/NatGeo_World_Map/MapServer/tile/")

private val ESRI_OCEAN = esri("EsriOcean", "https://server.arcgisonline.com/ArcGIS/rest/services/Ocean/World_Ocean_Base/MapServer/tile/")

private val ESRI_GRAY = esri("EsriGray", "https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Light_Gray_Base/MapServer/tile/")

private val ESRI_DARK_GRAY = esri("EsriDarkGray", "https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Dark_Gray_Base/MapServer/tile/")

private val ESRI_SHADED_RELIEF = esri("EsriShadedRelief", "https://server.arcgisonline.com/ArcGIS/rest/services/World_Shaded_Relief/MapServer/tile/")

private val CARTO_DARK = XYTileSource("CartoDBDarkMatter", 0, 25, 256, ".png", arrayOf(
    "https://a.basemaps.cartocdn.com/dark_all/",
    "https://b.basemaps.cartocdn.com/dark_all/",
    "https://c.basemaps.cartocdn.com/dark_all/"
), "? OpenStreetMap contributors, ? CARTO")

private val CARTO_LIGHT = XYTileSource("CartoDBLight", 0, 25, 256, ".png", arrayOf(
    "https://a.basemaps.cartocdn.com/light_all/",
    "https://b.basemaps.cartocdn.com/light_all/",
    "https://c.basemaps.cartocdn.com/light_all/"
), "? OpenStreetMap contributors, ? CARTO")

val ESRI_LABELS: ITileSource = esri("EsriLabels", "https://server.arcgisonline.com/ArcGIS/rest/services/Reference/World_Boundaries_and_Places/MapServer/tile/")

private class GoogleTileSource(name: String, zoomMin: Int, zoomMax: Int, private val style: String, urls: Array<String>)
    : XYTileSource(name, zoomMin, zoomMax, 256, "", urls, "\u00A9 Google") {
    override fun getTileURLString(tileIndex: Long): String =
        baseUrl + "?x=${MapTileIndex.getX(tileIndex)}&y=${MapTileIndex.getY(tileIndex)}&z=${MapTileIndex.getZoom(tileIndex)}"
}

private fun google(name: String, style: String) =
    GoogleTileSource(name, 0, 25, style, arrayOf("https://mt1.google.com/vt/lyrs=$style"))


private class BingTileSource(name: String, zoomMin: Int, zoomMax: Int, private val style: String, private val ext: String)
    : XYTileSource(name, zoomMin, zoomMax, 256, ext, emptyArray(), "\u00A9 Microsoft") {
    override fun getTileURLString(tileIndex: Long): String {
        val z = MapTileIndex.getZoom(tileIndex)
        val x = MapTileIndex.getX(tileIndex)
        val y = MapTileIndex.getY(tileIndex)
        val quadKey = bingQuadKey(x, y, z)
        val server = (x + y) % 4
        return "https://ecn.t$server.tiles.virtualearth.net/tiles/$style$quadKey$ext?g=1&key=" + bingDemoKey()
    }
}

private fun bingQuadKey(x: Int, y: Int, z: Int): String {
    val sb = StringBuilder(z)
    for (i in z - 1 downTo 0) {
        val bit = (x shr i and 1) or ((y shr i and 1) shl 1)
        sb.append(bit)
    }
    return sb.toString()
}

private fun bingDemoKey(): String = "AjVg7rON9JMmW6goTEvBYLJQ3Vf5v3UHv2LgRPGhYyQ-BU_qb7HWXzZ7e5kJER7R"

private fun bing(name: String, style: String, ext: String) =
    BingTileSource(name, 0, 19, style, ext)

private val YANDEX_SAT = XYTileSource("YandexSat", 0, 19, 256, ".jpg", arrayOf(
    "https://sat02.maps.yandex.net/tiles?l=sat&x={x}&y={y}&z={z}"
), "\u00A9 Yandex")

private val YANDEX_HYBRID = XYTileSource("YandexHybrid", 0, 19, 256, ".jpg", arrayOf(
    "https://sat02.maps.yandex.net/tiles?l=skl&x={x}&y={y}&z={z}"
), "\u00A9 Yandex")

private val ESRI_STREETS = esri("EsriStreets", "https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/")

private val OPEN_TOPO_MAP = XYTileSource("OpenTopoMap", 0, 17, 256, ".png", arrayOf(
    "https://a.tile.opentopomap.org/",
    "https://b.tile.opentopomap.org/"
), "\u00A9 OpenTopoMap contributors")

private val WIKIMEDIA = XYTileSource("WikimediaOSM", 0, 25, 256, ".png", arrayOf(
    "https://maps.wikimedia.org/osm-intl/"
), "\u00A9 OpenStreetMap contributors")

val EMPTY_TILE_SOURCE = XYTileSource("Empty", 0, 25, 256, ".png", emptyArray(), "")

fun groupTileSources(
    classification: String,
    sources: List<TileSourceDef> = tileSources
): List<Pair<String, List<TileSourceDef>>> {
    if (classification == "provider") {
        return sources.groupBy { it.sourceDesc ?: "Other" }
            .map { (label, items) -> label to items.sortedBy { it.key } }
            .sortedBy { (label, _) -> label }
    }
    return sources.groupBy { it.category }
        .map { (cat, items) -> cat.name to items.sortedBy { it.key } }
        .sortedBy { (_, _) -> 0 } // preserve category enum order
}

@Composable
fun BasemapPickerContent(
    tileSources: List<TileSourceDef>,
    selectedKey: String,
    defaultKey: String?,
    classification: String,
    onSelect: (String) -> Unit,
    radioStyle: Boolean = false
) {
    val grouped = groupTileSources(classification, tileSources)
    Column(Modifier.selectableGroup()) {
        grouped.forEach { (groupLabel, items) ->
            val titleRes = if (classification != "provider") {
                try { BasemapCategory.valueOf(groupLabel).nameRes } catch (_: Exception) { null }
            } else null
            if (titleRes != null) {
                Text(stringResource(titleRes), style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.W700, color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp))
            } else {
                Text(groupLabel, style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.W700, color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp))
            }
            items.forEach { ts ->
                val sel = selectedKey == ts.key
                val isDefault = defaultKey != null && ts.key == defaultKey
                if (radioStyle) {
                    val bgColor = if (sel) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
                        else MaterialTheme.colorScheme.surfaceContainerLow
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)).background(bgColor)
                            .selectable(selected = sel, role = androidx.compose.ui.semantics.Role.RadioButton, onClick = { onSelect(ts.key) })
                            .padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = sel, onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.secondary))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(ts.nameRes), fontWeight = if (sel) FontWeight.W700 else FontWeight.W500,
                            color = if (sel) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface)
                        if (ts.sourceDesc != null) {
                            Spacer(Modifier.width(4.dp))
                            Text("(${ts.sourceDesc})", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                } else {
                    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(
                        containerColor = if (sel) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceContainerHighest),
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(ts.key) }) {
                        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (sel) Icons.Default.Map else Icons.Default.Layers, null, Modifier.size(22.dp),
                                tint = if (sel) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isDefault) Text("★ ", fontWeight = FontWeight.W700, color = MaterialTheme.colorScheme.tertiary)
                                    Text(stringResource(ts.nameRes), fontWeight = if (sel) FontWeight.W700 else FontWeight.W500,
                                        color = if (sel) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface)
                                    if (ts.sourceDesc != null) {
                                        Spacer(Modifier.width(4.dp))
                                        Text("(${ts.sourceDesc})", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                                    }
                                }
                                if (sel) Text(stringResource(R.string.status_active), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                            }
                            if (sel) Icon(Icons.Default.CheckCircle, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
        }
    }
}

val tileSources = listOf(
    // ── STREET ──
    TileSourceDef("mapnik", R.string.map_name_mapnik, TileSourceFactory.MAPNIK, "OSM", category = BasemapCategory.STREET),
    TileSourceDef("transport", R.string.map_name_transport, CARTO_LIGHT, "CartoDB", "https://basemaps.cartocdn.com/gl/positron-gl-style/style.json", category = BasemapCategory.STREET),
    TileSourceDef("dark", R.string.map_name_dark, CARTO_DARK, "CartoDB", "https://basemaps.cartocdn.com/gl/dark-matter-gl-style/style.json", category = BasemapCategory.STREET),
    TileSourceDef("wikimedia", R.string.map_name_wikimedia, WIKIMEDIA, "Wikimedia", category = BasemapCategory.STREET),
    TileSourceDef("google_road", R.string.map_name_google_road, google("GoogleRoad", "m"), "Google", category = BasemapCategory.STREET),
    TileSourceDef("esri_streets", R.string.map_name_esri_streets, ESRI_STREETS, "Esri", category = BasemapCategory.STREET),
    TileSourceDef("bing_road", R.string.map_name_bing_road, bing("BingRoad", "r", ".png"), "Bing", category = BasemapCategory.STREET),

    // ── SATELLITE ──
    TileSourceDef("satellite", R.string.map_name_satellite, ESRI_SAT, "Esri", category = BasemapCategory.SATELLITE),
    TileSourceDef("satellite_clean", R.string.map_name_satellite_clean, ESRI_SAT, "Esri", category = BasemapCategory.SATELLITE),
    TileSourceDef("google_sat", R.string.map_name_google_sat, google("GoogleSat", "s"), "Google", category = BasemapCategory.SATELLITE),
    TileSourceDef("usgs_sat", R.string.map_name_usgs_sat, TileSourceFactory.USGS_SAT, "USGS", category = BasemapCategory.SATELLITE),
    TileSourceDef("yandex_sat", R.string.map_name_yandex_sat, YANDEX_SAT, "Yandex", category = BasemapCategory.SATELLITE),
    TileSourceDef("bing_sat", R.string.map_name_bing_sat, bing("BingSat", "a", ".jpeg"), "Bing", category = BasemapCategory.SATELLITE),

    // ── HYBRID ──
    TileSourceDef("hybrid", R.string.map_name_hybrid, ESRI_STREET, "Esri", category = BasemapCategory.HYBRID),
    TileSourceDef("google_hybrid", R.string.map_name_google_hybrid, google("GoogleHybrid", "y"), "Google", category = BasemapCategory.HYBRID),
    TileSourceDef("yandex_hybrid", R.string.map_name_yandex_hybrid, YANDEX_HYBRID, "Yandex", category = BasemapCategory.HYBRID),
    TileSourceDef("bing_hybrid", R.string.map_name_bing_hybrid, bing("BingHybrid", "h", ".jpeg"), "Bing", category = BasemapCategory.HYBRID),

    // ── TERRAIN ──
    TileSourceDef("topo", R.string.map_name_topo, TileSourceFactory.OpenTopo, "OpenTopoMap", category = BasemapCategory.TERRAIN),
    TileSourceDef("hikebike", R.string.map_name_hikebike, TileSourceFactory.HIKEBIKEMAP, "OSM", category = BasemapCategory.TERRAIN),
    TileSourceDef("usgs_topo", R.string.map_name_usgs_topo, TileSourceFactory.USGS_TOPO, "USGS", category = BasemapCategory.TERRAIN),
    TileSourceDef("opentopo", R.string.map_name_opentopo, OPEN_TOPO_MAP, "OpenTopoMap", category = BasemapCategory.TERRAIN),
    TileSourceDef("natgeo", R.string.map_name_natgeo, ESRI_NATGEO, "Esri", category = BasemapCategory.TERRAIN),
    TileSourceDef("relief", R.string.map_name_relief, ESRI_SHADED_RELIEF, "Esri", category = BasemapCategory.TERRAIN),
    TileSourceDef("google_terrain", R.string.map_name_google_terrain, google("GoogleTerrain", "p"), "Google", category = BasemapCategory.TERRAIN),

    // ── MARINE ──
    TileSourceDef("ocean", R.string.map_name_ocean, ESRI_OCEAN, "Esri", category = BasemapCategory.MARINE),

    // ── Other / Utility ──
    TileSourceDef("gray", R.string.map_name_gray, ESRI_GRAY, "Esri", category = BasemapCategory.STREET),
    TileSourceDef("dark_gray", R.string.map_name_dark_gray, ESRI_DARK_GRAY, "Esri", category = BasemapCategory.STREET)
)
val defaultTileKey = "mapnik"
