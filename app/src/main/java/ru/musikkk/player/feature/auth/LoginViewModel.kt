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
import ru.musikkk.player.domain.auth.AuthError

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isSubmitting: Boolean = false,
    @StringRes val errorRes: Int? = null,
) {
    val canSubmit: Boolean
        get() = !isSubmitting && username.isNotBlank() && password.isNotEmpty()
}

sealed class LoginEvent {
    data object AuthSucceeded : LoginEvent()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    private val _events = Channel<LoginEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onUsernameChange(value: String) {
        _state.update { it.copy(username = value, errorRes = null) }
    }

    fun onPasswordChange(value: String) {
        _state.update { it.copy(password = value, errorRes = null) }
    }

    fun onTogglePasswordVisibility() {
        _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun submit() {
        val snapshot = _state.value
        if (snapshot.isSubmitting) return
        if (snapshot.username.isBlank() || snapshot.password.isEmpty()) {
            _state.update { it.copy(errorRes = R.string.auth_error_empty_fields) }
            return
        }

        _state.update { it.copy(isSubmitting = true, errorRes = null) }
        viewModelScope.launch {
            val result = runCatching {
                authRepository.login(
                    username = snapshot.username.trim(),
                    password = snapshot.password,
                )
            }

            result
                .onSuccess {
                    _state.update { it.copy(isSubmitting = false, password = "") }
                    _events.send(LoginEvent.AuthSucceeded)
                }
                .onFailure { e ->
                    val res = (e as? AuthError)?.toMessageRes() ?: R.string.auth_error_unknown
                    _state.update { it.copy(isSubmitting = false, errorRes = res) }
                }
        }
    }
}
