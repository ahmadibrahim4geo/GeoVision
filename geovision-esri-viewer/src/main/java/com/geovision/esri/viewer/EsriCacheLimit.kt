package com.geovision.esri.viewer

enum class EsriCacheLimit(val bytes: Long?) {
    ONE_GB(1L * 1024L * 1024L * 1024L),
    TWO_GB(2L * 1024L * 1024L * 1024L),
    FIVE_GB(5L * 1024L * 1024L * 1024L),
    TEN_GB(10L * 1024L * 1024L * 1024L),
    CUSTOM(null)
}
