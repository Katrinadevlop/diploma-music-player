package ru.musikkk.player.data.user

import javax.inject.Inject
import javax.inject.Singleton
import ru.musikkk.player.core.database.dao.LibraryDao

/**
 * Сервер `/api/user/*` идентифицирует треки по `rel_path`, а очередь
 * плеера у нас — по `blob_id`. Этот резолвер делает прямой lookup в
 * Room: blob → rel_path. Используется PlaybackTracker'ом.
 */
@Singleton
class BlobToPathResolver @Inject constructor(
    private val libraryDao: LibraryDao,
) {
    suspend fun resolve(blobId: String): String? {
        return libraryDao.findFilePath(blobId)
    }
}
