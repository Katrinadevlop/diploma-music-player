package ru.musikkk.player.data.library

import ru.musikkk.player.core.database.entity.ArtistEntity
import ru.musikkk.player.core.database.entity.ReleaseEntity
import ru.musikkk.player.core.database.entity.TrackEntity
import ru.musikkk.player.core.network.dto.ArtistDto
import ru.musikkk.player.core.network.dto.LibraryResponseDto
import ru.musikkk.player.core.network.dto.ReleaseDto
import ru.musikkk.player.core.network.dto.TrackDto
import ru.musikkk.player.domain.library.Artist
import ru.musikkk.player.domain.library.Release
import ru.musikkk.player.domain.library.ReleaseSection
import ru.musikkk.player.domain.library.Track

/**
 * Превращает плоский ответ `/api/library` в три плоских списка для Room.
 * Сервер группирует релизы внутри артистов и по секциям, поэтому здесь
 * мы «разворачиваем» структуру.
 */
internal data class LibrarySnapshot(
    val artists: List<ArtistEntity>,
    val releases: List<ReleaseEntity>,
    val tracks: List<TrackEntity>,
)

internal fun LibraryResponseDto.toSnapshot(): LibrarySnapshot {
    val artistEntities = mutableListOf<ArtistEntity>()
    val releaseEntities = mutableListOf<ReleaseEntity>()
    val trackEntities = mutableListOf<TrackEntity>()

    for (artist in artists) {
        artistEntities += artist.toEntity()

        for ((section, releases) in artist.sectionsBySection()) {
            for (release in releases) {
                val releaseId = composeReleaseId(artist.id, section, release.id)
                releaseEntities += release.toEntity(
                    releaseId = releaseId,
                    artist = artist,
                    section = section,
                )
                release.tracks.forEachIndexed { index, track ->
                    trackEntities += track.toEntity(releaseId = releaseId, orderInRelease = index)
                }
            }
        }
    }

    return LibrarySnapshot(artistEntities, releaseEntities, trackEntities)
}

private fun ArtistDto.sectionsBySection(): List<Pair<ReleaseSection, List<ReleaseDto>>> = listOf(
    ReleaseSection.Album to albums,
    ReleaseSection.Ep to eps,
    ReleaseSection.Single to singles,
    ReleaseSection.Collab to collabs,
)

private fun ArtistDto.toEntity(): ArtistEntity =
    ArtistEntity(id = id, name = name, avatarCoverId = avatar)

private fun ReleaseDto.toEntity(
    releaseId: String,
    artist: ArtistDto,
    section: ReleaseSection,
): ReleaseEntity = ReleaseEntity(
    id = releaseId,
    artistId = artist.id,
    artistName = artist.name,
    name = name,
    section = section.raw,
    releaseDate = releaseDate,
    year = tracks.firstNotNullOfOrNull { it.year },
    coverId = cover ?: tracks.firstNotNullOfOrNull { it.coverId },
)

private fun TrackDto.toEntity(releaseId: String, orderInRelease: Int): TrackEntity = TrackEntity(
    blobId = blobId,
    releaseId = releaseId,
    filePath = filePath,
    title = title,
    artistName = artist,
    albumName = album,
    duration = duration,
    trackNumber = trackNumber?.toIntOrNull(),
    coverId = coverId,
    bitrate = bitrate,
    quality = quality,
    year = year,
    orderInRelease = orderInRelease,
)

/**
 * Сервер возвращает «id» релиза равным его имени, а имена не уникальны
 * между артистами и секциями. Чтобы не словить коллизию в PK Room,
 * собираем синтетический id из артиста, секции и имени.
 */
internal fun composeReleaseId(artistId: String, section: ReleaseSection, releaseName: String): String =
    "$artistId|${section.raw}|$releaseName"

// ----- Entity → Domain -----

internal fun ArtistEntity.toDomain(): Artist = Artist(
    id = id,
    name = name,
    avatarCoverId = avatarCoverId,
)

internal fun ReleaseEntity.toDomain(trackCount: Int): Release = Release(
    id = id,
    artistId = artistId,
    artistName = artistName,
    name = name,
    section = ReleaseSection.fromRaw(section),
    releaseDate = releaseDate,
    year = year,
    coverId = coverId,
    trackCount = trackCount,
)

internal fun TrackEntity.toDomain(): Track = Track(
    blobId = blobId,
    releaseId = releaseId,
    filePath = filePath,
    title = title,
    artistName = artistName,
    albumName = albumName,
    duration = duration,
    trackNumber = trackNumber,
    coverId = coverId,
    bitrate = bitrate,
    quality = quality,
    year = year,
)
