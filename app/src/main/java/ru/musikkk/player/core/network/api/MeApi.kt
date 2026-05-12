package ru.musikkk.player.core.network.api

import retrofit2.http.GET
import ru.musikkk.player.core.network.dto.MeResponse

/**
 * Защищённые эндпоинты. Живут на Retrofit-инстансе, на котором установлен
 * [ru.musikkk.player.core.network.AuthInterceptor].
 */
interface MeApi {
    @GET("api/me")
    suspend fun me(): MeResponse
}
