package ru.musikkk.player.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Ответ `GET /api/library`. Сервер группирует треки по артистам и
 * секциям (`albums`/`eps`/`singles`/`collabs`).
 */
@Serializable
data class LibraryResponseDto(
    val artists: List<ArtistDto> = emptyList(),
)

@Serializable
data class ArtistDto(
    val id: String,
    val name: String,
    val avatar: String? = null,
    val albums: List<ReleaseDto> = emptyList(),
    val eps: List<ReleaseDto> = emptyList(),
    val singles: List<ReleaseDto> = emptyList(),
    val collabs: List<ReleaseDto> = emptyList(),
)

@Serializable
data class ReleaseDto(
    val id: String,
    val name: String,
    @SerialName("release_date") val releaseDate: String? = null,
    val cover: String? = null,
    val tracks: List<TrackDto> = emptyList(),
)

@Serializable
data class TrackDto(
    @SerialName("blob_id") val blobId: String,
    @SerialName("original_blob_id") val originalBlobId: String? = null,
    @SerialName("audio_variants") val audioVariants: Map<String, String> = emptyMap(),
    @SerialName("file_path") val filePath: String,
    val filename: String? = null,
    val artist: String,
    @SerialName("album_artist") val albumArtist: String? = null,
    val album: String,
    val title: String,
    val duration: Int = 0,
    val bitrate: Int = 0,
    @SerialName("sample_rate") val sampleRate: Int = 0,
    @SerialName("bit_depth") val bitDepth: Int? = null,
    val quality: String? = null,
    val date: String? = null,
    val year: String? = null,
    @SerialName("track_number") val trackNumber: String? = null,
    @SerialName("cover_id") val coverId: String? = null,
    @SerialName("size_bytes") val sizeBytes: Long = 0,
)
