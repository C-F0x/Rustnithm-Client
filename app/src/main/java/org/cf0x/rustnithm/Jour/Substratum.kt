package org.cf0x.rustnithm.Jour

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import org.cf0x.rustnithm.Theme.SkinColorEngine

@Composable
fun Substratum(
    flickZoneNum: Int,
    touchPoints: Map<*, Offset>,
    modifier: Modifier = Modifier
) {
    val currentColor = MaterialTheme.colorScheme.primary
    val isDark = isSystemInDarkTheme()

    val engine = remember(currentColor, isDark) {
        SkinColorEngine(currentColor, isDark)
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val totalWidth = size.width
        val totalHeight = size.height

        val effectiveZoneNum = flickZoneNum.coerceAtLeast(1)
        val zoneHeight = totalHeight / effectiveZoneNum

        touchPoints.values.forEach { pos ->
            val zoneIndex = (pos.y / zoneHeight).toInt().coerceIn(0, effectiveZoneNum - 1)
            val rectTop = zoneIndex * zoneHeight

            drawRect(
                color = engine.getAreaColor(isActive = true, alpha = 0.12f),
                topLeft = Offset(0f, rectTop),
                size = Size(totalWidth, zoneHeight)
            )
        }

        val dividerColor = engine.getDividerColor(alpha = 0.3f)
        val tickColor = engine.getDividerColor(alpha = 0.6f)
        val tickLength = 10.dp.toPx()
        val strokeWidth = 1.dp.toPx()

        for (i in 0..effectiveZoneNum) {
            val lineY = i * zoneHeight

            drawLine(
                color = dividerColor.copy(alpha = 0.1f),
                start = Offset(0f, lineY),
                end = Offset(totalWidth, lineY),
                strokeWidth = 0.5.dp.toPx()
            )

            drawLine(
                color = tickColor,
                start = Offset(0f, lineY),
                end = Offset(tickLength, lineY),
                strokeWidth = strokeWidth
            )

            drawLine(
                color = tickColor,
                start = Offset(totalWidth - tickLength, lineY),
                end = Offset(totalWidth, lineY),
                strokeWidth = strokeWidth
            )
        }
    }
}