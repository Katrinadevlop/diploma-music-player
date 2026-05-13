package ru.musikkk.player.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import ru.musikkk.player.R
import ru.musikkk.player.ui.components.GlassSurface
import ru.musikkk.player.ui.components.MusikkkBackdrop
import ru.musikkk.player.ui.components.MusikkkTextField
import ru.musikkk.player.ui.components.PasswordField
import ru.musikkk.player.ui.theme.MusikkkRadius
import ru.musikkk.player.ui.theme.MusikkkSpacing

@Composable
fun LoginScreen(
    onAuthenticated: () -> Unit,
    onRegisterClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val events = viewModel.events

    LaunchedEffect(events) {
        events.collectLatest { event ->
            when (event) {
                LoginEvent.AuthSucceeded -> onAuthenticated()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MusikkkBackdrop(coverId = null)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MusikkkSpacing.s4, vertical = MusikkkSpacing.s6),
            verticalArrangement = Arrangement.Center,
        ) {
            BrandHeader()

            Spacer(Modifier.height(MusikkkSpacing.s6))

            GlassSurface(
                modifier = Modifier.fillMaxWidth(),
                radius = MusikkkRadius.xl,
            ) {
                Column(modifier = Modifier.padding(MusikkkSpacing.s5)) {
                    Text(
                        text = stringResource(id = R.string.auth_login_title),
                        style = MaterialTheme.typography.headlineLarge,
                    )
                    Spacer(Modifier.height(MusikkkSpacing.s1))
                    Text(
                        text = stringResource(id = R.string.auth_login_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(Modifier.height(MusikkkSpacing.s5))

                    MusikkkTextField(
                        value = state.username,
                        onValueChange = viewModel::onUsernameChange,
                        label = stringResource(id = R.string.auth_field_username),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isSubmitting,
                        isError = state.errorRes != null,
                        supportingText = stringResource(id = R.string.auth_field_username_hint),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Ascii,
                            imeAction = ImeAction.Next,
                            capitalization = KeyboardCapitalization.None,
                            autoCorrectEnabled = false,
                        ),
                    )

                    Spacer(Modifier.height(MusikkkSpacing.s3))

                    PasswordField(
                        value = state.password,
                        onValueChange = viewModel::onPasswordChange,
                        isVisible = state.isPasswordVisible,
                        onToggleVisibility = viewModel::onTogglePasswordVisibility,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isSubmitting,
                        isError = state.errorRes != null,
                        imeAction = ImeAction.Done,
                    )

                    if (state.errorRes != null) {
                        Spacer(Modifier.height(MusikkkSpacing.s3))
                        Text(
                            text = stringResource(id = state.errorRes!!),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }

                    Spacer(Modifier.height(MusikkkSpacing.s5))

                    Button(
                        onClick = viewModel::submit,
                        enabled = state.canSubmit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                    ) {
                        if (state.isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Text(stringResource(id = R.string.auth_action_login))
                        }
                    }

                    Spacer(Modifier.height(MusikkkSpacing.s2))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = stringResource(id = R.string.auth_login_no_account),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextButton(onClick = onRegisterClick, enabled = !state.isSubmitting) {
                            Text(stringResource(id = R.string.auth_login_register_link))
                        }
                    }

                    TextButton(
                        onClick = onForgotPasswordClick,
                        enabled = !state.isSubmitting,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) {
                        Text(stringResource(id = R.string.auth_login_forgot_password))
                    }
                }
            }
        }
    }
}

@Composable
internal fun BrandHeader() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        // На вебе бренд — 34px / 700. Берём чуть выше середины headline-шкалы Material3.
        Text(
            text = "Musikkk",
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "musikkk.ru",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
