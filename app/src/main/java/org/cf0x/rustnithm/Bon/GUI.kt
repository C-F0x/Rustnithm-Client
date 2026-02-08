package org.cf0x.rustnithm.Bon

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeMode: Int,
    seedColorLong: Long,
    percentPage: Float,
    multiA: Float,
    multiS: Float,
    airMode: Int,
    enableVibration: Boolean,
    accessCodeValue: String,
    isAccessCodeError: Boolean,
    passwordVisible: Boolean,
    frequencyValue: String,
    isFrequencyError: Boolean,

    onInfoClick: () -> Unit,
    onThemeChange: (Int) -> Unit,
    onColorPickerOpen: () -> Unit,
    onColorReset: () -> Unit,
    onPercentChange: (Float) -> Unit,
    onSensitivityAChange: (Float) -> Unit,
    onSensitivitySChange: (Float) -> Unit,
    onAirModeChange: (Int) -> Unit,
    onFrequencyValueChange: (String) -> Unit,
    onFrequencySave: () -> Unit,
    onAccessCodeValueChange: (String) -> Unit,
    onAccessCodeToggleVisible: () -> Unit,
    onAccessCodeSave: () -> Unit,
    onVibrationChange: (Boolean) -> Unit,
    onImportClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onResetAllClick: () -> Unit,

    contentPadding: PaddingValues
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = onInfoClick) { Icon(Icons.Default.Info, null) }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
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
                    Spacer(modifier = Modifier.height(20.dp))
                    ListItem(
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { onColorPickerOpen() }
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        headlineContent = { Text("Skin Seed Color") },
                        leadingContent = { Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(seedColorLong))) },
                        trailingContent = { IconButton(onClick = onColorReset) { Icon(Icons.Default.Build, null) } }
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Interaction", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Split Ratio: ${(percentPage * 100).roundToInt()}%")
                    Slider(value = percentPage, onValueChange = onPercentChange, valueRange = 0.1f..0.9f)
                    Text("Air Sensitivity: ${"%.2f".format(multiA)}")
                    Slider(value = multiA, onValueChange = onSensitivityAChange, valueRange = 0f..0.5f)
                    Text("Slide Sensitivity: ${"%.2f".format(multiS)}")
                    Slider(value = multiS, onValueChange = onSensitivitySChange, valueRange = 0f..0.5f)
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Air Mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
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
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Transmission", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = frequencyValue,
                        onValueChange = onFrequencyValueChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Send Frequency (Hz)") },
                        isError = isFrequencyError,
                        placeholder = { Text("1-8000") },
                        trailingIcon = {
                            IconButton(onClick = onFrequencySave) {
                                Icon(
                                    imageVector = Icons.Default.Done,
                                    contentDescription = "Save Frequency",
                                    tint = if (isFrequencyError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        supportingText = {
                            if (isFrequencyError) {
                                Text("Please enter a valid number (1-8000)", color = MaterialTheme.colorScheme.error)
                            } else {
                                Text("Higher frequency reduces latency but increases CPU load.", style = MaterialTheme.typography.bodySmall)
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Security", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = accessCodeValue,
                        onValueChange = onAccessCodeValueChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Access Codes (20 Digits)") },
                        isError = isAccessCodeError,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            Row {
                                IconButton(onClick = onAccessCodeToggleVisible) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle mask"
                                    )
                                }
                                IconButton(onClick = onAccessCodeSave) {
                                    Icon(
                                        imageVector = Icons.Default.Done,
                                        contentDescription = "Save",
                                        tint = if (isAccessCodeError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        },
                        supportingText = {
                            if (isAccessCodeError) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Must be exactly 20 digits", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text("Haptic Feedback") },
                    trailingContent = {
                        Switch(checked = enableVibration, onCheckedChange = onVibrationChange)
                    }
                )
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onImportClick, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Share, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import")
                }
                OutlinedButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.wrapContentSize(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete")
                }
            }
        }

        item {
            Button(
                onClick = onResetAllClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Refresh, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset All Settings")
            }
        }
    }
}

@Composable
fun ColorPickerDialog(
    initialColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val hsv = remember {
        val res = FloatArray(3)
        android.graphics.Color.colorToHSV(initialColor.toArgb(), res)
        res
    }
    var hue by remember { mutableFloatStateOf(hsv[0]) }
    var saturation by remember { mutableFloatStateOf(hsv[1]) }
    var value by remember { mutableFloatStateOf(hsv[2]) }
    val currentColor = remember(hue, saturation, value) {
        Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value)))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pick Seed Color") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(currentColor)
                )
                Slider(value = hue, onValueChange = { hue = it }, valueRange = 0f..360f)
                Slider(value = saturation, onValueChange = { saturation = it }, valueRange = 0f..1f)
                Slider(value = value, onValueChange = { value = it }, valueRange = 0f..1f)
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(currentColor.toArgb().toLong()) }) { Text("Confirm") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}