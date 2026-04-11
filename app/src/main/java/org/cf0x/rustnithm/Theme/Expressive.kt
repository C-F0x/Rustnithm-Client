package org.cf0x.rustnithm.Theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object ExpressiveMotion {
    fun <T> standard(): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    fun <T> expressive(): SpringSpec<T> = spring(
        dampingRatio = 0.6f,
        stiffness = Spring.StiffnessMediumLow
    )

    fun <T> expressiveBouncy(): SpringSpec<T> = spring(
        dampingRatio = 0.4f,
        stiffness = Spring.StiffnessLow
    )

    fun <T> expressiveFast(): SpringSpec<T> = spring(
        dampingRatio = 0.7f,
        stiffness = Spring.StiffnessMediumLow
    )
}

val ExpressiveShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(36.dp)
)

val StandardShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
)