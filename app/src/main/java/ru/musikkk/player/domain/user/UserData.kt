package ru.musikkk.player.domain.user

/** `trackPath` — это `rel_path` (то, чем сервер идентифицирует трек). */
data class Playlist(
    val id: String,
    val name: String,
    val coverId: String?,
    val trackPaths: List<String>,
    val createdAtMs: Long,
    val updatedAtMs: Long,
)

data class RecentEntry(
    val trackPath: String,
    val atMs: Long,
)

data class PlaycountEntry(
    val trackPath: String,
    val count: Int,
)

data class ContinueState(
    val trackPath: String,
    val timeSeconds: Double,
    val savedAtMs: Long,
)
