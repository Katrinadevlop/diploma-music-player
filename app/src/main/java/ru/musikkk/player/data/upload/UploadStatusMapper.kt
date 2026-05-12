package ru.musikkk.player.data.upload

import ru.musikkk.player.domain.upload.UploadStatus

/** Конвертация enum ↔ строки для хранения в SQLite. */
internal object UploadStatusMapper {

    fun toRaw(status: UploadStatus): String = when (status) {
        UploadStatus.Queued -> "QUEUED"
        UploadStatus.Running -> "RUNNING"
        UploadStatus.Completed -> "COMPLETED"
        UploadStatus.Failed -> "FAILED"
    }

    fun fromRaw(raw: String?): UploadStatus = when (raw) {
        "RUNNING" -> UploadStatus.Running
        "COMPLETED" -> UploadStatus.Completed
        "FAILED" -> UploadStatus.Failed
        else -> UploadStatus.Queued
    }
}
