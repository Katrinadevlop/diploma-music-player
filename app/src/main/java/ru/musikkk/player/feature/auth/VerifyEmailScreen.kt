package ru.musikkk.player.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import ru.musikkk.player.R
import ru.musikkk.player.ui.theme.MusikkkSpacing

@Composable
fun VerifyEmailScreen(
    onVerified: () -> Unit,
    onUseDifferentEmail: () -> Unit,
    onGoToLogin: () -> Unit,
    viewModel: VerifyEmailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val events = viewModel.events

    LaunchedEffect(events) {
        events.collectLatest { event ->
            when (event) {
                VerifyEmailEvent.Verified -> onVerified()
                VerifyEmailEvent.UseDifferentEmail -> onUseDifferentEmail()
                VerifyEmailEvent.GoToLogin -> onGoToLogin()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = MusikkkSpacing.s5, vertical = MusikkkSpacing.s6),
    ) {
        if (state.isLoadingInitial) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            return@Box
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(id = R.string.verify_email_title),
                style = MaterialTheme.typography.displayLarge,
            )
            Spacer(Modifier.height(MusikkkSpacing.s3))

            val bodyText = if (state.hasInMemoryPassword) {
                stringResource(id = R.string.verify_email_body, state.email)
            } else {
                stringResource(id = R.string.verify_email_body_no_password, state.email)
            }
            Text(
                text = bodyText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (state.infoRes != null) {
                Spacer(Modifier.height(MusikkkSpacing.s3))
                Text(
                    text = stringResource(id = state.infoRes!!),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (state.errorRes != null) {
                Spacer(Modifier.height(MusikkkSpacing.s3))
                Text(
                    text = stringResource(id = state.errorRes!!),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(Modifier.height(MusikkkSpacing.s5))

            if (state.hasInMemoryPassword) {
                Button(
                    onClick = viewModel::onConfirmedClick,
                    enabled = !state.isConfirming,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    if (state.isConfirming) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text(stringResource(id = R.string.verify_email_action_confirmed))
                    }
                }
            } else {
                Button(
                    onClick = viewModel::onGoToLoginClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    Text(stringResource(id = R.string.verify_email_action_go_to_login))
                }
            }

            Spacer(Modifier.height(MusikkkSpacing.s3))

            OutlinedButton(
                onClick = viewModel::onResendClick,
                enabled = state.canResend,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                val label = if (state.resendCooldownSeconds > 0) {
                    stringResource(
                        id = R.string.verify_email_action_resend_countdown,
                        state.resendCooldownSeconds,
                    )
                } else {
                    stringResource(id = R.string.verify_email_action_resend)
                }
                if (state.isResending) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(label)
                }
            }

            Spacer(Modifier.height(MusikkkSpacing.s4))

            TextButton(
                onClick = viewModel::onUseDifferentEmailClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(id = R.string.verify_email_action_use_other_email))
            }
        }
    }
}
