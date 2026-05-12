package ru.musikkk.player.feature.auth

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.musikkk.player.R
import ru.musikkk.player.data.auth.AuthRepository
import ru.musikkk.player.data.auth.RegisterOutcome
import ru.musikkk.player.domain.auth.AuthError

data class RegisterUiState(
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val passwordConfirm: String = "",
    val isPasswordVisible: Boolean = false,
    val acceptTerms: Boolean = false,
    val acceptEmailMarketing: Boolean = false,
    val isSubmitting: Boolean = false,
    @StringRes val usernameError: Int? = null,
    @StringRes val emailError: Int? = null,
    @StringRes val passwordError: Int? = null,
    @StringRes val passwordConfirmError: Int? = null,
    @StringRes val termsError: Int? = null,
    @StringRes val formError: Int? = null,
) {
    val canSubmit: Boolean
        get() = !isSubmitting &&
                username.isNotBlank() &&
                email.isNotBlank() &&
                password.isNotEmpty() &&
                passwordConfirm.isNotEmpty() &&
                acceptTerms
}

sealed class RegisterEvent {
    data object Registered : RegisterEvent()
    data class NeedsEmailVerification(val email: String) : RegisterEvent()
}

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()

    private val _events = Channel<RegisterEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onUsernameChange(value: String) {
        _state.update {
            it.copy(
                username = value,
                usernameError = null,
                formError = null,
            )
        }
    }

    fun onEmailChange(value: String) {
        _state.update {
            it.copy(email = value, emailError = null, formError = null)
        }
    }

    fun onPasswordChange(value: String) {
        _state.update {
            it.copy(
                password = value,
                passwordError = null,
                passwordConfirmError = null,
                formError = null,
            )
        }
    }

    fun onPasswordConfirmChange(value: String) {
        _state.update {
            it.copy(passwordConfirm = value, passwordConfirmError = null, formError = null)
        }
    }

    fun onTogglePasswordVisibility() {
        _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onAcceptTermsChange(value: Boolean) {
        _state.update { it.copy(acceptTerms = value, termsError = null, formError = null) }
    }

    fun onAcceptEmailMarketingChange(value: Boolean) {
        _state.update { it.copy(acceptEmailMarketing = value) }
    }

    fun submit() {
        val snapshot = _state.value
        if (snapshot.isSubmitting) return

        val clientErrors = validateClient(snapshot)
        if (clientErrors != null) {
            _state.update { clientErrors }
            return
        }

        _state.update { it.copy(isSubmitting = true) }

        viewModelScope.launch {
            val result = runCatching {
                authRepository.register(
                    username = snapshot.username.trim(),
                    password = snapshot.password,
                    email = snapshot.email.trim(),
                )
            }

            result
                .onSuccess { outcome ->
                    _state.update { it.copy(isSubmitting = false, password = "", passwordConfirm = "") }
                    when (outcome) {
                        is RegisterOutcome.LoggedIn -> _events.send(RegisterEvent.Registered)
                        is RegisterOutcome.NeedsEmailVerification -> _events.send(
                            RegisterEvent.NeedsEmailVerification(email = snapshot.email.trim()),
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { current ->
                        val mapped = applyServerError(current, e as? AuthError ?: AuthError.Unknown(cause = e))
                        mapped.copy(isSubmitting = false)
                    }
                }
        }
    }

    private fun validateClient(state: RegisterUiState): RegisterUiState? {
        val username = state.username.trim()
        val email = state.email.trim()

        var changed = false
        var next = state

        if (!USERNAME_REGEX.matches(username)) {
            next = next.copy(usernameError = R.string.register_error_username_invalid)
            changed = true
        }
        if (!EMAIL_REGEX.matches(email)) {
            next = next.copy(emailError = R.string.register_error_email_invalid)
            changed = true
        }
        if (state.password.length < MIN_PASSWORD_LENGTH) {
            next = next.copy(passwordError = R.string.register_error_password_too_short)
            changed = true
        }
        if (state.password != state.passwordConfirm) {
            next = next.copy(passwordConfirmError = R.string.register_error_password_mismatch)
            changed = true
        }
        if (!state.acceptTerms) {
            next = next.copy(termsError = R.string.register_error_terms_required)
            changed = true
        }

        return if (changed) next else null
    }

    private fun applyServerError(state: RegisterUiState, error: AuthError): RegisterUiState =
        when (error) {
            AuthError.UsernameInvalid -> state.copy(usernameError = R.string.register_error_username_invalid)
            AuthError.UsernameTaken -> state.copy(usernameError = R.string.register_error_username_taken)
            AuthError.PasswordInvalid -> state.copy(passwordError = R.string.register_error_password_invalid)
            AuthError.PasswordTooShort -> state.copy(passwordError = R.string.register_error_password_too_short)
            AuthError.EmailInvalid -> state.copy(emailError = R.string.register_error_email_invalid)
            AuthError.EmailTaken -> state.copy(emailError = R.string.register_error_email_taken)
            AuthError.EmailRequired -> state.copy(emailError = R.string.register_error_email_required)
            AuthError.TermsRequired -> state.copy(termsError = R.string.register_error_terms_required)
            is AuthError.RateLimited -> state.copy(formError = R.string.auth_error_rate_limited)
            is AuthError.Network -> state.copy(formError = R.string.auth_error_network)
            else -> state.copy(formError = R.string.auth_error_unknown)
        }

    private companion object {
        // Имя пользователя — те же правила, что на сервере (`mp/auth.py`).
        val USERNAME_REGEX = Regex("^[a-zA-Z0-9_-]{3,32}$")

        // Простой паттерн для UX-валидации. Окончательную валидацию делает сервер.
        val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

        const val MIN_PASSWORD_LENGTH = 8
    }
}
