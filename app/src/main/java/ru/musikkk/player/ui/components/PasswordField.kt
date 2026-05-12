package ru.musikkk.player.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import ru.musikkk.player.R

/**
 * Поле ввода пароля в стилистике [MusikkkTextField] + переключатель
 * «показать / скрыть» текстовой кнопкой (чтобы не тянуть
 * `material-icons-extended` ради двух иконок).
 */
@Composable
fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    isVisible: Boolean,
    onToggleVisibility: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    imeAction: ImeAction = ImeAction.Done,
    supportingText: String? = null,
) {
    MusikkkTextField(
        value = value,
        onValueChange = onValueChange,
        label = stringResource(id = R.string.auth_field_password),
        modifier = modifier,
        enabled = enabled,
        isError = isError,
        supportingText = supportingText,
        visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = imeAction,
        ),
        trailingIcon = {
            TextButton(onClick = onToggleVisibility, enabled = enabled) {
                Text(
                    stringResource(
                        id = if (isVisible) R.string.auth_password_hide else R.string.auth_password_show,
                    ),
                )
            }
        },
    )
}
