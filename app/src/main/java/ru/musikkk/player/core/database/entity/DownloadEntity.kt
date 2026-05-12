package ru.musikkk.player.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val blobId: String,
    val releaseId: String?,
    val localFileName: String,    // имя файла в `filesDir/tracks/`
    val sizeBytes: Long,
    val downloadedBytes: Long,
    val status: String,            // см. [ru.musikkk.player.data.download.DownloadStatusMapper]
    val mimeType: String?,
    val errorCode: String?,
    val createdAtMs: Long,
    val updatedAtMs: Long,
)
