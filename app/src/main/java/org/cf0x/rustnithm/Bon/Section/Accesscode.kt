package org.cf0x.rustnithm.Bon.Section

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import org.cf0x.rustnithm.Bon.SettingsGroup

@Composable
fun AccesscodeSection(
    accessCodeValue: String,
    isAccessCodeError: Boolean,
    passwordVisible: Boolean,
    onAccessCodeValueChange: (String) -> Unit,
    onAccessCodeToggleVisible: () -> Unit,
    onAccessCodeSave: () -> Unit
) {
    SettingsGroup(title = "Security") {
        Column(modifier = Modifier.padding(16.dp)) {
            OutlinedTextField(
                value = accessCodeValue,
                onValueChange = onAccessCodeValueChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Access Codes (20 Digits)") },
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
                                contentDescription = "Save Access Code",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
    }
}