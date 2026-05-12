package ru.musikkk.player.data.download

import android.content.Context
import android.net.Uri
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.musikkk.player.core.database.dao.DownloadDao
import ru.musikkk.player.core.database.entity.DownloadEntity
import ru.musikkk.player.domain.download.DownloadInfo
import ru.musikkk.player.domain.download.DownloadStatus
import ru.musikkk.player.domain.library.Track

@Singleton
class DownloadRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadDao: DownloadDao,
    private val files: DownloadFiles,
) : DownloadRepository {

    override fun observe(blobId: String): Flow<DownloadInfo?> =
        downloadDao.observe(blobId).map { it?.toDomain() }

    override fun observeMany(blobIds: List<String>): Flow<Map<String, DownloadInfo>> =
        downloadDao.observeMany(blobIds).map { list ->
            list.associate { it.blobId to it.toDomain() }
        }

    override suspend fun enqueue(track: Track) {
        val now = System.currentTimeMillis()
        val existing = downloadDao.get(track.blobId)

        if (existing?.statusEnum == DownloadStatus.Completed && files.fileFor(track.blobId).exists()) {
            // Уже скачан — ничего не делаем.
            return
        }

        downloadDao.upsert(
            DownloadEntity(
                blobId = track.blobId,
                releaseId = track.releaseId,
                localFileName = files.fileNameFor(track.blobId),
                sizeBytes = existing?.sizeBytes ?: 0L,
                downloadedBytes = existing?.downloadedBytes ?: 0L,
                status = DownloadStatusMapper.toRaw(DownloadStatus.Queued),
                mimeType = null,
                errorCode = null,
                createdAtMs = existing?.createdAtMs ?: now,
                updatedAtMs = now,
            )
        )

        val request = OneTimeWorkRequestBuilder<TrackDownloadWorker>()
            .setInputData(workDataOf(TrackDownloadWorker.KEY_BLOB_ID to track.blobId))
            .setConstraints(
                // На фоне ОК и через мобильную сеть — пользователь явно нажал «скачать».
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueWorkName(track.blobId),
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    override suspend fun cancel(blobId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(uniqueWorkName(blobId))
        files.fileFor(blobId).delete()
        downloadDao.delete(blobId)
    }

    override suspend fun delete(blobId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(uniqueWorkName(blobId))
        files.fileFor(blobId).delete()
        downloadDao.delete(blobId)
    }

    override suspend fun localFile(blobId: String): File? {
        val entity = downloadDao.get(blobId) ?: return null
        if (entity.statusEnum != DownloadStatus.Completed) return null
        val file = files.fileFor(blobId)
        return file.takeIf { it.exists() && it.length() > 0 }
    }

    // ---- mapping ----

    private val DownloadEntity.statusEnum: DownloadStatus
        get() = DownloadStatusMapper.fromRaw(status)

    private fun DownloadEntity.toDomain(): DownloadInfo {
        val statusEnum = statusEnum
        val localUri = if (statusEnum == DownloadStatus.Completed) {
            val file = files.fileFor(blobId)
            if (file.exists() && file.length() > 0) Uri.fromFile(file) else null
        } else null

        return DownloadInfo(
            blobId = blobId,
            status = statusEnum,
            downloadedBytes = downloadedBytes,
            sizeBytes = sizeBytes,
            localUri = localUri,
            errorCode = errorCode,
        )
    }

    private fun uniqueWorkName(blobId: String): String = "$WORK_PREFIX$blobId"

    private companion object {
        const val WORK_PREFIX = "track-download:"
    }
}
