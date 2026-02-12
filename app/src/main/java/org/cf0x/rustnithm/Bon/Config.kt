package org.cf0x.rustnithm.Bon

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.cf0x.rustnithm.Data.DataManager
import org.cf0x.rustnithm.Data.Haptic

class BonViewModel(
    private val dataManager: DataManager,
    private val haptic: Haptic,
    private val context: Context
) : ViewModel() {

    val themeMode: StateFlow<Int> = dataManager.themeMode
    val seedColor: StateFlow<Long> = dataManager.seedColor
    val percentPage: StateFlow<Float> = dataManager.percentPage
    val multiA: StateFlow<Float> = dataManager.multiA
    val multiS: StateFlow<Float> = dataManager.multiS
    val airMode: StateFlow<Int> = dataManager.airMode
    val enableVibration: StateFlow<Boolean> = dataManager.enableVibration
    val accessCodes: StateFlow<String> = dataManager.accessCodes
    val sendFrequency: StateFlow<Int> = dataManager.sendFrequency

    var textFieldValue by mutableStateOf("")
    var isError by mutableStateOf(false)
    var passwordVisible by mutableStateOf(false)
    var showColorPickerDialog by mutableStateOf(false)
    var showInfoDialog by mutableStateOf(false)
    var showResetDialog by mutableStateOf(false)

    var frequencyInput by mutableFloatStateOf(50f)

    fun initStates(currentAccessCodes: String, currentFrequency: Int) {
        textFieldValue = currentAccessCodes
        frequencyInput = currentFrequency.coerceIn(50, 1000).toFloat()
    }

    fun updateTheme(index: Int) = dataManager.updateThemeMode(index)

    fun updateSeedColor(color: Long) = dataManager.updateSeedColor(color)

    fun updatePercent(value: Float) = dataManager.updatePercentPage(value)

    fun updateSensitivityA(value: Float) = dataManager.updateMultiA(value)

    fun updateSensitivityS(value: Float) = dataManager.updateMultiS(value)

    fun updateAirMode(value: Int) = dataManager.updateAirMode(value)
    fun toggleVibration(enabled: Boolean) {
        dataManager.updateEnableVibration(enabled)
        if (!enabled) haptic.stop()
    }

    fun saveAccessCode() {
        val isValid = textFieldValue.length == 20 && textFieldValue.all { it.isDigit() }
        if (isValid) {
            isError = false
            dataManager.updateAccessCodes(textFieldValue)
        } else {
            isError = true
        }
    }

    fun saveFrequency() {
        val freq = frequencyInput.toInt()
        dataManager.updateSendFrequency(freq)
    }

    fun handleImport(uri: Uri) {
        val mimeType = context.contentResolver.getType(uri)
        if (mimeType == "application/json") {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val json = stream.bufferedReader().use { r -> r.readText() }
                dataManager.applySkinConfig(json)
            }
        } else if (mimeType?.startsWith("image/") == true) {
            dataManager.updateBackgroundAndPalette(uri, context)
        }
    }

    fun resetBackground() {
        viewModelScope.launch {
            dataManager.resetBackgroundAndSkin()
        }
    }

    fun resetAllSettings() {
        dataManager.resetToDefaults()
        showResetDialog = false
    }
}