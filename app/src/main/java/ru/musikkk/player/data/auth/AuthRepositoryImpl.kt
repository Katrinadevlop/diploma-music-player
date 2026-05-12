package ru.musikkk.player.data.auth

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import okio.IOException
import retrofit2.HttpException
import ru.musikkk.player.core.datastore.TokenStore
import ru.musikkk.player.core.network.api.AuthApi
import ru.musikkk.player.core.network.dto.ApiError
import ru.musikkk.player.core.network.dto.RegisterRequest
import ru.musikkk.player.core.network.dto.TokenRequest
import ru.musikkk.player.domain.auth.AuthError
import ru.musikkk.player.domain.auth.AuthSession
import ru.musikkk.player.domain.auth.AuthUser

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val tokenStore: TokenStore,
    private val json: Json,
) : AuthRepository {

    override val tokenFlow: Flow<String?> = tokenStore.tokenFlow

    override suspend fun login(username: String, password: String): AuthSession = wrap {
        val resp = authApi.token(TokenRequest(username = username, password = password))
        val session = AuthSession(
            token = resp.token,
            expiresAtMs = resp.expiresAt,
            user = AuthUser(publicId = resp.user.id, username = resp.user.username),
        )
        tokenStore.save(session.token, session.expiresAtMs)
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
            return@wrap RegisterOutcome.NeedsEmailVerification(resp.retryAfter)
        }

        // Сервер автоматически залогинил через cookie. Из приложения cookie
        // не видна, поэтому сразу же обмениваем те же креды на Bearer-токен.
        val session = login(username, password)
        RegisterOutcome.LoggedIn(session)
    }

    override suspend fun logout() {
        // Best-effort: отзываем сессию на сервере, но локальное состояние
        // чистим в любом случае.
        runCatching { authApi.logout() }
        tokenStore.clear()
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
        val code = parseErrorCode(e)
        return when {
            e.code() == 401 || code == "invalid_credentials" -> AuthError.InvalidCredentials
            e.code() == 403 && code == "email_not_verified" -> AuthError.EmailNotVerified
            e.code() == 429 || code == "rate_limited" -> AuthError.RateLimited
            else -> AuthError.Unknown(cause = e, code = code)
        }
    }

    private fun parseErrorCode(e: HttpException): String? = try {
        val raw = e.response()?.errorBody()?.string().orEmpty()
        if (raw.isBlank()) null else json.decodeFromString(ApiError.serializer(), raw).error
    } catch (_: Throwable) {
        null
    }
}
