package ru.musikkk.player.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import ru.musikkk.player.core.database.entity.ArtistEntity
import ru.musikkk.player.core.database.entity.ReleaseEntity
import ru.musikkk.player.core.database.entity.TrackEntity

@Dao
interface LibraryDao {

    // ----- наблюдение -----

    @Query(
        """
        SELECT * FROM releases
        ORDER BY
            CASE WHEN releaseDate IS NULL OR releaseDate = '' THEN 1 ELSE 0 END,
            releaseDate DESC,
            artistName COLLATE NOCASE ASC,
            name COLLATE NOCASE ASC
        """
    )
    fun observeReleases(): Flow<List<ReleaseEntity>>

    @Query("SELECT * FROM releases WHERE id = :releaseId LIMIT 1")
    fun observeRelease(releaseId: String): Flow<ReleaseEntity?>

    @Query(
        """
        SELECT * FROM tracks
        WHERE releaseId = :releaseId
        ORDER BY orderInRelease ASC
        """
    )
    fun observeReleaseTracks(releaseId: String): Flow<List<TrackEntity>>

    @Query("SELECT COUNT(*) FROM tracks WHERE releaseId = :releaseId")
    suspend fun countTracksInRelease(releaseId: String): Int

    // ----- запись -----

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertArtists(items: List<ArtistEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReleases(items: List<ReleaseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTracks(items: List<TrackEntity>)

    @Query("DELETE FROM artists")
    suspend fun clearArtists()

    @Query("DELETE FROM releases")
    suspend fun clearReleases()

    @Query("DELETE FROM tracks")
    suspend fun clearTracks()

    /**
     * Полностью заменяет содержимое библиотеки. Делаем именно так
     * (а не diff-merge), потому что сервер — источник истины: всё,
     * чего нет в свежем ответе, должно исчезнуть и из кэша.
     */
    @Transaction
    suspend fun replaceAll(
        artists: List<ArtistEntity>,
        releases: List<ReleaseEntity>,
        tracks: List<TrackEntity>,
    ) {
        clearTracks()
        clearReleases()
        clearArtists()
        upsertArtists(artists)
        upsertReleases(releases)
        upsertTracks(tracks)
    }
}
