package org.cf0x.rustnithm.Theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun DefaultGameSkin(
    activatedAir: Set<Int>,
    activatedSlide: Set<Int>,
    serverSliderLed: ByteArray = byteArrayOf(),
    useServerLed: Boolean = false,
    airWeight: Float,
    slideWeight: Float,
    multiA: Float,
    touchPoints: Map<*, Offset>,
    multiS: Float,
    airMode: Int
) {
    val currentColor = MaterialTheme.colorScheme.primary
    val isDark = isSystemInDarkTheme()

    val engine = remember(currentColor, isDark) {
        SkinColorEngine(currentColor, isDark)
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val totalHeight = size.height
        val totalWidth = size.width
        val airAreaHeight = totalHeight * (airWeight / (airWeight + slideWeight))
        val slideAreaHeight = totalHeight - airAreaHeight
        val singleAirHeight = airAreaHeight / 6

        for (i in 0 until 6) {
            val rectTopY = airAreaHeight - (i + 1) * singleAirHeight
            val isActive = activatedAir.contains(i + 1)

            drawRect(
                color = engine.getAreaColor(isActive = isActive),
                topLeft = Offset(0f, rectTopY),
                size = Size(totalWidth, singleAirHeight)
            )
            if (airMode == 1) {
                drawRect(
                    color = engine.getDividerColor(),
                    topLeft = Offset(0f, rectTopY),
                    size = Size(totalWidth, singleAirHeight),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
        }

        val sw = totalWidth / 16
        val rslide = sw * multiS
        val sh = slideAreaHeight / 2
        val cellMargin = 1.5.dp.toPx()
        val hasServerLed = useServerLed &&
            (serverSliderLed.size == 31 * 4 || serverSliderLed.size == 32 * 4)
        val cellInset = if (hasServerLed) cellMargin else 0f
        for (index in 0 until 32) {
            val row = if (index % 2 == 0) 0 else 1
            val colFromLeft = 15 - (index / 2)
            val rectOffset = Offset(colFromLeft * sw, airAreaHeight + row * sh)

            val isActive = activatedSlide.contains(index + 1)
            val ledColor = if (useServerLed) {
                serverSliderZoneColor(serverSliderLed, index)
            } else null
            drawRect(
                color = if (hasServerLed) {
                    ledColor ?: engine.getAreaColor(isActive = false)
                } else {
                    engine.getAreaColor(isActive = isActive)
                },
                topLeft = rectOffset + Offset(cellInset, cellInset),
                size = Size(sw - cellInset * 2f, sh - cellInset * 2f)
            )
            drawRect(
                color = engine.getDividerColor(alpha = 0.1f),
                topLeft = rectOffset + Offset(cellInset, cellInset),
                size = Size(sw - cellInset * 2f, sh - cellInset * 2f),
                style = Stroke(width = 0.5.dp.toPx())
            )
        }

        if (hasServerLed &&
            (serverSliderLed.size == 31 * 4 || serverSliderLed.size == 32 * 4)
        ) {
            for (i in 1..15) {
                val lineX = totalWidth - (i * sw)
                // Lines are drawn from the right edge to the left, matching
                // the alternating divider records in the server payload.
                val color = serverSliderDividerColor(serverSliderLed, i - 1)
                if (color != null) {
                    drawLine(
                        color = color,
                        start = Offset(lineX, airAreaHeight + cellMargin),
                        end = Offset(lineX, totalHeight - cellMargin),
                        strokeWidth = cellMargin * 2f,
                    )
                }
            }
        }

        touchPoints.values.forEach { pos ->
            val pointColor = engine.getAreaColor(isActive = true, alpha = 0.5f)

            drawCircle(
                color = pointColor,
                radius = rslide,
                center = pos,
            )
            drawCircle(
                color = engine.getDividerColor(alpha = 0.8f),
                radius = rslide,
                center = pos,
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
}

private fun serverSliderZoneColor(bytes: ByteArray, index: Int): androidx.compose.ui.graphics.Color? {
    // Each pair of logical slider touch points shares one zone RGB record.
    // The legacy 32-record payload is the same alternating stream with one
    // repeated final record, so it uses the same mapping as the 31-record
    // payload.
    val recordIndex = if (bytes.size == 31 * 4 || bytes.size == 32 * 4) {
        // The wire stream alternates zone and divider records. Each pair of
        // logical touch points shares the next even (zone) record, so skip
        // the divider record between adjacent zones.
        (index / 2) * 2
    } else {
        index
    }
    return serverSliderRecordColor(bytes, recordIndex)
}

private fun serverSliderDividerColor(bytes: ByteArray, index: Int): androidx.compose.ui.graphics.Color? {
    if ((bytes.size != 31 * 4 && bytes.size != 32 * 4) || index !in 0 until 15) {
        return null
    }
    return serverSliderRecordColor(bytes, index * 2 + 1)
}

private fun serverSliderRecordColor(bytes: ByteArray, recordIndex: Int): androidx.compose.ui.graphics.Color? {
    val offset = recordIndex * 4
    if (offset + 3 >= bytes.size) return null
    val red = bytes[offset].toInt() and 0xff
    val green = bytes[offset + 1].toInt() and 0xff
    val blue = bytes[offset + 2].toInt() and 0xff
    val brightness = bytes[offset + 3].toInt() and 0xff
    if (red == 0 && green == 0 && blue == 0) return null
    val scale = brightness / 255f
    return androidx.compose.ui.graphics.Color(
        red = (red * scale / 255f).coerceIn(0f, 1f),
        green = (green * scale / 255f).coerceIn(0f, 1f),
        blue = (blue * scale / 255f).coerceIn(0f, 1f),
        alpha = 1f,
    )
}
