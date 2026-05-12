package ru.musikkk.player.domain.download

import android.net.Uri

enum class DownloadStatus { Queued, Running, Completed, Failed }

/**
 * Состояние скачивания одного трека для UI. `localUri` появляется только
 * после [DownloadStatus.Completed]; для остальных статусов — null
 * (включая «файл был удалён вне приложения» — мы это детектируем в
 * репозитории и считаем, что скачивания нет).
 */
data class DownloadInfo(
    val blobId: String,
    val status: DownloadStatus,
    val downloadedBytes: Long,
    val sizeBytes: Long,
    val localUri: Uri?,
    val errorCode: String?,
) {
    val progress: Float
        get() = when {
            status == DownloadStatus.Completed -> 1f
            sizeBytes > 0 -> (downloadedBytes.toFloat() / sizeBytes.toFloat()).coerceIn(0f, 1f)
            else -> 0f
        }
}
