package org.cf0x.rustnithm.Bon.Section

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.cf0x.rustnithm.Bon.BonMath
import org.cf0x.rustnithm.Bon.ExpressiveSlider
import org.cf0x.rustnithm.Bon.SettingsGroup
import org.cf0x.rustnithm.Bon.ToggleSettingItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InteractionSection(
    percentPage: Float,
    multiA: Float,
    multiS: Float,
    airMode: Int,
    frequencyValue: Float,
    enableVibration: Boolean,
    isVibrationHardwareSupported: Boolean,
    onPercentChange: (Float) -> Unit,
    onSensitivityAChange: (Float) -> Unit,
    onSensitivitySChange: (Float) -> Unit,
    onAirModeChange: (Int) -> Unit,
    onFrequencyValueChange: (Float) -> Unit,
    onFrequencySave: () -> Unit,
    onVibrationChange: (Boolean) -> Unit
) {
    SettingsGroup(title = "Interaction") {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            ExpressiveSlider(
                label = "Split Ratio",
                value = percentPage,
                valueRange = 0.1f..0.9f,
                onValueChange = onPercentChange,
                displayValue = BonMath.formatPercent(percentPage)
            )
            ExpressiveSlider(
                label = "Air Sensitivity",
                value = multiA,
                valueRange = 0f..0.5f,
                onValueChange = onSensitivityAChange,
                displayValue = BonMath.formatSensitivity(multiA)
            )
            ExpressiveSlider(
                label = "Slide Sensitivity",
                value = multiS,
                valueRange = 0f..0.5f,
                onValueChange = onSensitivitySChange,
                displayValue = BonMath.formatSensitivity(multiS)
            )

            Spacer(Modifier.height(8.dp))

            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val options = listOf("Native", "Flick", "Auto")
                    options.forEachIndexed { index, label ->
                        val value = index + 1
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                            onClick = { onAirModeChange(value) },
                            selected = airMode == value
                        ) { Text(label) }
                    }
                }
            }

            ExpressiveSlider(
                label = "Frequency",
                value = frequencyValue,
                valueRange = 50f..1000f,
                onValueChange = onFrequencyValueChange,
                onValueChangeFinished = onFrequencySave,
                displayValue = BonMath.formatFrequency(frequencyValue)
            )

            Spacer(Modifier.height(4.dp))

            ToggleSettingItem(
                label = "Haptic Feedback",
                supportingText = if (isVibrationHardwareSupported) "Tactile response" else "Unsupported",
                checked = enableVibration,
                enabled = isVibrationHardwareSupported,
                onCheckedChange = onVibrationChange
            )
        }
    }
}