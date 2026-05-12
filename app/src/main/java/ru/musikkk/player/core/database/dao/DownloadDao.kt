package ru.musikkk.player.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.musikkk.player.core.database.entity.DownloadEntity

@Dao
interface DownloadDao {

    @Query("SELECT * FROM downloads WHERE blobId = :blobId LIMIT 1")
    fun observe(blobId: String): Flow<DownloadEntity?>

    @Query("SELECT * FROM downloads")
    fun observeAll(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE blobId IN (:blobIds)")
    fun observeMany(blobIds: List<String>): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE blobId = :blobId LIMIT 1")
    suspend fun get(blobId: String): DownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DownloadEntity)

    @Query(
        """
        UPDATE downloads
        SET status = :status,
            downloadedBytes = :downloaded,
            sizeBytes = CASE WHEN :total > 0 THEN :total ELSE sizeBytes END,
            updatedAtMs = :nowMs
        WHERE blobId = :blobId
        """
    )
    suspend fun updateProgress(
        blobId: String,
        status: String,
        downloaded: Long,
        total: Long,
        nowMs: Long,
    )

    @Query(
        """
        UPDATE downloads
        SET status = :status, errorCode = :errorCode, updatedAtMs = :nowMs
        WHERE blobId = :blobId
        """
    )
    suspend fun markStatus(blobId: String, status: String, errorCode: String?, nowMs: Long)

    @Query("DELETE FROM downloads WHERE blobId = :blobId")
    suspend fun delete(blobId: String)
}
