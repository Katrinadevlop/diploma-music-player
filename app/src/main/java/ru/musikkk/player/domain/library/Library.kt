package ru.musikkk.player.domain.library

/** Секция, в которой лежит релиз в библиотеке артиста. */
enum class ReleaseSection(val raw: String) {
    Album("albums"),
    Ep("eps"),
    Single("singles"),
    Collab("collabs");

    companion object {
        fun fromRaw(value: String?): ReleaseSection =
            entries.firstOrNull { it.raw == value } ?: Album
    }
}

data class Artist(
    val id: String,
    val name: String,
    val avatarCoverId: String?,
)

data class Release(
    val id: String,
    val artistId: String,
    val artistName: String,
    val name: String,
    val section: ReleaseSection,
    val releaseDate: String?,
    val year: String?,
    val coverId: String?,
    val trackCount: Int,
)

/** Релиз вместе со списком треков — пригодится экрану деталей. */
data class ReleaseWithTracks(
    val release: Release,
    val tracks: List<Track>,
)

data class Track(
    val blobId: String,
    val releaseId: String,
    val filePath: String,
    val title: String,
    val artistName: String,
    val albumName: String,
    val duration: Int,
    val trackNumber: Int?,
    val coverId: String?,
    val bitrate: Int,
    val quality: String?,
    val year: String?,
)
