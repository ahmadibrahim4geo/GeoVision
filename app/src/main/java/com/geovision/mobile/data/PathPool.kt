package com.geovision.mobile.data

import android.graphics.Path
import android.graphics.Paint

/**
 * Object pool for reusable Android Canvas drawing objects.
 * Reduces GC pressure by avoiding allocations in the draw() hot path.
 */
class PathPool(maxPoolSize: Int = 64) {
    private val pathPool = mutableListOf<Path>()
    private val paintPool = mutableListOf<Paint>()
    private val maxSize = maxPoolSize

    fun obtainPath(): Path {
        return if (pathPool.isNotEmpty()) {
            pathPool.removeAt(pathPool.lastIndex).also { it.rewind() }
        } else {
            Path().also { it.setFillType(Path.FillType.EVEN_ODD) }
        }
    }

    fun recyclePath(path: Path) {
        path.rewind()
        if (pathPool.size < maxSize) {
            pathPool.add(path)
        }
    }

    fun obtainPaint(flags: Int = Paint.ANTI_ALIAS_FLAG): Paint {
        return if (paintPool.isNotEmpty()) {
            paintPool.removeAt(paintPool.lastIndex)
        } else {
            Paint(flags)
        }
    }

    fun recyclePaint(paint: Paint) {
        if (paintPool.size < maxSize) {
            paintPool.add(paint)
        }
    }

    fun clear() {
        pathPool.clear()
        paintPool.clear()
    }
}
