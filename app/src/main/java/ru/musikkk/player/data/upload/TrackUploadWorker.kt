package ru.musikkk.player.data.upload

import android.content.Context
import androidx.core.net.toUri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.IOException
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.musikkk.player.BuildConfig
import ru.musikkk.player.core.database.dao.UploadDao
import ru.musikkk.player.core.network.di.AuthClient
import ru.musikkk.player.data.library.LibraryRepository
import ru.musikkk.player.domain.upload.UploadStatus

/**
 * Заливает один файл на сервер через `POST /api/library/upload`.
 *
 * Прогресс собирается параллельным тикером: `ProgressRequestBody`
 * атомарно увеличивает счётчик переданных байт, а отдельная корутина
 * раз в [PROGRESS_TICK_MS] пишет его в Room (UI-подписки на DAO
 * получают обновление).
 *
 * После успешного аплоада дёргаем `LibraryRepository.refresh()` —
 * иначе пользователь не увидит залитый трек в библиотеке до следующего
 * захода на экран.
 */
@HiltWorker
class TrackUploadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val uploadDao: UploadDao,
    private val libraryRepository: LibraryRepository,
    private val json: Json,
    @AuthClient private val okHttpClient: OkHttpClient,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val uploadId = inputData.getString(KEY_UPLOAD_ID)
            ?: return@withContext Result.failure()

        val entity = uploadDao.get(uploadId)
            ?: return@withContext Result.failure()

        uploadDao.markStatus(
            id = uploadId,
            status = UploadStatusMapper.toRaw(UploadStatus.Running),
            errorCode = null,
            nowMs = now(),
        )

        val uri = entity.localUri.toUri()
        val displayName = entity.displayName.ifBlank { "upload-$uploadId" }
        val sizeBytes = entity.sizeBytes.coerceAtLeast(0L)

        val contentResolver = applicationContext.contentResolver
        val mediaType = (contentResolver.getType(uri) ?: "application/octet-stream").toMediaTypeOrNull()

        val written = AtomicLong(0)
        val fileBody = ProgressRequestBody(
            source = {
                contentResolver.openInputStream(uri) ?: throw IOException("cannot_open_uri")
            },
            contentType = mediaType,
            contentLength = sizeBytes,
            bytesWritten = written,
        )

        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("rel_path", displayName)
            .addFormDataPart("file", displayName, fileBody)
            .build()

        val request = Request.Builder()
            .url(BuildConfig.API_BASE_URL.trimEnd('/') + "/api/library/upload")
            .post(multipart)
            .build()

        // Тикер прогресса — отдельная корутина, чтобы не блокировать поток.
        val progressJob = launch {
            while (isActive) {
                delay(PROGRESS_TICK_MS)
                uploadDao.updateProgress(
                    id = uploadId,
                    status = UploadStatusMapper.toRaw(UploadStatus.Running),
                    uploaded = written.get(),
                    nowMs = now(),
                )
                setProgress(workDataOf(KEY_PROGRESS_BYTES to written.get()))
            }
        }

        try {
            okHttpClient.newCall(request).execute().use { response ->
                progressJob.cancel()

                val bodyText = response.body?.string().orEmpty()

                if (!response.isSuccessful) {
                    val errorCode = parseErrorCode(bodyText) ?: "http_${response.code}"
                    uploadDao.markStatus(
                        id = uploadId,
                        status = UploadStatusMapper.toRaw(UploadStatus.Failed),
                        errorCode = errorCode,
                        nowMs = now(),
                    )
                    // 429 и 5xx — пробуем ещё раз, остальные 4xx — финальный fail.
                    return@withContext if (response.code == 429 || response.code in 500..599) {
                        Result.retry()
                    } else {
                        Result.failure()
                    }
                }

                val parsed = parseSuccess(bodyText)
                uploadDao.markCompleted(
                    id = uploadId,
                    status = UploadStatusMapper.toRaw(UploadStatus.Completed),
                    audioBlobId = parsed?.audioBlobId,
                    relPath = parsed?.relPath ?: displayName,
                    nowMs = now(),
                )

                // Обновить кэш библиотеки, чтобы новый трек появился у UI.
                runCatching { libraryRepository.refresh() }

                Result.success()
            }
        } catch (e: IOException) {
            progressJob.cancel()
            uploadDao.markStatus(
                id = uploadId,
                status = UploadStatusMapper.toRaw(UploadStatus.Failed),
                errorCode = "io_error",
                nowMs = now(),
            )
            Result.retry()
        } catch (e: Throwable) {
            progressJob.cancel()
            uploadDao.markStatus(
                id = uploadId,
                status = UploadStatusMapper.toRaw(UploadStatus.Failed),
                errorCode = "unknown",
                nowMs = now(),
            )
            Result.failure()
        }
    }

    private fun parseErrorCode(body: String): String? = try {
        if (body.isBlank()) null
        else json.decodeFromString(UploadErrorResponse.serializer(), body).error
    } catch (_: Throwable) {
        null
    }

    private fun parseSuccess(body: String): UploadSuccessResponse? = try {
        if (body.isBlank()) null
        else json.decodeFromString(UploadSuccessResponse.serializer(), body)
    } catch (_: Throwable) {
        null
    }

    private fun now(): Long = System.currentTimeMillis()

    @Serializable
    private data class UploadSuccessResponse(
        val ok: Boolean = true,
        @SerialName("audio_blob_id") val audioBlobId: String? = null,
        @SerialName("rel_path") val relPath: String? = null,
    )

    @Serializable
    private data class UploadErrorResponse(
        val error: String,
    )

    companion object {
        const val KEY_UPLOAD_ID = "upload_id"
        const val KEY_PROGRESS_BYTES = "progress_bytes"

        private const val PROGRESS_TICK_MS = 500L
    }
}
