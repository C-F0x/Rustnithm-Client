package org.cf0x.rustnithm.Bon

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.cf0x.rustnithm.Data.DataManager
import org.cf0x.rustnithm.Data.Haptic

class Config(
    application: Application,
    private val dataManager: DataManager,
    private val haptic: Haptic
) : AndroidViewModel(application) {
    private val context get() = getApplication<Application>()
    val language: StateFlow<String> = dataManager.language
    val themeMode: StateFlow<Int> = dataManager.themeMode
    val useDynamicColor: StateFlow<Boolean> = dataManager.useDynamicColor
    val useExpressive: StateFlow<Boolean> = dataManager.useExpressive
    val seedColor: StateFlow<Long> = dataManager.seedColor
    val percentPage: StateFlow<Float> = dataManager.percentPage
    val multiA: StateFlow<Float> = dataManager.multiA
    val multiS: StateFlow<Float> = dataManager.multiS
    val airMode: StateFlow<Int> = dataManager.airMode
    val enableVibration: StateFlow<Boolean> = dataManager.enableVibration
    val accessCodes: StateFlow<String> = dataManager.accessCodes
    val sendFrequency: StateFlow<Int> = dataManager.sendFrequency

    val flickThreshold: StateFlow<Int> = dataManager.flickThreshold
    val flickEqualizerPlus: StateFlow<Int> = dataManager.flickEqualizerPlus
    val flickEqualizerMinus: StateFlow<Int> = dataManager.flickEqualizerMinus
    val flickUp: StateFlow<Int> = dataManager.flickUp
    val flickDown: StateFlow<Int> = dataManager.flickDown
    val flickZoneNum: StateFlow<Int> = dataManager.flickZoneNum
    val flickOnce: StateFlow<Boolean> = dataManager.flickOnce

    var textFieldValue by mutableStateOf("")
    var isError by mutableStateOf(false)
    var passwordVisible by mutableStateOf(false)
    var showColorPickerDialog by mutableStateOf(false)
    var showInfoDialog by mutableStateOf(false)
    var showResetDialog by mutableStateOf(false)
    var showFormulaDialog by mutableStateOf(false)
    var frequencyInput by mutableFloatStateOf(500f)

    val isPhysicsInvalid: StateFlow<Boolean> = combine(
        flickThreshold,
        flickEqualizerPlus,
        flickEqualizerMinus
    ) { threshold, plus, minus ->
        plus >= threshold || minus >= threshold
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    fun initStates(currentAccessCodes: String, currentFrequency: Int) {
        textFieldValue = currentAccessCodes
        frequencyInput = currentFrequency.toFloat()
        if (!haptic.isSupportVibration(context) && enableVibration.value) {
            dataManager.updateEnableVibration(false)
        }
    }

    fun updateLanguage(lang: String) = dataManager.updateLanguage(lang)
    fun updateTheme(index: Int) = dataManager.updateThemeMode(index)
    fun updateDynamicColor(enabled: Boolean) = dataManager.updateUseDynamicColor(enabled)
    fun updateUseExpressive(enabled: Boolean) = dataManager.updateUseExpressive(enabled)
    fun updateSeedColor(color: Long) = dataManager.updateSeedColor(color)
    fun updatePercent(value: Float) = dataManager.updatePercentPage(value)
    fun updateSensitivityA(value: Float) = dataManager.updateMultiA(value)
    fun updateSensitivityS(value: Float) = dataManager.updateMultiS(value)
    fun updateAirMode(value: Int) = dataManager.updateAirMode(value)

    fun updateFlickThreshold(value: Int) {
        if (value > flickEqualizerPlus.value && value > flickEqualizerMinus.value) {
            dataManager.updateFlickThreshold(value)
        }
    }

    fun updateFlickEqualizerPlus(value: Int) {
        if (value < flickThreshold.value) {
            dataManager.updateFlickEqualizerPlus(value)
        }
    }

    fun updateFlickEqualizerMinus(value: Int) {
        if (value < flickThreshold.value) {
            dataManager.updateFlickEqualizerMinus(value)
        }
    }

    fun updateFlickUp(value: Int) = dataManager.updateFlickUp(value)
    fun updateFlickDown(value: Int) = dataManager.updateFlickDown(value)
    fun updateFlickZoneNum(value: Int) = dataManager.updateFlickZoneNum(value)
    fun updateFlickOnce(enabled: Boolean) = dataManager.updateFlickOnce(enabled)

    fun toggleVibration(enabled: Boolean) {
        val targetState = if (haptic.isSupportVibration(context)) enabled else false
        dataManager.updateEnableVibration(targetState)
        if (!targetState) haptic.stop()
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
        dataManager.updateSendFrequency(frequencyInput.toInt())
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