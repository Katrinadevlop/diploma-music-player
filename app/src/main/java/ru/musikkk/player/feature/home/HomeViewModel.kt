package ru.musikkk.player.feature.home

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
import okio.IOException
import retrofit2.HttpException
import ru.musikkk.player.R
import ru.musikkk.player.data.auth.AuthRepository
import ru.musikkk.player.data.me.MeRepository

data class HomeUiState(
    val isLoading: Boolean = true,
    val username: String? = null,
    @StringRes val errorRes: Int? = null,
)

sealed class HomeEvent {
    data object SignedOut : HomeEvent()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val meRepository: MeRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private val _events = Channel<HomeEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        loadProfile()
    }

    fun retry() {
        loadProfile()
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _events.send(HomeEvent.SignedOut)
        }
    }

    private fun loadProfile() {
        _state.update { it.copy(isLoading = true, errorRes = null) }
        viewModelScope.launch {
            val result = runCatching { meRepository.fetchMe() }

            result
                .onSuccess { me ->
                    _state.update { it.copy(isLoading = false, username = me.username) }
                }
                .onFailure { e ->
                    val res = when (e) {
                        is HttpException -> if (e.code() == 401) {
                            // Сессия протухла — выкидываем на экран входа.
                            authRepository.logout()
                            _events.send(HomeEvent.SignedOut)
                            return@onFailure
                        } else {
                            R.string.auth_error_unknown
                        }
                        is IOException -> R.string.auth_error_network
                        else -> R.string.auth_error_unknown
                    }
                    _state.update { it.copy(isLoading = false, errorRes = res) }
                }
        }
    }
}
