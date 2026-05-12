package ru.musikkk.player.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import ru.musikkk.player.data.auth.AuthRepository

/**
 * Корневая ViewModel — отвечает за выбор стартового маршрута навигации.
 *
 * Один раз читает токен из DataStore через [AuthRepository.tokenFlow] и
 * решает, нужен ли пользователю экран входа или сразу домашний. После
 * первого решения маршрутом управляет уже сама навигация (Login.onSuccess
 * → Library, Library.onLogout → Login).
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    authRepository: AuthRepository,
) : ViewModel() {

    val startDestination: StateFlow<String?> = authRepository.tokenFlow
        .map { token -> if (token != null) Routes.Library else Routes.Login }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )
}
