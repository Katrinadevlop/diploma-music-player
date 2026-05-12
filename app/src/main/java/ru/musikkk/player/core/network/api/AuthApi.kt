package ru.musikkk.player.core.network.api

import retrofit2.http.Body
import retrofit2.http.POST
import ru.musikkk.player.core.network.dto.RegisterRequest
import ru.musikkk.player.core.network.dto.RegisterResponse
import ru.musikkk.player.core.network.dto.TokenRequest
import ru.musikkk.player.core.network.dto.TokenResponse

/**
 * Эндпоинты, которые выдают или отзывают сессионный токен. Эти вызовы
 * не должны нести `Authorization`, поэтому живут на Retrofit-инстансе
 * без [ru.musikkk.player.core.network.AuthInterceptor].
 */
interface AuthApi {
    @POST("api/auth/token")
    suspend fun token(@Body body: TokenRequest): TokenResponse

    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequest): RegisterResponse

    @POST("api/auth/logout")
    suspend fun logout()
}
