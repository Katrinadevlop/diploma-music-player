package ru.musikkk.player.core.network

import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Interceptor
import okhttp3.Response
import ru.musikkk.player.core.datastore.TokenStore

/**
 * Добавляет `Authorization: Bearer <token>` к каждому исходящему запросу,
 * если в хранилище есть сессионный токен.
 *
 * Сами auth-эндпоинты (`/api/auth/token`, `/api/auth/register`) хедер
 * нести не должны, поэтому они идут через отдельный OkHttp-клиент без
 * этого перехватчика — см. `@UnauthClient` в [NetworkModule].
 *
 * Токен читается синхронно через [TokenStore.cachedToken] — кэш
 * прогревается при старте приложения, так что внутри dispatcher-потока
 * OkHttp `runBlocking` не нужен.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenStore: TokenStore,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        if (original.header(HEADER_AUTHORIZATION) != null) {
            return chain.proceed(original)
        }

        val token = tokenStore.cachedToken() ?: return chain.proceed(original)

        val authorized = original.newBuilder()
            .header(HEADER_AUTHORIZATION, "Bearer $token")
            .build()
        return chain.proceed(authorized)
    }

    private companion object {
        const val HEADER_AUTHORIZATION = "Authorization"
    }
}
