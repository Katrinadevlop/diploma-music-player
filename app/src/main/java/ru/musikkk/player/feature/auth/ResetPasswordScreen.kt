package ru.musikkk.player.feature.auth

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import ru.musikkk.player.R
import ru.musikkk.player.ui.components.GlassSurface
import ru.musikkk.player.ui.components.MusikkkBackdrop
import ru.musikkk.player.ui.components.MusikkkTextField
import ru.musikkk.player.ui.theme.MusikkkRadius
import ru.musikkk.player.ui.theme.MusikkkSpacing

/**
 * Сброс пароля — повторяет `auth_reset_password.html` веб-клиента
 * в режиме `request`: ввод e-mail, кнопка «Отправить ссылку».
 *
 * Сам POST на отправку письма уходит через веб-страницу
 * `https://musikkk.ru/reset-password?email=...` — у бэка нет mobile-API
 * для сброса (только HTML-route с CSRF). Поэтому здесь мы рендерим
 * нативный glass-form, а финальное «отправить» открываем в браузере с
 * предзаполненным email. Пользователь видит знакомую веб-форму с уже
 * вписанным адресом и подтверждает в один тап.
 *
 * Подтверждение нового пароля (`mode=confirm` с токеном из письма)
 * целиком живёт на веб-странице — это deep-link флоу, типичный для
 * email-confirmation (Google, Apple, …).
 */
@Composable
fun ResetPasswordScreen(
    onBackToLogin: () -> Unit,
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    val emailValid = remember(email) { isEmailLooksValid(email) }
    var showError by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        MusikkkBackdrop(coverId = null)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MusikkkSpacing.s5),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(MusikkkSpacing.s7))
            BrandHeader()
            Spacer(Modifier.height(MusikkkSpacing.s5))

            GlassSurface(
                modifier = Modifier.fillMaxWidth(),
                radius = MusikkkRadius.xl,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(MusikkkSpacing.s5),
                    verticalArrangement = Arrangement.spacedBy(MusikkkSpacing.s3),
                ) {
                    Text(
                        text = stringResource(id = R.string.auth_reset_title),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = stringResource(id = R.string.auth_reset_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    MusikkkTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            showError = false
                        },
                        label = stringResource(id = R.string.auth_reset_email_label),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done,
                        ),
                        isError = showError && !emailValid,
                        supportingText = if (showError && !emailValid) {
                            stringResource(id = R.string.auth_reset_email_invalid)
                        } else null,
                    )

                    Button(
                        onClick = {
                            if (!emailValid) {
                                showError = true
                                return@Button
                            }
                            val uri = "https://musikkk.ru/reset-password?email=${android.net.Uri.encode(email.trim())}".toUri()
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            runCatching { context.startActivity(intent) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(id = R.string.auth_reset_send))
                    }

                    Text(
                        text = stringResource(id = R.string.auth_reset_browser_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    TextButton(
                        onClick = onBackToLogin,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) {
                        Text(stringResource(id = R.string.auth_reset_back_to_login))
                    }
                }
            }
        }
    }
}

/**
 * Лёгкая проверка формата email — минимум: содержит `@` и хотя бы одну
 * точку после него. Полноценная RFC-валидация делается на бэке;
 * наша задача — не пускать явный мусор и пустую строку.
 */
private fun isEmailLooksValid(value: String): Boolean {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return false
    val at = trimmed.indexOf('@')
    if (at <= 0 || at == trimmed.lastIndex) return false
    val domain = trimmed.substring(at + 1)
    return '.' in domain && !domain.startsWith('.') && !domain.endsWith('.')
}
