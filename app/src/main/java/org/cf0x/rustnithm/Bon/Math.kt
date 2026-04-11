package org.cf0x.rustnithm.Bon

import kotlin.math.roundToInt

object BonMath {
    fun formatPercent(value: Float): String {
        return "${(value * 100).roundToInt()}%"
    }

    fun formatSensitivity(value: Float): String {
        return "%.2f".format(value)
    }

    fun formatFrequency(value: Float): String {
        return "${value.toInt()} Hz"
    }

    fun getMajorIndex(currentValue: Int, range: IntRange, majorStep: Int): Int {
        return ((currentValue.coerceIn(range) - range.first) / majorStep).coerceAtLeast(0)
    }

    fun getMinorIndex(currentValue: Int, range: IntRange, majorStep: Int): Int {
        return (currentValue.coerceIn(range) - range.first) % majorStep
    }

    fun calculateDialValue(
        layerIndex: Int,
        selectedValue: Int,
        currentValue: Int,
        range: IntRange,
        majorStep: Int
    ): Int {
        val relativeValue = currentValue.coerceIn(range) - range.first
        return if (layerIndex == 0) {
            (selectedValue + (relativeValue % majorStep) + range.first).coerceIn(range)
        } else {
            ((relativeValue / majorStep) * majorStep + selectedValue + range.first).coerceIn(range)
        }
    }
}