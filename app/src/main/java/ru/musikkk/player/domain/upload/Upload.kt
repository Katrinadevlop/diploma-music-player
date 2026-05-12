package ru.musikkk.player.domain.upload

enum class UploadStatus { Queued, Running, Completed, Failed }

/**
 * Состояние одного аплоада для UI. `audioBlobId` появляется после
 * успешного ответа сервера и идентифицирует уже залитый трек.
 */
data class UploadInfo(
    val id: String,
    val displayName: String,
    val sizeBytes: Long,
    val uploadedBytes: Long,
    val status: UploadStatus,
    val errorCode: String?,
    val audioBlobId: String?,
) {
    val progress: Float
        get() = when {
            status == UploadStatus.Completed -> 1f
            sizeBytes > 0 -> (uploadedBytes.toFloat() / sizeBytes.toFloat()).coerceIn(0f, 1f)
            else -> 0f
        }
}
