package org.cf0x.rustnithm.Bon.Section

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.cf0x.rustnithm.Bon.BonMath
import org.cf0x.rustnithm.Bon.SegmentSwitch
import org.cf0x.rustnithm.Bon.SettingsGroup
import org.cf0x.rustnithm.Bon.SettingClickItem
import org.cf0x.rustnithm.Bon.SettingSliderItem
import org.cf0x.rustnithm.Bon.SettingToggleItem
import org.cf0x.rustnithm.R

@Composable
fun AppearanceSection(
    language: String,
    onLanguageChange: (String) -> Unit,
    themeMode: Int,
    useDynamicColor: Boolean,
    useExpressive: Boolean,
    seedColorLong: Long,
    percentPage: Float,
    enableVibration: Boolean,
    isVibrationHardwareSupported: Boolean,
    onThemeChange: (Int) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onExpressiveChange: (Boolean) -> Unit,
    onColorPickerOpen: () -> Unit,
    onPercentChange: (Float) -> Unit,
    onVibrationChange: (Boolean) -> Unit,
    onImportClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var langExpanded by remember { mutableStateOf(false) }

    val langOptions = listOf(
        "en" to stringResource(R.string.lang_english),
        "zh-CN" to stringResource(R.string.lang_schinese),
        "zh-TW" to stringResource(R.string.lang_tchinese),
        "ja" to stringResource(R.string.lang_japanese),
        "ko" to stringResource(R.string.lang_korean),
        "fr" to stringResource(R.string.lang_french)
    )
    val currentLangLabel = langOptions.firstOrNull { it.first == language }?.second
        ?: stringResource(R.string.lang_english)

    SettingsGroup(icon = Icons.Outlined.AutoAwesome, title = stringResource(R.string.appearance_title)) {
        SegmentSwitch(
            options = listOf(
                stringResource(R.string.theme_light),
                stringResource(R.string.theme_dark),
                stringResource(R.string.theme_system)
            ),
            selectedIndex = themeMode.coerceIn(0, 2),
            onSelect = onThemeChange
        )

        val isCustomEnabled = !useDynamicColor
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                SettingToggleItem(
                    title = stringResource(R.string.dynamic_color),
                    checked = useDynamicColor,
                    onToggle = onDynamicColorChange
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                SettingClickItem(
                    title = stringResource(R.string.skin_seed_color),
                    enabled = isCustomEnabled,
                    onClick = onColorPickerOpen,
                    trailing = {
                        Box(
                            Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isCustomEnabled) Color(seedColorLong)
                                    else MaterialTheme.colorScheme.surfaceContainerHighest
                                )
                        )
                    }
                )
            }
        }

        SettingToggleItem(
            title = stringResource(R.string.expressive),
            checked = useExpressive,
            onToggle = onExpressiveChange
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilledTonalButton(
                onClick = onImportClick,
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.large
            ) {
                Icon(Icons.Default.FileDownload, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.import_skin))
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
                Text(stringResource(R.string.delete_skin))
            }
        }

        SettingSliderItem(
            title = stringResource(R.string.split_ratio),
            value = percentPage,
            valueRange = 0.1f..0.9f,
            onValueChange = onPercentChange,
            displayValue = BonMath.formatPercent(percentPage)
        )

        SettingToggleItem(
            title = stringResource(R.string.haptic_feedback),
            subtitle = if (isVibrationHardwareSupported) {
                stringResource(R.string.haptic_tactile_response)
            } else {
                stringResource(R.string.unsupported)
            },
            checked = enableVibration,
            enabled = isVibrationHardwareSupported,
            onToggle = onVibrationChange
        )

        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )

        SettingClickItem(
            title = stringResource(R.string.lang_select),
            subtitle = currentLangLabel,
            onClick = { langExpanded = !langExpanded },
            trailing = {
                Icon(
                    imageVector = if (langExpanded) {
                        Icons.Default.ExpandLess
                    } else {
                        Icons.AutoMirrored.Filled.KeyboardArrowRight
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        )

        AnimatedVisibility(
            visible = langExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column {
                langOptions.forEachIndexed { index, (code, label) ->
                    val isSelected = language == code
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onLanguageChange(code)
                                langExpanded = false
                            }
                            .padding(horizontal = 30.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    if (index < langOptions.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 30.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    }
}
