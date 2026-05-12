package ru.musikkk.player.data.upload

import android.net.Uri
import kotlinx.coroutines.flow.Flow
import ru.musikkk.player.domain.upload.UploadInfo

interface UploadRepository {

    /** Все аплоады, отсортированные по времени постановки в очередь (новые сверху). */
    fun observeAll(): Flow<List<UploadInfo>>

    /**
     * Поставить файл из SAF-URI в очередь на аплоад.
     * - забирает persistable URI permission,
     * - читает имя и размер из `OpenableColumns`,
     * - пишет запись в БД,
     * - ставит `OneTimeWork` с уникальным именем `upload:<uploadId>`.
     */
    suspend fun enqueue(uri: Uri)

    /** Отменить активный аплоад. Запись остаётся в БД со статусом Failed. */
    suspend fun cancel(uploadId: String)

    /** Повторить упавший аплоад. */
    suspend fun retry(uploadId: String)

    /** Удалить запись об аплоаде (история). */
    suspend fun remove(uploadId: String)

    /** Очистить все завершённые (Completed) аплоады. */
    suspend fun clearCompleted()
}
