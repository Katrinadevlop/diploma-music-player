package ru.musikkk.player.core.network.api

import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import ru.musikkk.player.core.network.dto.AddRecentRequest
import ru.musikkk.player.core.network.dto.BumpPlaycountRequest
import ru.musikkk.player.core.network.dto.BumpPlaycountResponse
import ru.musikkk.player.core.network.dto.CreatePlaylistRequest
import ru.musikkk.player.core.network.dto.LikesResponse
import ru.musikkk.player.core.network.dto.PlaycountsResponse
import ru.musikkk.player.core.network.dto.PlaylistEnvelope
import ru.musikkk.player.core.network.dto.PlaylistsResponse
import ru.musikkk.player.core.network.dto.PutContinueRequest
import ru.musikkk.player.core.network.dto.RecentResponse
import ru.musikkk.player.core.network.dto.ToggleLikeRequest
import ru.musikkk.player.core.network.dto.ToggleLikeResponse
import ru.musikkk.player.core.network.dto.UpdatePlaylistRequest

/**
 * Эндпоинты `/api/user/...` — пользовательские данные (лайки, плейлисты,
 * recent, top, continue). Все требуют авторизации.
 *
 * Сервер `/api/user/continue` возвращает либо JSON-объект, либо JSON-null.
 * Retrofit-маппинг nullable верхнего уровня капризный, поэтому забираем
 * сырой `ResponseBody` и парсим в репозитории.
 */
interface UserDataApi {

    // Likes
    @GET("api/user/likes")
    suspend fun getLikes(): LikesResponse

    @POST("api/user/likes/toggle")
    suspend fun toggleLike(@Body body: ToggleLikeRequest): ToggleLikeResponse

    // Playlists
    @GET("api/user/playlists")
    suspend fun getPlaylists(): PlaylistsResponse

    @POST("api/user/playlists")
    suspend fun createPlaylist(@Body body: CreatePlaylistRequest): PlaylistEnvelope

    @PUT("api/user/playlists/{id}")
    suspend fun updatePlaylist(
        @Path("id") id: String,
        @Body body: UpdatePlaylistRequest,
    ): PlaylistEnvelope

    @DELETE("api/user/playlists/{id}")
    suspend fun deletePlaylist(@Path("id") id: String)

    // Recent
    @GET("api/user/recent")
    suspend fun getRecent(): RecentResponse

    @POST("api/user/recent")
    suspend fun addRecent(@Body body: AddRecentRequest)

    // Playcounts
    @GET("api/user/playcounts")
    suspend fun getPlaycounts(): PlaycountsResponse

    @POST("api/user/playcounts/bump")
    suspend fun bumpPlaycount(@Body body: BumpPlaycountRequest): BumpPlaycountResponse

    // Continue
    @GET("api/user/continue")
    suspend fun getContinue(): ResponseBody

    @PUT("api/user/continue")
    suspend fun putContinue(@Body body: PutContinueRequest)
}
