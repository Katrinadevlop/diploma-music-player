package ru.musikkk.player.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.MapColumn
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

    @Query("SELECT * FROM artists ORDER BY name COLLATE NOCASE ASC")
    fun observeArtists(): Flow<List<ArtistEntity>>

    @Query("SELECT * FROM artists WHERE id = :artistId LIMIT 1")
    fun observeArtist(artistId: String): Flow<ArtistEntity?>

    @Query(
        """
        SELECT * FROM releases
        WHERE artistId = :artistId
        ORDER BY
            CASE WHEN releaseDate IS NULL OR releaseDate = '' THEN 1 ELSE 0 END,
            releaseDate DESC,
            name COLLATE NOCASE ASC
        """
    )
    fun observeReleasesByArtist(artistId: String): Flow<List<ReleaseEntity>>

    /**
     * Map `releaseId → trackCount`. Используется на главной и экране
     * артиста, чтобы карточка релиза показывала количество треков.
     * `@MapColumn` собирает результат в `Map<String, Int>` без N+1.
     */
    @Query("SELECT releaseId, COUNT(*) AS c FROM tracks GROUP BY releaseId")
    fun observeTrackCountsByRelease(): Flow<Map<
        @MapColumn(columnName = "releaseId") String,
        @MapColumn(columnName = "c") Int,
    >>

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

    @Query("SELECT filePath FROM tracks WHERE blobId = :blobId LIMIT 1")
    suspend fun findFilePath(blobId: String): String?

    @Query("SELECT * FROM tracks WHERE filePath IN (:paths)")
    suspend fun findTracksByPaths(paths: List<String>): List<TrackEntity>

    @Query("SELECT * FROM tracks WHERE filePath = :path LIMIT 1")
    suspend fun findTrackByPath(path: String): TrackEntity?

    // ----- поиск -----

    /**
     * Поиск по релизам: совпадение в названии релиза или имени артиста.
     * `LIKE` сравнение case-insensitive через `COLLATE NOCASE`. Сначала
     * идут те, у кого `name` начинается с запроса (prefix-match), потом
     * остальные. `LIMIT` нужен, чтобы крупная библиотека не повесила UI.
     */
    @Query(
        """
        SELECT * FROM releases
        WHERE name LIKE '%' || :query || '%' COLLATE NOCASE
           OR artistName LIKE '%' || :query || '%' COLLATE NOCASE
        ORDER BY
            CASE WHEN name LIKE :query || '%' COLLATE NOCASE THEN 0 ELSE 1 END,
            artistName COLLATE NOCASE ASC,
            name COLLATE NOCASE ASC
        LIMIT :limit
        """
    )
    fun searchReleases(query: String, limit: Int): Flow<List<ReleaseEntity>>

    /** Поиск по трекам: совпадение в названии трека или имени артиста. */
    @Query(
        """
        SELECT * FROM tracks
        WHERE title LIKE '%' || :query || '%' COLLATE NOCASE
           OR artistName LIKE '%' || :query || '%' COLLATE NOCASE
        ORDER BY
            CASE WHEN title LIKE :query || '%' COLLATE NOCASE THEN 0 ELSE 1 END,
            title COLLATE NOCASE ASC
        LIMIT :limit
        """
    )
    fun searchTracks(query: String, limit: Int): Flow<List<TrackEntity>>

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
