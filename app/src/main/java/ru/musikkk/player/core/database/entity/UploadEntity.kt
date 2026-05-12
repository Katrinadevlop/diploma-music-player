package ru.musikkk.player.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "uploads")
data class UploadEntity(
    @PrimaryKey val id: String,          // UUID, локальный
    val localUri: String,                // content:// URI из SAF
    val displayName: String,             // имя файла из OpenableColumns.DISPLAY_NAME
    val sizeBytes: Long,                 // OpenableColumns.SIZE; -1 если неизвестно
    val uploadedBytes: Long,             // сколько уже отослано worker'ом
    val status: String,                  // см. UploadStatusMapper
    val errorCode: String?,              // код ошибки сервера или "io_error"
    val audioBlobId: String?,            // вернулся сервером после success
    val relPath: String?,                // вернулся сервером (или = displayName)
    val createdAtMs: Long,
    val updatedAtMs: Long,
)
