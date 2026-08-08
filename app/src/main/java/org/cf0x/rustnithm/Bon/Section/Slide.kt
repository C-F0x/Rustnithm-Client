package org.cf0x.rustnithm.Bon.Section

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.cf0x.rustnithm.Bon.BonMath
import org.cf0x.rustnithm.Bon.SettingsGroup
import org.cf0x.rustnithm.Bon.SettingSliderItem
import org.cf0x.rustnithm.R

/**
 * Slide (trackpad-style sliding) sensitivity. Note: slide = 滑动,
 * slider = 触控板, flick = 轻扫.
 */
@Composable
fun SlideSection(
    multiS: Float,
    onSensitivitySChange: (Float) -> Unit
) {
    SettingsGroup(icon = Icons.Outlined.TouchApp, title = stringResource(R.string.slide_section_title)) {
        SettingSliderItem(
            title = stringResource(R.string.slide_sensitivity),
            value = multiS,
            valueRange = 0f..0.5f,
            onValueChange = onSensitivitySChange,
            displayValue = BonMath.formatSensitivity(multiS)
        )
    }
}
