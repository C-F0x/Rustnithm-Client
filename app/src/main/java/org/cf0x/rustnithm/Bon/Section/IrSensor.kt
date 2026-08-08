package org.cf0x.rustnithm.Bon.Section

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.cf0x.rustnithm.Bon.BonMath
import org.cf0x.rustnithm.Bon.SegmentSwitch
import org.cf0x.rustnithm.Bon.SettingsGroup
import org.cf0x.rustnithm.Bon.SettingSliderItem
import org.cf0x.rustnithm.R

/**
 * IR Sensor (红外感应) settings: the air-pad sensing mode selector and its
 * mode-specific tuning.
 *  - native: manual air sensing, air sensitivity slider shown
 *  - flick:  轻扫 (flick) pulse takeover, flick physics shown
 *  - auto:   automatic pulse takeover, no extra tuning
 */
@Composable
fun IrSensorSection(
    airMode: Int,
    multiA: Float,
    onAirModeChange: (Int) -> Unit,
    onSensitivityAChange: (Float) -> Unit,
    flickThreshold: Int,
    flickEqualizerPlus: Int,
    flickEqualizerMinus: Int,
    flickUp: Int,
    flickDown: Int,
    flickZoneNum: Int,
    flickOnce: Boolean,
    isPhysicsInvalid: Boolean,
    showFormulaDialog: Boolean,
    onFlickThresholdChange: (Int) -> Unit,
    onFlickEqualizerPlusChange: (Int) -> Unit,
    onFlickEqualizerMinusChange: (Int) -> Unit,
    onFlickUpChange: (Int) -> Unit,
    onFlickDownChange: (Int) -> Unit,
    onFlickZoneNumChange: (Int) -> Unit,
    onFlickOnceChange: (Boolean) -> Unit,
    onFormulaDialogToggle: (Boolean) -> Unit
) {
    SettingsGroup(icon = Icons.Outlined.Sensors, title = stringResource(R.string.ir_sensor_title)) {
        SegmentSwitch(
            options = listOf(
                stringResource(R.string.air_mode_native),
                stringResource(R.string.air_mode_flick),
                stringResource(R.string.air_mode_auto)
            ),
            selectedIndex = (airMode - 1).coerceIn(0, 2),
            onSelect = { onAirModeChange(it + 1) }
        )

        AnimatedVisibility(
            visible = airMode == 1,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            SettingSliderItem(
                title = stringResource(R.string.air_sensitivity),
                value = multiA,
                valueRange = 0f..0.5f,
                onValueChange = onSensitivityAChange,
                displayValue = BonMath.formatSensitivity(multiA)
            )
        }

        AnimatedVisibility(
            visible = airMode == 2,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            FlickContent(
                flickThreshold = flickThreshold,
                flickEqualizerPlus = flickEqualizerPlus,
                flickEqualizerMinus = flickEqualizerMinus,
                flickUp = flickUp,
                flickDown = flickDown,
                flickZoneNum = flickZoneNum,
                flickOnce = flickOnce,
                isPhysicsInvalid = isPhysicsInvalid,
                showFormulaDialog = showFormulaDialog,
                onFlickThresholdChange = onFlickThresholdChange,
                onFlickEqualizerPlusChange = onFlickEqualizerPlusChange,
                onFlickEqualizerMinusChange = onFlickEqualizerMinusChange,
                onFlickUpChange = onFlickUpChange,
                onFlickDownChange = onFlickDownChange,
                onFlickZoneNumChange = onFlickZoneNumChange,
                onFlickOnceChange = onFlickOnceChange,
                onFormulaDialogToggle = onFormulaDialogToggle
            )
        }
    }
}
