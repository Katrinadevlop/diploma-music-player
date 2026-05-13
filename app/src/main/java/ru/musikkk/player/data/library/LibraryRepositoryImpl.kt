package ru.musikkk.player.data.library

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import ru.musikkk.player.core.database.dao.LibraryDao
import ru.musikkk.player.core.network.api.LibraryApi
import ru.musikkk.player.domain.library.Artist
import ru.musikkk.player.domain.library.Release
import ru.musikkk.player.domain.library.Track

@Singleton
class LibraryRepositoryImpl @Inject constructor(
    private val libraryApi: LibraryApi,
    private val libraryDao: LibraryDao,
) : LibraryRepository {

    override fun observeReleases(): Flow<List<Release>> =
        combine(
            libraryDao.observeReleases(),
            libraryDao.observeTrackCountsByRelease(),
        ) { entities, counts ->
            entities.map { it.toDomain(trackCount = counts[it.id] ?: 0) }
        }

    override fun observeArtists(): Flow<List<Artist>> =
        libraryDao.observeArtists().map { entities -> entities.map { it.toDomain() } }

    override fun observeArtist(artistId: String): Flow<Artist?> =
        libraryDao.observeArtist(artistId).map { it?.toDomain() }

    override fun observeReleasesByArtist(artistId: String): Flow<List<Release>> =
        combine(
            libraryDao.observeReleasesByArtist(artistId),
            libraryDao.observeTrackCountsByRelease(),
        ) { entities, counts ->
            entities.map { it.toDomain(trackCount = counts[it.id] ?: 0) }
        }

    override fun observeRelease(releaseId: String): Flow<Release?> =
        combine(
            libraryDao.observeRelease(releaseId),
            libraryDao.observeReleaseTracks(releaseId),
        ) { release, tracks ->
            release?.toDomain(trackCount = tracks.size)
        }

    override fun observeReleaseTracks(releaseId: String): Flow<List<Track>> =
        libraryDao.observeReleaseTracks(releaseId).map { entities ->
            entities.map { it.toDomain() }
        }

    override fun searchReleases(query: String, limit: Int): Flow<List<Release>> =
        libraryDao.searchReleases(query, limit).map { entities ->
            entities.map { it.toDomain(trackCount = 0) }
        }

    override fun searchTracks(query: String, limit: Int): Flow<List<Track>> =
        libraryDao.searchTracks(query, limit).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun tracksByPaths(paths: List<String>): List<Track> {
        if (paths.isEmpty()) return emptyList()
        val byPath = libraryDao.findTracksByPaths(paths).associateBy { it.filePath }
        // Сохраняем порядок входных rel_path (важно для Recent/Playlists).
        return paths.mapNotNull { byPath[it]?.toDomain() }
    }

    override suspend fun trackByPath(path: String): Track? =
        libraryDao.findTrackByPath(path)?.toDomain()

    override suspend fun refresh() {
        val response = libraryApi.library()
        val snapshot = response.toSnapshot()
        libraryDao.replaceAll(
            artists = snapshot.artists,
            releases = snapshot.releases,
            tracks = snapshot.tracks,
        )
    }
}
