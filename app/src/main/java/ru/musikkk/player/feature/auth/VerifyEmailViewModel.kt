package ru.musikkk.player.feature.auth

import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.max
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.musikkk.player.R
import ru.musikkk.player.data.auth.AuthRepository
import ru.musikkk.player.data.auth.PendingVerificationCreds
import ru.musikkk.player.domain.auth.AuthError

data class VerifyEmailUiState(
    val email: String = "",
    val username: String = "",
    val hasInMemoryPassword: Boolean = false,
    val resendCooldownSeconds: Int = 0,
    val isResending: Boolean = false,
    val isConfirming: Boolean = false,
    val isLoadingInitial: Boolean = true,
    @StringRes val infoRes: Int? = null,
    @StringRes val errorRes: Int? = null,
) {
    val canResend: Boolean
        get() = !isResending && resendCooldownSeconds == 0 && email.isNotBlank()
}

sealed class VerifyEmailEvent {
    data object Verified : VerifyEmailEvent()
    data object UseDifferentEmail : VerifyEmailEvent()
    data object GoToLogin : VerifyEmailEvent()
}

@HiltViewModel
class VerifyEmailViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val pendingCreds: PendingVerificationCreds,
) : ViewModel() {

    private val transient = MutableStateFlow(TransientState())

    /**
     * Интервал тикера countdown'а. Видимо для тестов: `runTest`
     * с `advanceUntilIdle()` зацикливается на бесконечном [delay],
     * поэтому в тестах ставим [Long.MAX_VALUE], чтобы тикер сделал
     * один emit и «уснул навсегда» в виртуальном времени.
     */
    @VisibleForTesting
    internal var tickerIntervalMillis: Long = 1_000L

    val state: StateFlow<VerifyEmailUiState> = combine(
        authRepository.pendingVerificationFlow,
        transient,
        tickerFlow(),
    ) { pending, t, _ ->
        if (pending == null) {
            VerifyEmailUiState(isLoadingInitial = false)
        } else {
            val remainingMs = max(0L, pending.resendBlockedUntilMs - System.currentTimeMillis())
            VerifyEmailUiState(
                email = pending.email,
                username = pending.username,
                hasInMemoryPassword = pendingCreds.read() != null,
                resendCooldownSeconds = ((remainingMs + 999) / 1000).toInt(),
                isResending = t.isResending,
                isConfirming = t.isConfirming,
                isLoadingInitial = false,
                infoRes = t.infoRes,
                errorRes = t.errorRes,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
        initialValue = VerifyEmailUiState(),
    )

    private val _events = Channel<VerifyEmailEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    /**
     * Тикер раз в секунду, чтобы countdown пересчитывался. Вшит в [combine]
     * как третий поток — благодаря [SharingStarted.WhileSubscribed] он
     * автоматически останавливается, когда у `state` не остаётся
     * подписчиков (uplift из background или unit-теста — JVM не висит).
     */
    private fun tickerFlow(): Flow<Long> = flow {
        var tick = 0L
        while (true) {
            emit(tick++)
            delay(tickerIntervalMillis)
        }
    }

    fun onConfirmedClick() {
        val creds = pendingCreds.read()
        if (creds == null) {
            // После рестарта приложения пароль потерян — отправляем на Login.
            viewModelScope.launch { _events.send(VerifyEmailEvent.GoToLogin) }
            return
        }

        if (transient.value.isConfirming) return
        transient.update { it.copy(isConfirming = true, errorRes = null, infoRes = null) }

        viewModelScope.launch {
            val result = runCatching {
                authRepository.login(creds.username, creds.password)
            }

            result
                .onSuccess {
                    transient.update { it.copy(isConfirming = false) }
                    _events.send(VerifyEmailEvent.Verified)
                }
                .onFailure { e ->
                    val info = when (e) {
                        AuthError.EmailNotVerified -> R.string.verify_email_not_verified_yet
                        else -> null
                    }
                    val err = if (info != null) null else when (e) {
                        is AuthError -> e.toMessageRes()
                        else -> R.string.auth_error_unknown
                    }
                    transient.update {
                        it.copy(isConfirming = false, infoRes = info, errorRes = err)
                    }
                }
        }
    }

    fun onResendClick() {
        val email = state.value.email
        if (email.isBlank() || !state.value.canResend) return

        transient.update { it.copy(isResending = true, errorRes = null, infoRes = null) }
        viewModelScope.launch {
            val result = runCatching { authRepository.resendVerificationEmail(email) }
            result
                .onSuccess {
                    transient.update {
                        it.copy(
                            isResending = false,
                            infoRes = R.string.verify_email_resend_sent,
                            errorRes = null,
                        )
                    }
                }
                .onFailure { e ->
                    transient.update {
                        it.copy(
                            isResending = false,
                            errorRes = when (e) {
                                is AuthError -> e.toMessageRes()
                                else -> R.string.auth_error_unknown
                            },
                        )
                    }
                }
        }
    }

    fun onUseDifferentEmailClick() {
        viewModelScope.launch {
            authRepository.clearPendingVerification()
            _events.send(VerifyEmailEvent.UseDifferentEmail)
        }
    }

    fun onGoToLoginClick() {
        viewModelScope.launch { _events.send(VerifyEmailEvent.GoToLogin) }
    }

    private data class TransientState(
        val isResending: Boolean = false,
        val isConfirming: Boolean = false,
        @StringRes val infoRes: Int? = null,
        @StringRes val errorRes: Int? = null,
    )
}
