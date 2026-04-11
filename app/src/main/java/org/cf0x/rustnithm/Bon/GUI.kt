package org.cf0x.rustnithm.Bon

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.cf0x.rustnithm.Bon.Section.AboutSection
import org.cf0x.rustnithm.Bon.Section.AccesscodeSection
import org.cf0x.rustnithm.Bon.Section.AppearanceSection
import org.cf0x.rustnithm.Bon.Section.FlickSection
import org.cf0x.rustnithm.Bon.Section.InteractionSection

@Composable
fun SettingsScreen(
    language: String,
    onLanguageChange: (String) -> Unit,
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
    useExpressive: Boolean,
    onExpressiveChange: (Boolean) -> Unit,
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                IconButton(onClick = onInfoClick) {
                    Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        item {
            AboutSection(
                language = language,
                onLanguageChange = onLanguageChange
            )
        }

        item {
            AppearanceSection(
                themeMode = themeMode,
                useDynamicColor = useDynamicColor,
                useExpressive = useExpressive,
                seedColorLong = seedColorLong,
                onThemeChange = onThemeChange,
                onDynamicColorChange = onDynamicColorChange,
                onExpressiveChange = onExpressiveChange,
                onColorPickerOpen = onColorPickerOpen
            )
        }

        item {
            InteractionSection(
                percentPage = percentPage,
                multiA = multiA,
                multiS = multiS,
                airMode = airMode,
                frequencyValue = frequencyValue,
                enableVibration = enableVibration,
                isVibrationHardwareSupported = isVibrationHardwareSupported,
                onPercentChange = onPercentChange,
                onSensitivityAChange = onSensitivityAChange,
                onSensitivitySChange = onSensitivitySChange,
                onAirModeChange = onAirModeChange,
                onFrequencyValueChange = onFrequencyValueChange,
                onFrequencySave = onFrequencySave,
                onVibrationChange = onVibrationChange
            )
        }

        item {
            AnimatedVisibility(
                visible = airMode == 2,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                FlickSection(
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

        item {
            AccesscodeSection(
                accessCodeValue = accessCodeValue,
                isAccessCodeError = isAccessCodeError,
                passwordVisible = passwordVisible,
                onAccessCodeValueChange = onAccessCodeValueChange,
                onAccessCodeToggleVisible = onAccessCodeToggleVisible,
                onAccessCodeSave = onAccessCodeSave
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilledTonalButton(
                    onClick = onImportClick,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.large
                ) {
                    Icon(Icons.Default.FileDownload, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Import Skin")
                }
                OutlinedButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Icon(Icons.Default.DeleteForever, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Clear Skin")
                }
            }
        }

        item {
            Button(
                onClick = onResetAllClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            ) {
                Icon(Icons.Default.Warning, null)
                Spacer(Modifier.width(8.dp))
                Text("Factory Reset All Settings")
            }
        }
    }
}