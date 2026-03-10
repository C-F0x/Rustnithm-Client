package org.cf0x.rustnithm.Bon

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.*

enum class PointPos { Tip, Valley }

data class WavyoidConfig<T>(
    val options: List<T>,
    val initialSelectedIndex: Int = 0,
    val pointPos: PointPos = PointPos.Tip,
    val customSamplingPoints: Int? = null,
    val amplitude: Float = 15f,
    val strokeWidth: Dp = 3.dp,
    val color: Color = Color.Unspecified,
    val labelFactory: (T) -> String = { it.toString() }
) {
    init {
        require(options.isNotEmpty()) { "Wavyoid option list cannot be empty!" }
    }
}

fun createWavyoidPath(
    center: Offset,
    baseRadius: Float,
    waveCount: Int,
    amplitude: Float,
    pointPos: PointPos,
    customSamplingPoints: Int?
): Path {
    val path = Path()
    val samplingPoints = customSamplingPoints ?: max(180, waveCount * 20)
    val phaseOffset = -PI.toFloat() / 2f

    for (i in 0..samplingPoints) {
        val relativeAngle = (i.toFloat() / samplingPoints) * 2 * PI
        val absoluteAngle = relativeAngle + phaseOffset
        val currentAmplitude = if (pointPos == PointPos.Tip) amplitude else -amplitude
        val r = baseRadius + currentAmplitude * cos(relativeAngle * waveCount).toFloat()

        val x = center.x + r * cos(absoluteAngle).toFloat()
        val y = center.y + r * sin(absoluteAngle).toFloat()

        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

fun calculateSnappedIndex(
    touchPos: Offset,
    center: Offset,
    optionsCount: Int
): Int {
    var angle = atan2(touchPos.y - center.y, touchPos.x - center.x).toDouble() + PI / 2
    if (angle < 0) angle += 2 * PI
    val index = (angle / (2 * PI) * optionsCount).roundToInt()
    return index % optionsCount
}

@Composable
fun WavyoidDial(
    modifier: Modifier = Modifier,
    configs: List<WavyoidConfig<out Any>>,
    isFocusMode: Boolean = true,
    coreTextFormatter: (List<Any?>) -> String,
    onValuesChange: (layerIndex: Int, newIndex: Int, selectedValue: Any) -> Unit
) {
    val selectedIndices = remember {
        mutableStateListOf(*configs.map { it.initialSelectedIndex }.toTypedArray())
    }
    var focusedLayerIndex by remember { mutableIntStateOf(0) }

    BoxWithConstraints(modifier = modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        val center = Offset(constraints.maxWidth / 2f, constraints.maxHeight / 2f)
        val maxAvailableRadius = constraints.maxWidth / 2f
        val density = LocalDensity.current
        val coreRadius = maxAvailableRadius * 0.3f
        val layerSpacing = (maxAvailableRadius - coreRadius) / configs.size

        val drawOrder = remember(focusedLayerIndex, configs.size) {
            (0 until configs.size).sortedBy { it == focusedLayerIndex }
        }

        drawOrder.forEach { index ->
            key(index) {
                val config = configs[index]
                val isFocused = !isFocusMode || focusedLayerIndex == index
                val currentRadius = maxAvailableRadius - (index * layerSpacing) - (config.amplitude)

                WavyoidLayer(
                    config = config,
                    radius = currentRadius,
                    selectedIndex = selectedIndices[index],
                    isFocused = isFocused,
                    center = center,
                    onIndexChange = { newIdx ->
                        selectedIndices[index] = newIdx
                        onValuesChange(index, newIdx, config.options[newIdx])
                    }
                )
            }
        }

        val currentSelections = configs.mapIndexed { index, config -> config.options[selectedIndices[index]] }
        val coreText = coreTextFormatter(currentSelections)
        val coreDiameterDp = with(density) { (coreRadius * 2).toDp() }

        Box(
            modifier = Modifier
                .size(coreDiameterDp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable {
                    if (isFocusMode) {
                        focusedLayerIndex = (focusedLayerIndex + 1) % configs.size
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = coreText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun <T> WavyoidLayer(
    config: WavyoidConfig<T>,
    radius: Float,
    selectedIndex: Int,
    isFocused: Boolean,
    center: Offset,
    onIndexChange: (Int) -> Unit
) {
    val waveCount = config.options.size
    val strokeColor = if (config.color != Color.Unspecified) config.color else MaterialTheme.colorScheme.primary

    val updatedOnIndexChange by rememberUpdatedState(onIndexChange)
    val updatedSelectedIndex by rememberUpdatedState(selectedIndex)

    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isFocused) 1.03f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val targetAlpha = if (isFocused) 1f else 0.15f
    val grayscaleMatrix = remember(isFocused) {
        ColorMatrix().apply {
            if (isFocused) setToSaturation(1f) else setToSaturation(0f)
        }
    }

    val interactionModifier = if (isFocused) {
        Modifier.pointerInput(waveCount, center) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    val pressedChanges = event.changes.filter { it.pressed }

                    if (pressedChanges.isEmpty()) continue

                    val activeChange = pressedChanges.maxByOrNull { it.uptimeMillis }

                    if (activeChange != null) {
                        val newIndex = calculateSnappedIndex(activeChange.position, center, waveCount)
                        if (newIndex != updatedSelectedIndex) {
                            activeChange.consume()
                            updatedOnIndexChange(newIndex)
                        }
                    }
                }
            }
        }
    } else {
        Modifier
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .scale(scale)
            .alpha(targetAlpha)
            .then(interactionModifier)
    ) {
        val path = createWavyoidPath(
            center = center,
            baseRadius = radius,
            waveCount = waveCount,
            amplitude = config.amplitude,
            pointPos = config.pointPos,
            customSamplingPoints = config.customSamplingPoints
        )

        drawPath(
            path = path,
            color = strokeColor,
            style = Stroke(width = config.strokeWidth.toPx()),
            colorFilter = ColorFilter.colorMatrix(grayscaleMatrix)
        )

        val indicatorAngle = (selectedIndex.toFloat() / waveCount) * 2 * PI - PI / 2
        val indicatorR = radius + if (config.pointPos == PointPos.Tip) config.amplitude else -config.amplitude

        val indicatorPos = Offset(
            x = center.x + cos(indicatorAngle).toFloat() * indicatorR,
            y = center.y + sin(indicatorAngle).toFloat() * indicatorR
        )

        drawCircle(
            color = strokeColor,
            radius = config.strokeWidth.toPx() * (if (isFocused) 3f else 1.5f),
            center = indicatorPos,
            colorFilter = ColorFilter.colorMatrix(grayscaleMatrix)
        )
    }
}