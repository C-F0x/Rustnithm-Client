package org.cf0x.rustnithm.Bon

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeMode: Int,
    useDynamicColor: Boolean,
    seedColorLong: Long,
    percentPage: Float,
    multiA: Float,
    multiS: Float,
    airMode: Int,
    enableVibration: Boolean,
    isVibrationHardwareSupported: Boolean = true,
    accessCodeValue: String,
    isAccessCodeError: Boolean,
    passwordVisible: Boolean,
    frequencyValue: Float,

    flickThreshold: Int,
    flickEqualizerPlus: Int,
    flickEqualizerMinus: Int,
    flickUp: Int,
    flickDown: Int,
    flickZoneNum: Int,
    flickOnce: Boolean,

    isPhysicsInvalid: Boolean,
    showFormulaDialog: Boolean,
    onFormulaDialogToggle: (Boolean) -> Unit,

    onInfoClick: () -> Unit,
    onThemeChange: (Int) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onColorPickerOpen: () -> Unit,
    onPercentChange: (Float) -> Unit,
    onSensitivityAChange: (Float) -> Unit,
    onSensitivitySChange: (Float) -> Unit,
    onAirModeChange: (Int) -> Unit,
    onFrequencyValueChange: (Float) -> Unit,
    onFrequencySave: () -> Unit,
    onAccessCodeValueChange: (String) -> Unit,
    onAccessCodeToggleVisible: () -> Unit,
    onAccessCodeSave: () -> Unit,
    onVibrationChange: (Boolean) -> Unit,
    onImportClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onResetAllClick: () -> Unit,

    onFlickThresholdChange: (Int) -> Unit,
    onFlickEqualizerPlusChange: (Int) -> Unit,
    onFlickEqualizerMinusChange: (Int) -> Unit,
    onFlickUpChange: (Int) -> Unit,
    onFlickDownChange: (Int) -> Unit,
    onFlickZoneNumChange: (Int) -> Unit,
    onFlickOnceChange: (Boolean) -> Unit,

    contentPadding: PaddingValues
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                IconButton(onClick = onInfoClick) {
                    Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        item {
            SettingsGroup(title = "Appearance") {
                Column(modifier = Modifier.padding(16.dp)) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        val options = listOf("Light", "Dark", "System")
                        options.forEachIndexed { index, label ->
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                                onClick = { onThemeChange(index) },
                                selected = themeMode == index
                            ) { Text(label) }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    ToggleSettingItem("Dynamic Color", "Material You tones", useDynamicColor, onCheckedChange = onDynamicColorChange)

                    val isCustomEnabled = !useDynamicColor
                    val customBackgroundColor = if (isCustomEnabled) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    }


                    ListItem(
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.large)
                            .clickable(enabled = isCustomEnabled) { onColorPickerOpen() }
                            .background(customBackgroundColor),
                        headlineContent = {
                            Text(
                                "Skin Seed Color",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isCustomEnabled) 1f else 0.38f)
                            )
                        },
                        leadingContent = {
                            val previewColor = if (isCustomEnabled) Color(seedColorLong) else MaterialTheme.colorScheme.primary
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(previewColor)
                                    .border(
                                        width = if (isCustomEnabled) 1.dp else 1.5.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                        shape = CircleShape
                                    )
                            )
                        },
                        trailingContent = {
                            if (isCustomEnabled) {
                                Icon(
                                    Icons.Default.Palette,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }

        item {
            SettingsGroup(title = "Interaction") {
                ExpressiveSlider("Split Ratio", percentPage, 0.1f..0.9f, onValueChange = onPercentChange, displayValue = "${(percentPage * 100).roundToInt()}%")
                ExpressiveSlider("Air Sensitivity", multiA, 0f..0.5f, onValueChange = onSensitivityAChange, displayValue = "%.2f".format(multiA))
                ExpressiveSlider("Slide Sensitivity", multiS, 0f..0.5f, onValueChange = onSensitivitySChange, displayValue = "%.2f".format(multiS))
            }
        }

        item {
            SettingsGroup(title = "Air Mode") {
                Column(modifier = Modifier.padding(16.dp)) {
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
                ExpressiveSlider("Frequency", frequencyValue, 50f..1000f, onValueChange = onFrequencyValueChange, onValueChangeFinished = onFrequencySave, displayValue = "${frequencyValue.toInt()} Hz")
            }
        }

        item {
            val errorModifier = if (isPhysicsInvalid) {
                Modifier.border(BorderStroke(2.dp, MaterialTheme.colorScheme.error), MaterialTheme.shapes.extraLarge)
            } else Modifier

            SettingsGroup(
                title = "Flick Physics",
                modifier = errorModifier
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.weight(1f)) {
                            ToggleSettingItem(
                                label = "Flick Once",
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
                            headlineContent = { Text("Tutorial") },
                            trailingContent = {
                                Icon(
                                    Icons.AutoMirrored.Outlined.HelpOutline,
                                    contentDescription = "Show Formula",
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

                    ValueDialItemExtended("Trigger Threshold", flickThreshold, 0..160, 20, onFlickThresholdChange)

                    Row(Modifier.fillMaxWidth()) {
                        Box(Modifier.weight(1f)) {
                            ValueDialItemExtended("Eq +", flickEqualizerPlus, 0..40, 5, onFlickEqualizerPlusChange)
                        }
                        Box(Modifier.weight(1f)) {
                            ValueDialItemExtended("Eq -", flickEqualizerMinus, 0..40, 5, onFlickEqualizerMinusChange)
                        }
                    }

                    Row(Modifier.fillMaxWidth()) {
                        Box(Modifier.weight(1f)) {
                            ValueDialItemExtended("Step Up", flickUp, 0..100, 10, onFlickUpChange)
                        }
                        Box(Modifier.weight(1f)) {
                            ValueDialItemExtended("Step Down", flickDown, 0..100, 10, onFlickDownChange)
                        }
                    }

                    ValueDialItemExtended("Flick Zones", flickZoneNum, 0..64, 16, onFlickZoneNumChange)

                    if (isPhysicsInvalid) {
                        Text(
                            text = "Invalid physics parameters detected. Tap tutorial to check formula.",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            SettingsGroup(title = "Security & Haptics") {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = accessCodeValue, onValueChange = onAccessCodeValueChange,
                        modifier = Modifier.fillMaxWidth(), label = { Text("Access Codes (20 Digits)") },
                        isError = isAccessCodeError, shape = MaterialTheme.shapes.large,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            Row {
                                IconButton(onClick = onAccessCodeToggleVisible) { Icon(if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null) }
                                IconButton(onClick = onAccessCodeSave) { Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary) }
                            }
                        },
                        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Spacer(Modifier.height(16.dp))
                    ToggleSettingItem("Haptic Feedback", if (isVibrationHardwareSupported) "Tactile response" else "Unsupported", enableVibration, isVibrationHardwareSupported, onVibrationChange)
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(onClick = onImportClick, modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.large) {
                    Icon(Icons.Default.FileDownload, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Import Skin")
                }
                OutlinedButton(onClick = onDeleteClick, modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.large, colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.tertiary)) {
                    Icon(Icons.Default.DeleteForever, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Clear Skin")
                }
            }
        }

        item {
            Button(
                onClick = onResetAllClick, modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer)
            ) {
                Icon(Icons.Default.Warning, null)
                Spacer(Modifier.width(8.dp))
                Text("Factory Reset All Settings")
            }
        }
    }

    if (showFormulaDialog) {
        FormulaDialog(onDismiss = { onFormulaDialogToggle(false) })
    }
}

@Composable
fun FormulaDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Physics Diagnostics", fontWeight = FontWeight.Bold) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Acknowledge") }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FormulaItem(" If Z > Z_last:", "H_next = H + U")
                FormulaItem(" If Z < Z_last:", "H_next = H - D")
                FormulaItem(" If |Δ| >= T:", "Signal = 1, H = H_default")
                FormulaItem(" If 0 < Δ < T:", "H_next = H - Eq_minus")
                FormulaItem(" If -T < Δ < 0:", "H_next = H + Eq_plus")

                Spacer(Modifier.height(8.dp))
                Text(
                    "Note: Δ = H - 1024. \nSystem will block saving if Eq >= T.",
                    style = MaterialTheme.typography.labelSmall,
                    lineHeight = 16.sp
                )
            }
        }
    )
}

@Composable
fun FormulaItem(condition: String, result: String) {
    Column {
        Text(condition, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(result, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
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
            onDismiss = { },
            onConfirm = {
                onValueChange(it)
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        confirmButton = {
            TextButton(onClick = { onConfirm(currentValue) }) {
                Text("Confirm", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                WavyoidDial(
                    modifier = Modifier.size(280.dp),
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
                    text = "Tap center to switch Coarse/Fine",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}