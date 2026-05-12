package ru.musikkk.player.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import ru.musikkk.player.core.database.dao.DownloadDao
import ru.musikkk.player.core.database.dao.LibraryDao
import ru.musikkk.player.core.database.entity.ArtistEntity
import ru.musikkk.player.core.database.entity.DownloadEntity
import ru.musikkk.player.core.database.entity.ReleaseEntity
import ru.musikkk.player.core.database.entity.TrackEntity

@Database(
    entities = [
        ArtistEntity::class,
        ReleaseEntity::class,
        TrackEntity::class,
        DownloadEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class MusikkkDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao
    abstract fun downloadDao(): DownloadDao
}
