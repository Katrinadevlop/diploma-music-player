package ru.musikkk.player.data.auth

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import okio.IOException
import retrofit2.HttpException
import ru.musikkk.player.core.datastore.PendingVerification
import ru.musikkk.player.core.datastore.PendingVerificationStore
import ru.musikkk.player.core.datastore.TokenStore
import ru.musikkk.player.core.network.api.AuthApi
import ru.musikkk.player.core.network.dto.ApiError
import ru.musikkk.player.core.network.dto.RegisterRequest
import ru.musikkk.player.core.network.dto.TokenRequest
import ru.musikkk.player.core.network.dto.VerifyResendRequest
import ru.musikkk.player.domain.auth.AuthError
import ru.musikkk.player.domain.auth.AuthSession
import ru.musikkk.player.domain.auth.AuthUser

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val tokenStore: TokenStore,
    private val pendingVerificationStore: PendingVerificationStore,
    private val pendingVerificationCreds: PendingVerificationCreds,
    private val json: Json,
) : AuthRepository {

    override val tokenFlow: Flow<String?> = tokenStore.tokenFlow

    override val pendingVerificationFlow: Flow<PendingVerification?> =
        pendingVerificationStore.pendingFlow

    override suspend fun login(username: String, password: String): AuthSession = wrap {
        val resp = authApi.token(TokenRequest(username = username, password = password))
        val session = AuthSession(
            token = resp.token,
            expiresAtMs = resp.expiresAt,
            user = AuthUser(publicId = resp.user.id, username = resp.user.username),
        )
        tokenStore.save(session.token, session.expiresAtMs)
        // Если пользователь логинится — pending-метаданные больше не нужны.
        pendingVerificationStore.clear()
        pendingVerificationCreds.clear()
        session
    }

    override suspend fun register(
        username: String,
        password: String,
        email: String?,
    ): RegisterOutcome = wrap {
        val resp = authApi.register(
            RegisterRequest(
                username = username,
                password = password,
                email = email,
                consentTermsPrivacy = true,
            )
        )

        if (resp.needsEmailVerification) {
            // Сохраняем метаданные на диск, креды — только в память.
            pendingVerificationStore.set(
                email = email.orEmpty(),
                username = username,
                retryAfterSeconds = resp.retryAfter,
            )
            pendingVerificationCreds.set(username = username, password = password)
            return@wrap RegisterOutcome.NeedsEmailVerification(resp.retryAfter)
        }

        // Сервер автоматически залогинил через cookie (dev-режим без verify).
        // Получаем Bearer тем же путём.
        val session = login(username, password)
        RegisterOutcome.LoggedIn(session)
    }

    override suspend fun resendVerificationEmail(email: String): Int = wrap {
        val resp = authApi.verifyResend(VerifyResendRequest(email = email))
        val cooldown = (resp.retryAfter ?: DEFAULT_RESEND_COOLDOWN_S).coerceAtLeast(1)
        pendingVerificationStore.updateResendCooldown(cooldown)
        cooldown
    }

    override suspend fun clearPendingVerification() {
        pendingVerificationStore.clear()
        pendingVerificationCreds.clear()
    }

    override suspend fun logout() {
        // Best-effort: отзываем сессию на сервере, но локальное состояние
        // чистим в любом случае.
        runCatching { authApi.logout() }
        tokenStore.clear()
        pendingVerificationStore.clear()
        pendingVerificationCreds.clear()
    }

    private suspend fun <T> wrap(block: suspend () -> T): T = try {
        block()
    } catch (e: HttpException) {
        throw mapHttp(e)
    } catch (e: IOException) {
        throw AuthError.Network(e)
    } catch (e: AuthError) {
        throw e
    } catch (e: Throwable) {
        throw AuthError.Unknown(cause = e)
    }

    private fun mapHttp(e: HttpException): AuthError {
        val parsed = parseError(e)
        val code = parsed?.error
        val retry = parsed?.retryAfter

        return when (code) {
            "invalid_credentials" -> AuthError.InvalidCredentials
            "email_not_verified" -> AuthError.EmailNotVerified
            "username_invalid" -> AuthError.UsernameInvalid
            "username_taken" -> AuthError.UsernameTaken
            "password_invalid" -> AuthError.PasswordInvalid
            "password_too_short" -> AuthError.PasswordTooShort
            "email_invalid" -> AuthError.EmailInvalid
            "email_taken" -> AuthError.EmailTaken
            "email_required" -> AuthError.EmailRequired
            "terms_privacy_required" -> AuthError.TermsRequired
            "resend_too_soon" -> AuthError.ResendTooSoon(
                retryAfterSeconds = retry?.coerceAtLeast(1) ?: 1,
            )
            "rate_limited" -> AuthError.RateLimited(retryAfterSeconds = retry)
            else -> when (e.code()) {
                401 -> AuthError.InvalidCredentials
                429 -> AuthError.RateLimited(retryAfterSeconds = retry)
                else -> AuthError.Unknown(cause = e, code = code)
            }
        }
    }

    private fun parseError(e: HttpException): ApiError? = try {
        val raw = e.response()?.errorBody()?.string().orEmpty()
        if (raw.isBlank()) null else json.decodeFromString(ApiError.serializer(), raw)
    } catch (_: Throwable) {
        null
    }

    private companion object {
        const val DEFAULT_RESEND_COOLDOWN_S = 60
    }
}
