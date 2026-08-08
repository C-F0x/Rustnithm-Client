package org.cf0x.rustnithm.Bon.Section

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import org.cf0x.rustnithm.Bon.BonMath
import org.cf0x.rustnithm.Bon.SegmentSwitch
import org.cf0x.rustnithm.Bon.SettingsGroup
import org.cf0x.rustnithm.Bon.SettingSliderItem
import org.cf0x.rustnithm.Jour.JourBackend
import org.cf0x.rustnithm.R

@Composable
fun ConnectionSection(
    initialIp: String,
    initialPort: String,
    protocolType: Int,
    accessCodeValue: String,
    isAccessCodeError: Boolean,
    passwordVisible: Boolean,
    frequencyValue: Float,
    onIpSaved: (String) -> Unit,
    onPortSaved: (String) -> Unit,
    onProtocolSelect: (Int) -> Unit,
    onAccessCodeValueChange: (String) -> Unit,
    onAccessCodeToggleVisible: () -> Unit,
    onAccessCodeSave: () -> Unit,
    onFrequencyValueChange: (Float) -> Unit,
    onFrequencySave: () -> Unit
) {
    var tempIp by remember { mutableStateOf(initialIp) }
    var tempPort by remember { mutableStateOf(initialPort) }
    var isIpError by remember { mutableStateOf(false) }
    var isPortError by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    SettingsGroup(icon = Icons.Outlined.Link, title = stringResource(R.string.connection_section_title)) {
        OutlinedTextField(
            value = tempIp,
            onValueChange = {
                if (it.length <= 15) {
                    tempIp = it
                    isIpError = it.isNotBlank() && !JourBackend.validateIp(it)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (!focusState.isFocused && tempIp.isNotBlank() && !isIpError) {
                        onIpSaved(tempIp)
                    }
                },
            label = { Text(stringResource(R.string.ip_label)) },
            isError = isIpError,
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (tempIp.isNotBlank() && !isIpError) {
                        onIpSaved(tempIp)
                    } else {
                        isIpError = true
                    }
                    focusManager.clearFocus()
                }
            )
        )

        OutlinedTextField(
            value = tempPort,
            onValueChange = {
                if (it.length <= 5 && it.all { c -> c.isDigit() }) {
                    tempPort = it
                    val p = it.toIntOrNull()
                    isPortError = p == null || p !in 0..65535
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (!focusState.isFocused && tempPort.isNotEmpty() && !isPortError) {
                        onPortSaved(tempPort)
                    }
                },
            label = { Text(stringResource(R.string.port_label)) },
            isError = isPortError,
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (tempPort.isNotEmpty() && !isPortError) {
                        onPortSaved(tempPort)
                    } else {
                        isPortError = true
                    }
                    focusManager.clearFocus()
                }
            )
        )

        SegmentSwitch(
            options = listOf(
                stringResource(R.string.protocol_udp),
                stringResource(R.string.protocol_tcp)
            ),
            selectedIndex = protocolType.coerceIn(0, 1),
            onSelect = onProtocolSelect
        )

        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )

        OutlinedTextField(
            value = accessCodeValue,
            onValueChange = onAccessCodeValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.access_codes_label)) },
            isError = isAccessCodeError,
            shape = MaterialTheme.shapes.large,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                Row {
                    IconButton(onClick = onAccessCodeToggleVisible) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null
                        )
                    }
                    IconButton(onClick = onAccessCodeSave) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = stringResource(R.string.save_access_code),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )

        SettingSliderItem(
            title = stringResource(R.string.frequency),
            value = frequencyValue,
            valueRange = 50f..1000f,
            onValueChange = onFrequencyValueChange,
            onValueChangeFinished = onFrequencySave,
            displayValue = BonMath.formatFrequency(frequencyValue)
        )
    }
}
