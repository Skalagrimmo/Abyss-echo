package com.example.ui.screen

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.R
import com.example.model.EnemyArchetype
import kotlin.math.max
import kotlin.math.min

data class EnemySpriteFrames(
    val idle: ImageBitmap,
    val attack: ImageBitmap,
    val hurt: ImageBitmap
)

data class SpriteRect(
    val minX: Int,
    val minY: Int,
    val maxX: Int,
    val maxY: Int
) {
    val width: Int get() = (maxX - minX + 1).coerceAtLeast(1)
    val height: Int get() = (maxY - minY + 1).coerceAtLeast(1)
}

object EnemySpriteCache {
    @Volatile
    private var cachedBank: Map<EnemyArchetype, EnemySpriteFrames>? = null

    fun getOrLoad(context: Context): Map<EnemyArchetype, EnemySpriteFrames> {
        cachedBank?.let { return it }
        synchronized(this) {
            cachedBank?.let { return it }
            val loaded = mutableMapOf<EnemyArchetype, EnemySpriteFrames>()
            for (archetype in EnemyArchetype.entries) {
                val resId = resourceForArchetype(archetype)
                decodeAndSliceSheet(context, resId)?.let { frames ->
                    loaded[archetype] = frames
                }
            }
            cachedBank = loaded
            return loaded
        }
    }

    private fun resourceForArchetype(archetype: EnemyArchetype): Int = when (archetype) {
        EnemyArchetype.CRAWLING_SPIDER -> R.drawable.img_spider_sheet
        EnemyArchetype.CHTHONIC_CULTIST -> R.drawable.img_cultist_sheet
        EnemyArchetype.IRON_AUTOMATON -> R.drawable.img_automaton_sheet
        EnemyArchetype.SLAG_HOUND -> R.drawable.img_hound_sheet
        EnemyArchetype.CRUCIBLE_TITAN -> R.drawable.img_titan_sheet
    }

    private fun decodeAndSliceSheet(context: Context, resId: Int): EnemySpriteFrames? {
        return try {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeResource(context.resources, resId, boundsOpts)
            if (boundsOpts.outWidth <= 0 || boundsOpts.outHeight <= 0) return null

            var sampleSize = 1
            while ((boundsOpts.outWidth / sampleSize) > 768) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val rawBitmap = BitmapFactory.decodeResource(context.resources, resId, decodeOpts)
                ?: return null

            val w = rawBitmap.width
            val h = rawBitmap.height
            val pixels = IntArray(w * h)
            rawBitmap.getPixels(pixels, 0, w, 0, 0, w, h)
            rawBitmap.recycle()

            val splits = processSheetPixelsAndFindBounds(pixels, w, h) ?: return null
            val (box0, box1, box2) = splits

            val commonDim = maxOf(
                box0.width, box0.height,
                box1.width, box1.height,
                box2.width, box2.height
            ) + 4

            val frame0 = extractSquareFrame(pixels, w, box0, commonDim).asImageBitmap()
            val frame1 = extractSquareFrame(pixels, w, box1, commonDim).asImageBitmap()
            val frame2 = extractSquareFrame(pixels, w, box2, commonDim).asImageBitmap()

            EnemySpriteFrames(
                idle = frame0,
                attack = frame1,
                hurt = frame2
            )
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Removes border-connected near-white background via BFS flood-fill,
     * cleans 1px anti-aliased edge halo, and splits horizontally into 3 sprite bounding boxes.
     */
    internal fun processSheetPixelsAndFindBounds(
        pixels: IntArray,
        w: Int,
        h: Int
    ): Triple<SpriteRect, SpriteRect, SpriteRect>? {
        if (w < 6 || h < 4) return null

        val visited = BooleanArray(w * h)
        val queue = IntArray(w * h)
        var head = 0
        var tail = 0

        fun isNearWhiteBg(argb: Int): Boolean {
            val a = (argb ushr 24) and 0xFF
            if (a < 32) return true
            val r = (argb ushr 16) and 0xFF
            val g = (argb ushr 8) and 0xFF
            val b = argb and 0xFF
            val maxC = max(r, max(g, b))
            val minC = min(r, min(g, b))
            return r >= 224 && g >= 224 && b >= 224 && (maxC - minC) <= 28
        }

        fun enqueueIfBg(x: Int, y: Int) {
            val idx = y * w + x
            if (!visited[idx] && isNearWhiteBg(pixels[idx])) {
                visited[idx] = true
                pixels[idx] = 0
                queue[tail++] = idx
            }
        }

        // Seed outer perimeter
        for (x in 0 until w) {
            enqueueIfBg(x, 0)
            enqueueIfBg(x, h - 1)
        }
        for (y in 0 until h) {
            enqueueIfBg(0, y)
            enqueueIfBg(w - 1, y)
        }

        // 4-way BFS flood-fill
        while (head < tail) {
            val idx = queue[head++]
            val x = idx % w
            val y = idx / w
            if (x > 0) enqueueIfBg(x - 1, y)
            if (x + 1 < w) enqueueIfBg(x + 1, y)
            if (y > 0) enqueueIfBg(x, y - 1)
            if (y + 1 < h) enqueueIfBg(x, y + 1)
        }

        // Single-pass 1px halo de-fringe around transparent background
        val defringeIndices = IntArray(w * h)
        var defringeCount = 0
        for (y in 1 until h - 1) {
            val rowOff = y * w
            for (x in 1 until w - 1) {
                val idx = rowOff + x
                val px = pixels[idx]
                if (px == 0) continue
                val hasTransparentNeighbor = pixels[idx - 1] == 0 ||
                    pixels[idx + 1] == 0 ||
                    pixels[idx - w] == 0 ||
                    pixels[idx + w] == 0
                if (hasTransparentNeighbor) {
                    val r = (px ushr 16) and 0xFF
                    val g = (px ushr 8) and 0xFF
                    val b = px and 0xFF
                    val maxC = max(r, max(g, b))
                    val minC = min(r, min(g, b))
                    if (r >= 200 && g >= 200 && b >= 200 && (maxC - minC) <= 28) {
                        defringeIndices[defringeCount++] = idx
                    }
                }
            }
        }
        for (i in 0 until defringeCount) {
            pixels[defringeIndices[i]] = 0
        }

        // Compute per-column opaque counts to detect gaps between the 3 poses
        val colCounts = IntArray(w)
        for (y in 0 until h) {
            val rowOff = y * w
            for (x in 0 until w) {
                if ((pixels[rowOff + x] ushr 24) > 20) {
                    colCounts[x]++
                }
            }
        }

        val split1 = findBestValleySplit(colCounts, (w * 0.20f).toInt(), (w * 0.43f).toInt(), w / 3)
        val split2 = findBestValleySplit(colCounts, (w * 0.57f).toInt(), (w * 0.80f).toInt(), (w * 2) / 3)

        val box0 = computeTightBounds(pixels, w, h, 0, split1) ?: return null
        val box1 = computeTightBounds(pixels, w, h, split1, split2) ?: return null
        val box2 = computeTightBounds(pixels, w, h, split2, w) ?: return null

        return Triple(box0, box1, box2)
    }

    private fun findBestValleySplit(colCounts: IntArray, startX: Int, endX: Int, fallback: Int): Int {
        val lo = startX.coerceIn(1, colCounts.size - 2)
        val hi = endX.coerceIn(lo + 1, colCounts.size - 1)

        // Prefer center of longest empty column run
        var bestZeroStart = -1
        var bestZeroLen = 0
        var curStart = -1
        for (x in lo..hi) {
            if (colCounts[x] == 0) {
                if (curStart == -1) curStart = x
                val len = x - curStart + 1
                if (len > bestZeroLen) {
                    bestZeroLen = len
                    bestZeroStart = curStart
                }
            } else {
                curStart = -1
            }
        }
        if (bestZeroLen > 0 && bestZeroStart >= 0) {
            return bestZeroStart + bestZeroLen / 2
        }

        // Otherwise pick minimum opaque count column
        var minCol = fallback.coerceIn(lo, hi)
        var minVal = Int.MAX_VALUE
        for (x in lo..hi) {
            if (colCounts[x] < minVal) {
                minVal = colCounts[x]
                minCol = x
            }
        }
        return minCol
    }

    private fun computeTightBounds(
        pixels: IntArray,
        w: Int,
        h: Int,
        xStart: Int,
        xEnd: Int
    ): SpriteRect? {
        var minX = xEnd
        var minY = h
        var maxX = -1
        var maxY = -1

        for (y in 0 until h) {
            val rowOff = y * w
            for (x in xStart until xEnd) {
                if ((pixels[rowOff + x] ushr 24) > 20) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }

        if (maxX < minX || maxY < minY) return null
        return SpriteRect(minX, minY, maxX, maxY)
    }

    private fun extractSquareFrame(
        sheetPixels: IntArray,
        sheetW: Int,
        rect: SpriteRect,
        targetDim: Int
    ): Bitmap {
        val outPixels = IntArray(targetDim * targetDim)
        val offsetX = ((targetDim - rect.width) / 2).coerceAtLeast(0)
        // Bottom-align sprite near ground baseline so Idle, Attack, Hurt share floor contact
        val offsetY = (targetDim - rect.height - 2).coerceAtLeast(0)

        for (ry in 0 until rect.height) {
            val srcY = rect.minY + ry
            val dstY = offsetY + ry
            if (dstY !in 0 until targetDim) continue
            val srcRow = srcY * sheetW
            val dstRow = dstY * targetDim
            for (rx in 0 until rect.width) {
                val srcX = rect.minX + rx
                val dstX = offsetX + rx
                if (dstX !in 0 until targetDim) continue
                outPixels[dstRow + dstX] = sheetPixels[srcRow + srcX]
            }
        }

        return Bitmap.createBitmap(outPixels, targetDim, targetDim, Bitmap.Config.ARGB_8888)
    }
}
