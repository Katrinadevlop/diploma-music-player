package ru.musikkk.player.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.musikkk.player.core.database.entity.UploadEntity

@Dao
interface UploadDao {

    @Query("SELECT * FROM uploads ORDER BY createdAtMs DESC")
    fun observeAll(): Flow<List<UploadEntity>>

    @Query("SELECT * FROM uploads WHERE id = :uploadId LIMIT 1")
    suspend fun get(uploadId: String): UploadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: UploadEntity)

    @Query(
        """
        UPDATE uploads
        SET status = :status,
            uploadedBytes = :uploaded,
            updatedAtMs = :nowMs
        WHERE id = :id
        """
    )
    suspend fun updateProgress(id: String, status: String, uploaded: Long, nowMs: Long)

    @Query(
        """
        UPDATE uploads
        SET status = :status, errorCode = :errorCode, updatedAtMs = :nowMs
        WHERE id = :id
        """
    )
    suspend fun markStatus(id: String, status: String, errorCode: String?, nowMs: Long)

    @Query(
        """
        UPDATE uploads
        SET status = :status,
            audioBlobId = :audioBlobId,
            relPath = :relPath,
            uploadedBytes = sizeBytes,
            errorCode = NULL,
            updatedAtMs = :nowMs
        WHERE id = :id
        """
    )
    suspend fun markCompleted(
        id: String,
        status: String,
        audioBlobId: String?,
        relPath: String?,
        nowMs: Long,
    )

    @Query("DELETE FROM uploads WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM uploads WHERE status = 'COMPLETED'")
    suspend fun clearCompleted()
}
