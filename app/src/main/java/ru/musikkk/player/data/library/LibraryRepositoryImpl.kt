package ru.musikkk.player.data.library

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import ru.musikkk.player.core.database.dao.LibraryDao
import ru.musikkk.player.core.network.api.LibraryApi
import ru.musikkk.player.domain.library.Release
import ru.musikkk.player.domain.library.Track

@Singleton
class LibraryRepositoryImpl @Inject constructor(
    private val libraryApi: LibraryApi,
    private val libraryDao: LibraryDao,
) : LibraryRepository {

    override fun observeReleases(): Flow<List<Release>> =
        libraryDao.observeReleases().map { entities ->
            // Считать треки на каждый релиз отдельным suspend-вызовом было
            // бы N+1; для UI карточек хватает количества из ответа сервера,
            // которое мы сюда не пробрасываем — добавим, когда понадобится.
            entities.map { it.toDomain(trackCount = 0) }
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
