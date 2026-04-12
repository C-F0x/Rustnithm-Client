package org.cf0x.rustnithm.Bon.Section

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.cf0x.rustnithm.Bon.BonDialogScaffold
import org.cf0x.rustnithm.Bon.PointPos
import org.cf0x.rustnithm.Bon.SettingsGroup
import org.cf0x.rustnithm.Bon.ToggleSettingItem
import org.cf0x.rustnithm.Bon.WavyoidConfig
import org.cf0x.rustnithm.Bon.WavyoidDial
import org.cf0x.rustnithm.R

@Composable
fun FlickSection(
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
    val errorModifier = if (isPhysicsInvalid) {
        Modifier.border(
            BorderStroke(2.dp, MaterialTheme.colorScheme.error),
            MaterialTheme.shapes.extraLarge
        )
    } else Modifier

    SettingsGroup(
        title = stringResource(R.string.flick_physics_title),
        modifier = errorModifier
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.weight(1f)) {
                    ToggleSettingItem(
                        label = stringResource(R.string.flick_once),
                        supportingText = null,
                        checked = flickOnce,
                        onCheckedChange = onFlickOnceChange
                    )
                }

                VerticalDivider(
                    modifier = Modifier.height(32.dp).padding(horizontal = 8.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                ListItem(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onFormulaDialogToggle(true) },
                    headlineContent = { Text(stringResource(R.string.tutorial)) },
                    trailingContent = {
                        Icon(
                            Icons.AutoMirrored.Outlined.HelpOutline,
                            contentDescription = stringResource(R.string.show_formula),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )

            ValueDialItemExtended(stringResource(R.string.trigger_threshold), flickThreshold, 0..160, 20, onFlickThresholdChange)

            Row(Modifier.fillMaxWidth()) {
                Box(Modifier.weight(1f)) {
                    ValueDialItemExtended(stringResource(R.string.eq_plus), flickEqualizerPlus, 0..40, 5, onFlickEqualizerPlusChange)
                }
                Box(Modifier.weight(1f)) {
                    ValueDialItemExtended(stringResource(R.string.eq_minus), flickEqualizerMinus, 0..40, 5, onFlickEqualizerMinusChange)
                }
            }

            Row(Modifier.fillMaxWidth()) {
                Box(Modifier.weight(1f)) {
                    ValueDialItemExtended(stringResource(R.string.step_up), flickUp, 0..100, 10, onFlickUpChange)
                }
                Box(Modifier.weight(1f)) {
                    ValueDialItemExtended(stringResource(R.string.step_down), flickDown, 0..100, 10, onFlickDownChange)
                }
            }

            ValueDialItemExtended(stringResource(R.string.flick_zones), flickZoneNum, 0..64, 16, onFlickZoneNumChange)

            if (isPhysicsInvalid) {
                Text(
                    text = stringResource(R.string.invalid_physics_warning),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showFormulaDialog) {
        FormulaDialog(onDismiss = { onFormulaDialogToggle(false) })
    }
}

@Composable
fun ValueDialItemExtended(
    label: String,
    value: Int,
    range: IntRange,
    majorStep: Int,
    onValueChange: (Int) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    ListItem(
        modifier = Modifier.clickable { showDialog = true },
        headlineContent = { Text(label) },
        trailingContent = {
            Text(value.toString(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )

    if (showDialog) {
        PhysicsDialDialog(
            title = label,
            initialValue = value,
            range = range,
            majorStep = majorStep,
            onDismiss = { showDialog = false },
            onConfirm = {
                onValueChange(it)
                showDialog = false
            }
        )
    }
}

@Composable
fun PhysicsDialDialog(
    title: String,
    initialValue: Int,
    range: IntRange,
    majorStep: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var currentValue by remember { mutableIntStateOf(initialValue.coerceIn(range)) }
    val primaryColor = MaterialTheme.colorScheme.primary

    val dialConfigs = remember(range, majorStep) {
        val majorOptions = (range.first..range.last step majorStep).toList()
        val minorOptions = (0 until majorStep).toList()
        listOf(
            WavyoidConfig(
                options = majorOptions,
                initialSelectedIndex = ((currentValue - range.first) / majorStep).coerceAtLeast(0),
                color = primaryColor,
                amplitude = 18f
            ),
            WavyoidConfig(
                options = minorOptions,
                initialSelectedIndex = (currentValue - range.first) % majorStep,
                color = primaryColor.copy(alpha = 0.6f),
                amplitude = 10f,
                pointPos = PointPos.Valley
            )
        )
    }

    BonDialogScaffold(
        title = title,
        onDismiss = onDismiss,
        onConfirm = { onConfirm(currentValue) }
    ) {
        WavyoidDial(
            modifier = Modifier
                .size(280.dp)
                .align(Alignment.CenterHorizontally),
            configs = dialConfigs,
            isFocusMode = true,
            coreTextFormatter = { _ -> currentValue.toString() },
            onValuesChange = { layerIndex, _, selectedValue ->
                val selectedInt = selectedValue as Int
                currentValue = if (layerIndex == 0) {
                    (selectedInt + ((currentValue - range.first) % majorStep) + range.first).coerceIn(range)
                } else {
                    (((currentValue - range.first) / majorStep) * majorStep + selectedInt + range.first).coerceIn(range)
                }
            }
        )
        Text(
            text = stringResource(R.string.dial_coarse_fine_hint),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
fun FormulaDialog(onDismiss: () -> Unit) {
    BonDialogScaffold(
        title = stringResource(R.string.physics_diagnostics_title),
        confirmLabel = stringResource(R.string.acknowledge),
        onDismiss = onDismiss,
        onConfirm = onDismiss
    ) {
        FormulaItem(stringResource(R.string.formula_cond_z_gt), stringResource(R.string.formula_result_h_plus_u))
        FormulaItem(stringResource(R.string.formula_cond_z_lt), stringResource(R.string.formula_result_h_minus_d))
        FormulaItem(stringResource(R.string.formula_cond_delta_gte_t), stringResource(R.string.formula_result_signal))
        FormulaItem(stringResource(R.string.formula_cond_delta_pos), stringResource(R.string.formula_result_h_minus_eq))
        FormulaItem(stringResource(R.string.formula_cond_delta_neg), stringResource(R.string.formula_result_h_plus_eq))

        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.physics_formula_note),
            style = MaterialTheme.typography.labelSmall,
            lineHeight = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun FormulaItem(condition: String, result: String) {
    Column {
        Text(condition, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(result, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}