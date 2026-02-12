package org.cf0x.rustnithm.Bon

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.cf0x.rustnithm.Data.DataManager
import org.cf0x.rustnithm.Data.Haptic

@Composable
fun Bon() {
    val context = LocalContext.current
    val haptic = remember { Haptic.getInstance() }

    val dataManager: DataManager = viewModel(factory = DataManager.Factory(context))
    val config: BonViewModel = remember {
        BonViewModel(dataManager, haptic, context)
    }

    val themeMode by config.themeMode.collectAsState()
    val seedColorLong by config.seedColor.collectAsState()
    val percentPage by config.percentPage.collectAsState()
    val multiA by config.multiA.collectAsState()
    val multiS by config.multiS.collectAsState()
    val airMode by config.airMode.collectAsState()
    val enableVibration by config.enableVibration.collectAsState()
    val accessCodes by config.accessCodes.collectAsState()
    val sendFrequency by config.sendFrequency.collectAsState()

    LaunchedEffect(accessCodes, sendFrequency) {
        config.initStates(accessCodes, sendFrequency)
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { config.handleImport(it) }
    }

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val topAppBarHeight = 64.dp
    val contentPadding = androidx.compose.foundation.layout.PaddingValues(
        start = 16.dp, end = 16.dp,
        top = statusBarHeight + topAppBarHeight, bottom = 16.dp
    )

    val defaultSeedColor = MaterialTheme.colorScheme.primary.toArgb().toLong()

    SettingsScreen(
        themeMode = themeMode,
        seedColorLong = seedColorLong,
        percentPage = percentPage,
        multiA = multiA,
        multiS = multiS,
        enableVibration = enableVibration,
        accessCodeValue = config.textFieldValue,
        isAccessCodeError = config.isError,
        passwordVisible = config.passwordVisible,
        frequencyValue = config.frequencyInput,
        airMode = airMode,

        onInfoClick = { config.showInfoDialog = true },
        onThemeChange = { config.updateTheme(it) },
        onColorPickerOpen = { config.showColorPickerDialog = true },
        onColorReset = { config.updateSeedColor(defaultSeedColor) },
        onPercentChange = { config.updatePercent(it) },
        onSensitivityAChange = { config.updateSensitivityA(it) },
        onSensitivitySChange = { config.updateSensitivityS(it) },
        onAirModeChange = { config.updateAirMode(it) },
        onFrequencyValueChange = { config.frequencyInput = it },
        onFrequencySave = { config.saveFrequency() },
        onAccessCodeValueChange = { config.textFieldValue = it },
        onAccessCodeToggleVisible = { config.passwordVisible = !config.passwordVisible },
        onAccessCodeSave = { config.saveAccessCode() },
        onVibrationChange = { config.toggleVibration(it) },
        onImportClick = { filePickerLauncher.launch(arrayOf("application/json", "image/*")) },
        onDeleteClick = { config.resetBackground() },
        onResetAllClick = { config.showResetDialog = true },
        contentPadding = contentPadding,
    )
    if (config.showColorPickerDialog) {
        ColorPickerDialog(
            initialColor = Color(seedColorLong),
            onDismiss = { config.showColorPickerDialog = false },
            onConfirm = {
                config.updateSeedColor(it)
                config.showColorPickerDialog = false
            }
        )
    }

    if (config.showInfoDialog) {
        AlertDialog(
            onDismissRequest = { config.showInfoDialog = false },
            title = { Text("About Rustnithm") },
            text = { Text("Customizable rhythm controller.") },
            confirmButton = {
                TextButton(onClick = { config.showInfoDialog = false }) { Text("OK") }
            }
        )
    }

    if (config.showResetDialog) {
        AlertDialog(
            onDismissRequest = { config.showResetDialog = false },
            title = { Text("Reset All?") },
            text = { Text("This will restore all configurations (including keys and skin) to factory defaults.") },
            confirmButton = {
                Button(
                    onClick = { config.resetAllSettings() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) { Text("Reset Now") }
            },
            dismissButton = {
                TextButton(onClick = { config.showResetDialog = false }) { Text("Cancel") }
            }
        )
    }
}