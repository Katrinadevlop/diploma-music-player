package ru.musikkk.player.data.download

import java.io.File
import kotlinx.coroutines.flow.Flow
import ru.musikkk.player.domain.download.DownloadInfo
import ru.musikkk.player.domain.library.Track

interface DownloadRepository {

    /** Наблюдает за состоянием скачивания одного трека (null — нет записи). */
    fun observe(blobId: String): Flow<DownloadInfo?>

    /** Наблюдает за состоянием по набору blob_id. */
    fun observeMany(blobIds: List<String>): Flow<Map<String, DownloadInfo>>

    /** Поставить трек в очередь на скачивание (или продолжить, если был частично). */
    suspend fun enqueue(track: Track)

    /** Отменить активное скачивание и удалить запись/файл. */
    suspend fun cancel(blobId: String)

    /** Удалить скачанный трек (файл + запись в БД). */
    suspend fun delete(blobId: String)

    /** Локальный файл, если трек уже скачан и доступен. */
    suspend fun localFile(blobId: String): File?
}
