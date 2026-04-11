package org.cf0x.rustnithm.Bon

import android.app.Application
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.cf0x.rustnithm.Data.DataManager
import org.cf0x.rustnithm.Data.Haptic

@Composable
fun Bon() {
    val context = LocalContext.current
    val haptic = remember { Haptic.getInstance() }

    val isHardwareSupportVibration = remember {
        haptic.isSupportVibration(context)
    }

    val dataManager: DataManager = viewModel(factory = DataManager.Factory(context))

    val config: Config = remember {
        Config(
            context.applicationContext as Application,
            dataManager,
            haptic
        )
    }

    val language by config.language.collectAsState(initial = "system")
    val themeMode by config.themeMode.collectAsState(initial = 0)
    val useDynamicColor by config.useDynamicColor.collectAsState(initial = true)
    val useExpressive by config.useExpressive.collectAsState(initial = false)
    val seedColorLong by config.seedColor.collectAsState(initial = 0L)
    val percentPage by config.percentPage.collectAsState(initial = 0.5f)
    val multiA by config.multiA.collectAsState(initial = 0.05f)
    val multiS by config.multiS.collectAsState(initial = 0.05f)
    val airMode by config.airMode.collectAsState(initial = 1)
    val enableVibration by config.enableVibration.collectAsState(initial = false)
    val accessCodes by config.accessCodes.collectAsState(initial = "")
    val sendFrequency by config.sendFrequency.collectAsState(initial = 500)

    val flickThreshold by config.flickThreshold.collectAsState(initial = 40)
    val flickEqualizerPlus by config.flickEqualizerPlus.collectAsState(initial = 1)
    val flickEqualizerMinus by config.flickEqualizerMinus.collectAsState(initial = 1)
    val flickUp by config.flickUp.collectAsState(initial = 1)
    val flickDown by config.flickDown.collectAsState(initial = 1)
    val flickZoneNum by config.flickZoneNum.collectAsState(initial = 32)
    val flickOnce by config.flickOnce.collectAsState(initial = false)
    val isPhysicsInvalid by config.isPhysicsInvalid.collectAsState(initial = false)

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

    SettingsScreen(
        language = language,
        onLanguageChange = { config.updateLanguage(it) },
        themeMode = themeMode,
        useDynamicColor = useDynamicColor,
        onDynamicColorChange = { config.updateDynamicColor(it) },
        useExpressive = useExpressive,
        onExpressiveChange = { config.updateUseExpressive(it) },
        seedColorLong = seedColorLong,
        percentPage = percentPage,
        multiA = multiA,
        multiS = multiS,
        enableVibration = if (isHardwareSupportVibration) enableVibration else false,
        isVibrationHardwareSupported = isHardwareSupportVibration,

        accessCodeValue = config.textFieldValue,
        isAccessCodeError = config.isError,
        passwordVisible = config.passwordVisible,
        frequencyValue = config.frequencyInput,
        airMode = airMode,

        flickThreshold = flickThreshold,
        flickEqualizerPlus = flickEqualizerPlus,
        flickEqualizerMinus = flickEqualizerMinus,
        flickUp = flickUp,
        flickDown = flickDown,
        flickZoneNum = flickZoneNum,
        flickOnce = flickOnce,
        onFlickOnceChange = { config.updateFlickOnce(it) },

        isPhysicsInvalid = isPhysicsInvalid,
        showFormulaDialog = config.showFormulaDialog,
        onFormulaDialogToggle = { config.showFormulaDialog = it },

        onInfoClick = { config.showInfoDialog = true },
        onThemeChange = { config.updateTheme(it) },
        onColorPickerOpen = { config.showColorPickerDialog = true },
        onPercentChange = { config.updatePercent(it) },
        onSensitivityAChange = { config.updateSensitivityA(it) },
        onSensitivitySChange = { config.updateSensitivityS(it) },
        onAirModeChange = { config.updateAirMode(it) },

        onFlickThresholdChange = { config.updateFlickThreshold(it) },
        onFlickEqualizerPlusChange = { config.updateFlickEqualizerPlus(it) },
        onFlickEqualizerMinusChange = { config.updateFlickEqualizerMinus(it) },
        onFlickUpChange = { config.updateFlickUp(it) },
        onFlickDownChange = { config.updateFlickDown(it) },
        onFlickZoneNumChange = { config.updateFlickZoneNum(it) },

        onFrequencyValueChange = { config.frequencyInput = it },
        onFrequencySave = { config.saveFrequency() },
        onAccessCodeValueChange = { config.textFieldValue = it },
        onAccessCodeToggleVisible = { config.passwordVisible = !config.passwordVisible },
        onAccessCodeSave = { config.saveAccessCode() },

        onVibrationChange = { if (isHardwareSupportVibration) config.toggleVibration(it) },

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