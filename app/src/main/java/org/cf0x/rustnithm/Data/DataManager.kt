package org.cf0x.rustnithm.Data

import android.content.Context
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.core.graphics.toColorInt
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.palette.graphics.Palette
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class SkinConfig(
    val skinName: String = "Default",
    val seedColor: String = "#FF6750A4",
    val themeMode: Int = 2,
    val airWeight: Float = 0.5f,
    val multiA: Float = 0.15f,
    val multiS: Float = 0.15f
)
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "rustnithm_settings")

class DataManager(context: Context) : ViewModel() {

    private val dataStore = context.dataStore

    private object PreferenceKeys {
        val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        val THEME_MODE = intPreferencesKey("theme_mode")
        val ENABLE_VIBRATION = booleanPreferencesKey("enable_vibration")
        val PERCENT_PAGE = floatPreferencesKey("percent_page")
        val MULTI_A = floatPreferencesKey("multi_a")
        val MULTI_S = floatPreferencesKey("multi_s")
        val SEED_COLOR = longPreferencesKey("seed_color")
        val BACKGROUND_IMAGE_PATH = stringPreferencesKey("background_image_path")
        val TARGET_IP = stringPreferencesKey("target_ip")
        val TARGET_PORT = stringPreferencesKey("target_port")
        val ACCESS_CODES = stringPreferencesKey("access_codes")
        val SEND_FREQUENCY = intPreferencesKey("send_frequency")
        val PROTOCOL_TYPE = intPreferencesKey("protocol_type")
        val AIR_MODE = intPreferencesKey("air_mode")
        val FLICK_THRESHOLD = intPreferencesKey("flick_threshold")
        val FLICK_EQUALIZER_PLUS = intPreferencesKey("flick_equalizer_plus")
        val FLICK_EQUALIZER_MINUS = intPreferencesKey("flick_equalizer_minus")
        val FLICK_UP = intPreferencesKey("flick_up")
        val FLICK_DOWN = intPreferencesKey("flick_down")
        val FLICK_ZONE_NUM = intPreferencesKey("flick_zone_num")
        val FLICK_ONCE = booleanPreferencesKey("flick_once")
    }

    private companion object {
        const val DEFAULT_USE_DYNAMIC_COLOR = true
        const val DEFAULT_THEME_MODE = 2
        const val DEFAULT_ENABLE_VIBRATION = true
        const val DEFAULT_PERCENT_PAGE = 0.5f
        const val DEFAULT_MULTI_A = 0.15f
        const val DEFAULT_MULTI_S = 0.15f
        const val DEFAULT_SEED_COLOR = 0xFF6750A4L
        const val DEFAULT_SEND_FREQUENCY = 500
        const val DEFAULT_PROTOCOL_TYPE = 0
        const val DEFAULT_AIR_MODE = 1
        const val DEFAULT_ACCESS_CODES = "12345678901234567890"
        const val DEFAULT_FLICK_THRESHOLD = 80
        const val DEFAULT_FLICK_EQUALIZER_PLUS = 10
        const val DEFAULT_FLICK_EQUALIZER_MINUS = 10
        const val DEFAULT_FLICK_UP = 10
        const val DEFAULT_FLICK_DOWN = 10
        const val DEFAULT_FLICK_ZONE_NUM = 32
        const val DEFAULT_FLICK_ONCE = false
    }

    init {
        viewModelScope.launch {
            dataStore.data.map { preferences ->
                preferences[PreferenceKeys.SEND_FREQUENCY] ?: DEFAULT_SEND_FREQUENCY
            }.collect { frequency ->
                Net.initEngine(frequency)
            }
        }
    }
    val useDynamicColor: StateFlow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[PreferenceKeys.USE_DYNAMIC_COLOR] ?: DEFAULT_USE_DYNAMIC_COLOR
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DEFAULT_USE_DYNAMIC_COLOR)
    val targetIp: StateFlow<String> = dataStore.data
        .map { preferences -> preferences[PreferenceKeys.TARGET_IP] ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val targetPort: StateFlow<String> = dataStore.data
        .map { preferences -> preferences[PreferenceKeys.TARGET_PORT] ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val sendFrequency: StateFlow<Int> = dataStore.data
        .map { preferences -> preferences[PreferenceKeys.SEND_FREQUENCY] ?: DEFAULT_SEND_FREQUENCY }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DEFAULT_SEND_FREQUENCY)
    val backgroundImage: StateFlow<String?> = dataStore.data
        .map { preferences -> preferences[PreferenceKeys.BACKGROUND_IMAGE_PATH] }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val themeMode: StateFlow<Int> = dataStore.data
        .map { preferences -> preferences[PreferenceKeys.THEME_MODE] ?: DEFAULT_THEME_MODE }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DEFAULT_THEME_MODE)

    val enableVibration: StateFlow<Boolean> = dataStore.data
        .map { preferences -> preferences[PreferenceKeys.ENABLE_VIBRATION] ?: DEFAULT_ENABLE_VIBRATION }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DEFAULT_ENABLE_VIBRATION)

    val percentPage: StateFlow<Float> = dataStore.data
        .map { preferences -> preferences[PreferenceKeys.PERCENT_PAGE] ?: DEFAULT_PERCENT_PAGE }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DEFAULT_PERCENT_PAGE)

    val multiA: StateFlow<Float> = dataStore.data
        .map { preferences -> preferences[PreferenceKeys.MULTI_A] ?: DEFAULT_MULTI_A }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DEFAULT_MULTI_A)

    val multiS: StateFlow<Float> = dataStore.data
        .map { preferences -> preferences[PreferenceKeys.MULTI_S] ?: DEFAULT_MULTI_S }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DEFAULT_MULTI_S)

    val seedColor: StateFlow<Long> = dataStore.data
        .map { preferences -> preferences[PreferenceKeys.SEED_COLOR] ?: DEFAULT_SEED_COLOR }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DEFAULT_SEED_COLOR)

    val accessCodes: StateFlow<String> = dataStore.data
        .map { preferences -> preferences[PreferenceKeys.ACCESS_CODES] ?: DEFAULT_ACCESS_CODES }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DEFAULT_ACCESS_CODES)
    val protocolType: StateFlow<Int> = dataStore.data
        .map { preferences -> preferences[PreferenceKeys.PROTOCOL_TYPE] ?: DEFAULT_PROTOCOL_TYPE }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DEFAULT_PROTOCOL_TYPE)
    val airMode: StateFlow<Int> = dataStore.data
        .map { preferences -> preferences[PreferenceKeys.AIR_MODE] ?: DEFAULT_AIR_MODE }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DEFAULT_AIR_MODE)
    val flickThreshold: StateFlow<Int> = dataStore.data
        .map { it[PreferenceKeys.FLICK_THRESHOLD] ?: DEFAULT_FLICK_THRESHOLD }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DEFAULT_FLICK_THRESHOLD)

    val flickEqualizerPlus: StateFlow<Int> = dataStore.data
        .map { it[PreferenceKeys.FLICK_EQUALIZER_PLUS] ?: DEFAULT_FLICK_EQUALIZER_PLUS }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DEFAULT_FLICK_EQUALIZER_PLUS)

    val flickEqualizerMinus: StateFlow<Int> = dataStore.data
        .map { it[PreferenceKeys.FLICK_EQUALIZER_MINUS] ?: DEFAULT_FLICK_EQUALIZER_MINUS }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DEFAULT_FLICK_EQUALIZER_MINUS)

    val flickUp: StateFlow<Int> = dataStore.data
        .map { it[PreferenceKeys.FLICK_UP] ?: DEFAULT_FLICK_UP }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DEFAULT_FLICK_UP)

    val flickDown: StateFlow<Int> = dataStore.data
        .map { it[PreferenceKeys.FLICK_DOWN] ?: DEFAULT_FLICK_DOWN }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DEFAULT_FLICK_DOWN)

    val flickZoneNum: StateFlow<Int> = dataStore.data
        .map { it[PreferenceKeys.FLICK_ZONE_NUM] ?: DEFAULT_FLICK_ZONE_NUM }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DEFAULT_FLICK_ZONE_NUM)

    val flickOnce: StateFlow<Boolean> = dataStore.data
        .map { it[PreferenceKeys.FLICK_ONCE] ?: DEFAULT_FLICK_ONCE }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DEFAULT_FLICK_ONCE)

    fun updateBackgroundAndPalette(uri: Uri, context: Context) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[PreferenceKeys.BACKGROUND_IMAGE_PATH] = uri.toString()
            }
            if (!useDynamicColor.value) {
                try {
                    val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                    Palette.from(bitmap).generate { palette ->
                        val colorInt = palette?.getVibrantColor(
                            palette.getDominantColor(DEFAULT_SEED_COLOR.toInt())
                        )
                        colorInt?.let {
                            updateSeedColor(it.toLong())
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    fun updateUseDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { it[PreferenceKeys.USE_DYNAMIC_COLOR] = enabled }
        }
    }
    fun updateTargetIp(ip: String) {
        viewModelScope.launch {
            dataStore.edit { it[PreferenceKeys.TARGET_IP] = ip }
        }
    }

    fun updateTargetPort(port: String) {
        viewModelScope.launch {
            dataStore.edit { it[PreferenceKeys.TARGET_PORT] = port }
        }
    }

    fun updateSendFrequency(frequency: Int) {
        viewModelScope.launch {
            dataStore.edit { it[PreferenceKeys.SEND_FREQUENCY] = frequency.coerceIn(1, 8000) }
        }
    }

    fun resetBackgroundAndSkin() {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences.remove(PreferenceKeys.BACKGROUND_IMAGE_PATH)
                preferences[PreferenceKeys.SEED_COLOR] = DEFAULT_SEED_COLOR
            }
        }
    }
    fun applySkinConfig(jsonString: String) {
        try {
            val config = Json.decodeFromString<SkinConfig>(jsonString)
            val colorLong = config.seedColor.toColorInt().toLong()
            viewModelScope.launch {
                updateSeedColor(colorLong)
                updateThemeMode(config.themeMode)
                updatePercentPage(config.airWeight)
                updateMultiA(config.multiA)
                updateMultiS(config.multiS)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateThemeMode(mode: Int) {
        viewModelScope.launch {
            dataStore.edit { it[PreferenceKeys.THEME_MODE] = mode }
        }
    }

    fun updateEnableVibration(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { it[PreferenceKeys.ENABLE_VIBRATION] = enabled }
        }
    }

    fun updatePercentPage(value: Float) {
        viewModelScope.launch {
            dataStore.edit { it[PreferenceKeys.PERCENT_PAGE] = value.coerceIn(0.1f, 0.9f) }
        }
    }

    fun updateMultiA(value: Float) {
        viewModelScope.launch {
            dataStore.edit { it[PreferenceKeys.MULTI_A] = value.coerceIn(0f, 0.5f) }
        }
    }

    fun updateMultiS(value: Float) {
        viewModelScope.launch {
            dataStore.edit { it[PreferenceKeys.MULTI_S] = value.coerceIn(0f, 0.5f) }
        }
    }

    fun updateSeedColor(value: Long) {
        viewModelScope.launch {
            dataStore.edit { it[PreferenceKeys.SEED_COLOR] = value }
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            dataStore.edit {
                it.clear()
                it[PreferenceKeys.USE_DYNAMIC_COLOR] = DEFAULT_USE_DYNAMIC_COLOR
            }
        }
    }

    fun updateAccessCodes(code: String) {
        viewModelScope.launch {
            dataStore.edit { it[PreferenceKeys.ACCESS_CODES] = code }
        }
    }

    fun updateProtocolType(type: Int) {
        viewModelScope.launch {
            dataStore.edit { it[PreferenceKeys.PROTOCOL_TYPE] = type }
        }
    }

    fun updateAirMode(mode: Int) {
        viewModelScope.launch {
            dataStore.edit { it[PreferenceKeys.AIR_MODE] = mode }
        }
    }
    fun updateFlickThreshold(value: Int) {
        viewModelScope.launch { dataStore.edit { it[PreferenceKeys.FLICK_THRESHOLD] = value } }
    }
    fun updateFlickEqualizerPlus(value: Int) {
        viewModelScope.launch { dataStore.edit { it[PreferenceKeys.FLICK_EQUALIZER_PLUS] = value } }
    }

    fun updateFlickEqualizerMinus(value: Int) {
        viewModelScope.launch { dataStore.edit { it[PreferenceKeys.FLICK_EQUALIZER_MINUS] = value } }
    }

    fun updateFlickUp(value: Int) {
        viewModelScope.launch { dataStore.edit { it[PreferenceKeys.FLICK_UP] = value } }
    }

    fun updateFlickDown(value: Int) {
        viewModelScope.launch { dataStore.edit { it[PreferenceKeys.FLICK_DOWN] = value } }
    }

    fun updateFlickZoneNum(value: Int) {
        viewModelScope.launch { dataStore.edit { it[PreferenceKeys.FLICK_ZONE_NUM] = value } }
    }
    fun updateFlickOnce(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { it[PreferenceKeys.FLICK_ONCE] = enabled }
        }
    }
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DataManager::class.java)) {
                return DataManager(context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}