package ru.musikkk.player.data.auth

import kotlinx.coroutines.flow.Flow
import ru.musikkk.player.domain.auth.AuthSession

interface AuthRepository {
    /** Эмитит текущий токен или null, если пользователь не залогинен. */
    val tokenFlow: Flow<String?>

    /** Обменивает учётку на Bearer-токен (POST /api/auth/token). */
    suspend fun login(username: String, password: String): AuthSession

    /** Создаёт аккаунт (POST /api/auth/register). */
    suspend fun register(username: String, password: String, email: String?): RegisterOutcome

    /** Отзывает текущий токен на сервере (POST /api/auth/logout) и чистит локальное состояние. */
    suspend fun logout()
}

sealed class RegisterOutcome {
    /** Сервер принял регистрацию и сразу залогинил (верификация почты не требуется). */
    data class LoggedIn(val session: AuthSession) : RegisterOutcome()

    /** Перед использованием аккаунта нужна верификация почты. */
    data class NeedsEmailVerification(val retryAfterSeconds: Int?) : RegisterOutcome()
}
