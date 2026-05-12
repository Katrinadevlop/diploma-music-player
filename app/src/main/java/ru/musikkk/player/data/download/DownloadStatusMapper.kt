package ru.musikkk.player.data.download

import ru.musikkk.player.domain.download.DownloadStatus

/** Конвертация enum ↔ строки для хранения в SQLite. */
internal object DownloadStatusMapper {

    fun toRaw(status: DownloadStatus): String = when (status) {
        DownloadStatus.Queued -> "QUEUED"
        DownloadStatus.Running -> "RUNNING"
        DownloadStatus.Completed -> "COMPLETED"
        DownloadStatus.Failed -> "FAILED"
    }

    fun fromRaw(raw: String?): DownloadStatus = when (raw) {
        "RUNNING" -> DownloadStatus.Running
        "COMPLETED" -> DownloadStatus.Completed
        "FAILED" -> DownloadStatus.Failed
        else -> DownloadStatus.Queued
    }
}
