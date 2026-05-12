package ru.musikkk.player.core.network.api

import retrofit2.http.GET
import ru.musikkk.player.core.network.dto.LibraryResponseDto

/**
 * Эндпоинты библиотеки. Все требуют авторизации, поэтому живут на
 * Retrofit-инстансе с [ru.musikkk.player.core.network.AuthInterceptor].
 */
interface LibraryApi {
    @GET("api/library")
    suspend fun library(): LibraryResponseDto
}
