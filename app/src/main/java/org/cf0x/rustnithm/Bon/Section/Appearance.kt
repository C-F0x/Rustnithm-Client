package org.cf0x.rustnithm.Bon.Section

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.cf0x.rustnithm.Bon.SettingsGroup
import org.cf0x.rustnithm.Bon.ToggleSettingItem
import org.cf0x.rustnithm.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSection(
    themeMode: Int,
    useDynamicColor: Boolean,
    useExpressive: Boolean,
    seedColorLong: Long,
    onThemeChange: (Int) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onExpressiveChange: (Boolean) -> Unit,
    onColorPickerOpen: () -> Unit
) {
    SettingsGroup(title = stringResource(R.string.appearance_title)) {
        Column(modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val options = listOf(
                    stringResource(R.string.theme_light),
                    stringResource(R.string.theme_dark),
                    stringResource(R.string.theme_system)
                )
                options.forEachIndexed { index, label ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                        onClick = { onThemeChange(index) },
                        selected = themeMode == index
                    ) { Text(label) }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    ToggleSettingItem(
                        label = stringResource(R.string.dynamic_color),
                        checked = useDynamicColor,
                        onCheckedChange = onDynamicColorChange
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    ToggleSettingItem(
                        label = stringResource(R.string.expressive),
                        checked = useExpressive,
                        onCheckedChange = onExpressiveChange
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

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
                        stringResource(R.string.skin_seed_color),
                        color = MaterialTheme.colorScheme.onSurface.copy(
                            alpha = if (isCustomEnabled) 1f else 0.38f
                        )
                    )
                },
                leadingContent = {
                    val previewColor = if (isCustomEnabled) Color(seedColorLong)
                    else MaterialTheme.colorScheme.primary
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