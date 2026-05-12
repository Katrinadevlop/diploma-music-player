package ru.musikkk.player.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import ru.musikkk.player.data.auth.AuthRepository

/**
 * Корневая ViewModel — отвечает за выбор стартового маршрута навигации.
 *
 * Решение принимается из двух источников: есть ли сессионный токен и
 * есть ли «висящая» регистрация без подтверждения почты. Приоритет:
 *
 * 1. токен есть → `Library`
 * 2. иначе если есть pending verification → `VerifyEmail` (даже после
 *    рестарта приложения пользователь возвращается в этот флоу)
 * 3. иначе → `Login`
 *
 * Пока решение не вычислено, отдаём `null` — `AppNavHost` показывает
 * индикатор загрузки.
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    authRepository: AuthRepository,
) : ViewModel() {

    val startDestination: StateFlow<String?> = combine(
        authRepository.tokenFlow,
        authRepository.pendingVerificationFlow,
    ) { token, pending ->
        when {
            token != null -> Routes.Library
            pending != null -> Routes.VerifyEmail
            else -> Routes.Login
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null,
    )
}
