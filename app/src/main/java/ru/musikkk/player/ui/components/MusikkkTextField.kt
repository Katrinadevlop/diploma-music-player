package ru.musikkk.player.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation
import ru.musikkk.player.ui.theme.MusikkkColors
import ru.musikkk.player.ui.theme.MusikkkRadius

/**
 * Поле ввода в стилистике веб-клиента: полупрозрачный фон («glass»),
 * мятный акцент при фокусе, скруглённые углы. Используется на формах
 * (Login, Register, Search, AddToPlaylist, диалогах).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusikkkTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    supportingText: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        isError = isError,
        singleLine = singleLine,
        label = { Text(label) },
        supportingText = supportingText?.let { txt -> { Text(txt) } },
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        trailingIcon = trailingIcon,
        shape = RoundedCornerShape(MusikkkRadius.md),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MusikkkColors.Surface,
            unfocusedContainerColor = MusikkkColors.Surface.copy(alpha = 0.35f),
            disabledContainerColor = MusikkkColors.Surface.copy(alpha = 0.2f),
            focusedBorderColor = MusikkkColors.Accent,
            unfocusedBorderColor = MusikkkColors.BorderStrong,
            focusedLabelColor = MusikkkColors.Accent,
            cursorColor = MusikkkColors.Accent,
        ),
    )
}
