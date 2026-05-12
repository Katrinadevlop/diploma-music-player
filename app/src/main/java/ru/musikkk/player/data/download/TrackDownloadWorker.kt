package ru.musikkk.player.data.download

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.FileOutputStream
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.musikkk.player.core.network.MediaUrls
import ru.musikkk.player.core.network.di.AuthClient
import ru.musikkk.player.core.database.dao.DownloadDao
import ru.musikkk.player.domain.download.DownloadStatus

/**
 * Качает трек с сервера на устройство. Поддерживает Range-resume:
 * если файл уже частично есть, посылает `Range: bytes=N-` и дописывает.
 *
 * Прогресс пишется в БД (для UI) и в `Worker.setProgress` (для WorkManager).
 * Кодов ошибок мало — стрим либо есть, либо нет; на 401/403 retry бесполезен,
 * на network errors — `Result.retry()` с дефолтным WorkManager backoff.
 */
@HiltWorker
class TrackDownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val downloadDao: DownloadDao,
    private val files: DownloadFiles,
    @AuthClient private val okHttpClient: OkHttpClient,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): androidx.work.ListenableWorker.Result = withContext(Dispatchers.IO) {
        val blobId = inputData.getString(KEY_BLOB_ID)
            ?: return@withContext androidx.work.ListenableWorker.Result.failure()

        val entity = downloadDao.get(blobId)
            ?: return@withContext androidx.work.ListenableWorker.Result.failure()

        val targetFile = files.fileFor(blobId)
        val alreadyDownloaded = if (targetFile.exists()) targetFile.length() else 0L

        downloadDao.markStatus(
            blobId = blobId,
            status = DownloadStatusMapper.toRaw(DownloadStatus.Running),
            errorCode = null,
            nowMs = now(),
        )

        val request = Request.Builder()
            .url(MediaUrls.trackStreamUrl(blobId = blobId, variant = null))
            .apply { if (alreadyDownloaded > 0) header("Range", "bytes=$alreadyDownloaded-") }
            .build()

        try {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext fail(blobId, "http_${response.code}")
                }

                val body = response.body
                    ?: return@withContext fail(blobId, "empty_body")

                // Сервер на Range-запросе вернёт 206 + Content-Range, по
                // которому считаем итоговый размер. Без Range — Content-Length.
                val total = computeTotalBytes(response, alreadyDownloaded)

                targetFile.parentFile?.mkdirs()
                val appendMode = alreadyDownloaded > 0 && response.code == 206

                FileOutputStream(targetFile, appendMode).use { out ->
                    body.byteStream().use { input ->
                        val buf = ByteArray(BUFFER_BYTES)
                        var written = if (appendMode) alreadyDownloaded else 0L
                        var lastReportedAt = 0L

                        while (true) {
                            val read = input.read(buf)
                            if (read < 0) break
                            out.write(buf, 0, read)
                            written += read

                            if (written - lastReportedAt >= PROGRESS_TICK_BYTES) {
                                lastReportedAt = written
                                downloadDao.updateProgress(
                                    blobId = blobId,
                                    status = DownloadStatusMapper.toRaw(DownloadStatus.Running),
                                    downloaded = written,
                                    total = total,
                                    nowMs = now(),
                                )
                                setProgress(workDataOf(KEY_PROGRESS_BYTES to written))
                            }
                        }

                        downloadDao.updateProgress(
                            blobId = blobId,
                            status = DownloadStatusMapper.toRaw(DownloadStatus.Completed),
                            downloaded = written,
                            total = if (total > 0) total else written,
                            nowMs = now(),
                        )
                    }
                }

                androidx.work.ListenableWorker.Result.success()
            }
        } catch (e: IOException) {
            // Сеть — пробуем retry, WorkManager сам делает backoff.
            downloadDao.markStatus(
                blobId = blobId,
                status = DownloadStatusMapper.toRaw(DownloadStatus.Failed),
                errorCode = "io_error",
                nowMs = now(),
            )
            androidx.work.ListenableWorker.Result.retry()
        }
    }

    private suspend fun fail(blobId: String, code: String): androidx.work.ListenableWorker.Result {
        downloadDao.markStatus(
            blobId = blobId,
            status = DownloadStatusMapper.toRaw(DownloadStatus.Failed),
            errorCode = code,
            nowMs = now(),
        )
        return androidx.work.ListenableWorker.Result.failure()
    }

    private fun computeTotalBytes(
        response: okhttp3.Response,
        alreadyDownloaded: Long,
    ): Long {
        val contentRange = response.header("Content-Range")
        if (contentRange != null) {
            // Формат: "bytes 0-1023/2048" — берём после `/`.
            val total = contentRange.substringAfter('/').toLongOrNull()
            if (total != null && total > 0) return total
        }
        val contentLength = response.body?.contentLength() ?: -1L
        return if (contentLength > 0) contentLength + alreadyDownloaded else 0L
    }

    private fun now(): Long = System.currentTimeMillis()

    companion object {
        const val KEY_BLOB_ID = "blob_id"
        const val KEY_PROGRESS_BYTES = "progress_bytes"

        private const val BUFFER_BYTES = 64 * 1024
        private const val PROGRESS_TICK_BYTES = 256 * 1024L
    }
}
