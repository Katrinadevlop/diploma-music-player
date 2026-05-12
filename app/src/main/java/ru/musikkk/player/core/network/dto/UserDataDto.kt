package ru.musikkk.player.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Везде `track_id` на сервере — это `rel_path` трека (см. mp/user_data.py),
// не внутренний UUID. Названия полей оставляем как у сервера.

// ----- Likes -----

@Serializable
data class LikesResponse(
    val likes: List<String> = emptyList(),
)

@Serializable
data class ToggleLikeRequest(
    @SerialName("track_id") val trackId: String,
)

@Serializable
data class ToggleLikeResponse(
    val ok: Boolean = true,
    val liked: Boolean = false,
)

// ----- Playlists -----

@Serializable
data class PlaylistsResponse(
    val playlists: List<PlaylistDto> = emptyList(),
)

@Serializable
data class PlaylistDto(
    val id: String,
    val name: String,
    val cover: String? = null,
    val tracks: List<String> = emptyList(),
    @SerialName("created_at") val createdAt: Long = 0,
    @SerialName("updated_at") val updatedAt: Long = 0,
)

@Serializable
data class CreatePlaylistRequest(
    val name: String,
)

@Serializable
data class PlaylistEnvelope(
    val ok: Boolean = true,
    val playlist: PlaylistDto,
)

@Serializable
data class UpdatePlaylistRequest(
    val name: String? = null,
    val cover: String? = null,
    val tracks: List<String>? = null,
)

// ----- Recent -----

@Serializable
data class RecentResponse(
    val recent: List<RecentEntryDto> = emptyList(),
)

@Serializable
data class RecentEntryDto(
    @SerialName("track_id") val trackId: String,
    val at: Long = 0,
)

@Serializable
data class AddRecentRequest(
    @SerialName("track_id") val trackId: String,
    val at: Long? = null,
)

// ----- Playcounts -----

@Serializable
data class PlaycountsResponse(
    val counts: Map<String, Int> = emptyMap(),
)

@Serializable
data class BumpPlaycountRequest(
    @SerialName("track_id") val trackId: String,
    val by: Int? = null,
)

@Serializable
data class BumpPlaycountResponse(
    val ok: Boolean = true,
    val count: Int = 0,
)

// ----- Continue -----

/**
 * Сервер на GET /api/user/continue возвращает либо объект, либо `null`.
 * Retrofit/kotlinx.serialization не любит nullable top-level — поэтому
 * клиент использует `@Body` через wrapping, а тут декодим вручную в репозитории.
 */
@Serializable
data class ContinueDto(
    @SerialName("track_id") val trackId: String,
    val time: Double = 0.0,
    val route: Map<String, String>? = null,
    @SerialName("saved_at") val savedAt: Long = 0,
)

@Serializable
data class PutContinueRequest(
    @SerialName("track_id") val trackId: String,
    val time: Double,
    val route: Map<String, String>? = null,
    @SerialName("saved_at") val savedAt: Long? = null,
)
