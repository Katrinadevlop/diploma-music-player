package ru.musikkk.player.data.upload

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import ru.musikkk.player.core.database.dao.UploadDao
import ru.musikkk.player.core.database.entity.UploadEntity
import ru.musikkk.player.domain.upload.UploadInfo
import ru.musikkk.player.domain.upload.UploadStatus

@Singleton
class UploadRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val uploadDao: UploadDao,
) : UploadRepository {

    override fun observeAll(): Flow<List<UploadInfo>> =
        uploadDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun enqueue(uri: Uri) = withContext(Dispatchers.IO) {
        // Persistable permission — чтобы worker мог открыть URI и после
        // того, как Activity, инициировавшая выбор, умерла.
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }

        val meta = readMeta(uri, context.contentResolver)
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        uploadDao.upsert(
            UploadEntity(
                id = id,
                localUri = uri.toString(),
                displayName = meta.displayName,
                sizeBytes = meta.sizeBytes,
                uploadedBytes = 0L,
                status = UploadStatusMapper.toRaw(UploadStatus.Queued),
                errorCode = null,
                audioBlobId = null,
                relPath = null,
                createdAtMs = now,
                updatedAtMs = now,
            )
        )

        scheduleWork(id)
    }

    override suspend fun cancel(uploadId: String) = withContext(Dispatchers.IO) {
        WorkManager.getInstance(context).cancelUniqueWork(uniqueWorkName(uploadId))
        uploadDao.markStatus(
            id = uploadId,
            status = UploadStatusMapper.toRaw(UploadStatus.Failed),
            errorCode = "cancelled",
            nowMs = System.currentTimeMillis(),
        )
    }

    override suspend fun retry(uploadId: String) = withContext(Dispatchers.IO) {
        uploadDao.markStatus(
            id = uploadId,
            status = UploadStatusMapper.toRaw(UploadStatus.Queued),
            errorCode = null,
            nowMs = System.currentTimeMillis(),
        )
        scheduleWork(uploadId)
    }

    override suspend fun remove(uploadId: String) = withContext(Dispatchers.IO) {
        WorkManager.getInstance(context).cancelUniqueWork(uniqueWorkName(uploadId))
        uploadDao.delete(uploadId)
    }

    override suspend fun clearCompleted() = withContext(Dispatchers.IO) {
        uploadDao.clearCompleted()
    }

    // ---- internals ----

    private fun scheduleWork(uploadId: String) {
        val request = OneTimeWorkRequestBuilder<TrackUploadWorker>()
            .setInputData(workDataOf(TrackUploadWorker.KEY_UPLOAD_ID to uploadId))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueWorkName(uploadId),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    private data class UriMeta(val displayName: String, val sizeBytes: Long)

    private fun readMeta(uri: Uri, resolver: ContentResolver): UriMeta {
        var displayName = uri.lastPathSegment.orEmpty()
        var sizeBytes = -1L

        resolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIdx >= 0 && !cursor.isNull(nameIdx)) {
                    displayName = cursor.getString(nameIdx)
                }
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
                    sizeBytes = cursor.getLong(sizeIdx)
                }
            }
        }

        if (displayName.isBlank()) displayName = "upload"
        return UriMeta(displayName = displayName, sizeBytes = sizeBytes)
    }

    private fun uniqueWorkName(uploadId: String): String = "$WORK_PREFIX$uploadId"

    private fun UploadEntity.toDomain(): UploadInfo = UploadInfo(
        id = id,
        displayName = displayName,
        sizeBytes = sizeBytes,
        uploadedBytes = uploadedBytes,
        status = UploadStatusMapper.fromRaw(status),
        errorCode = errorCode,
        audioBlobId = audioBlobId,
    )

    private companion object {
        const val WORK_PREFIX = "track-upload:"
    }
}
