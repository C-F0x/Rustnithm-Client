package org.cf0x.rustnithm.Bon

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.cf0x.rustnithm.Bon.Section.AboutSection
import org.cf0x.rustnithm.Bon.Section.AppearanceSection
import org.cf0x.rustnithm.Bon.Section.ConnectionSection
import org.cf0x.rustnithm.Bon.Section.IrSensorSection
import org.cf0x.rustnithm.Bon.Section.SlideSection
import org.cf0x.rustnithm.R

/**
 * Bon page: five settings groups in KonamikU style.
 *  1. About / software introduction
 *  2. Appearance (theme, skin, split ratio, haptics, language)
 *  3. Slide sensitivity
 *  4. IR Sensor (native / flick / auto)
 *  5. Connection (network, security, frequency)
 */
@Composable
fun SettingsScreen(
    language: String,
    onLanguageChange: (String) -> Unit,
    themeMode: Int,
    useDynamicColor: Boolean,
    useExpressive: Boolean,
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

    onThemeChange: (Int) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
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

    ipValue: String,
    portValue: String,
    protocolType: Int,
    onIpSaved: (String) -> Unit,
    onPortSaved: (String) -> Unit,
    onProtocolSelect: (Int) -> Unit,

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
            AboutSection()
        }

        item {
            AppearanceSection(
                language = language,
                onLanguageChange = onLanguageChange,
                themeMode = themeMode,
                useDynamicColor = useDynamicColor,
                useExpressive = useExpressive,
                seedColorLong = seedColorLong,
                percentPage = percentPage,
                enableVibration = enableVibration,
                isVibrationHardwareSupported = isVibrationHardwareSupported,
                onThemeChange = onThemeChange,
                onDynamicColorChange = onDynamicColorChange,
                onExpressiveChange = onExpressiveChange,
                onColorPickerOpen = onColorPickerOpen,
                onPercentChange = onPercentChange,
                onVibrationChange = onVibrationChange,
                onImportClick = onImportClick,
                onDeleteClick = onDeleteClick
            )
        }

        item {
            SlideSection(
                multiS = multiS,
                onSensitivitySChange = onSensitivitySChange
            )
        }

        item {
            IrSensorSection(
                airMode = airMode,
                multiA = multiA,
                onAirModeChange = onAirModeChange,
                onSensitivityAChange = onSensitivityAChange,
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

        item {
            ConnectionSection(
                initialIp = ipValue,
                initialPort = portValue,
                protocolType = protocolType,
                accessCodeValue = accessCodeValue,
                isAccessCodeError = isAccessCodeError,
                passwordVisible = passwordVisible,
                frequencyValue = frequencyValue,
                onIpSaved = onIpSaved,
                onPortSaved = onPortSaved,
                onProtocolSelect = onProtocolSelect,
                onAccessCodeValueChange = onAccessCodeValueChange,
                onAccessCodeToggleVisible = onAccessCodeToggleVisible,
                onAccessCodeSave = onAccessCodeSave,
                onFrequencyValueChange = onFrequencyValueChange,
                onFrequencySave = onFrequencySave
            )
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
                Text(stringResource(R.string.factory_reset_all))
            }
        }
    }
}
