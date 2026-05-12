package ru.musikkk.player.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import ru.musikkk.player.core.database.dao.LibraryDao
import ru.musikkk.player.core.database.entity.ArtistEntity
import ru.musikkk.player.core.database.entity.ReleaseEntity
import ru.musikkk.player.core.database.entity.TrackEntity

@Database(
    entities = [
        ArtistEntity::class,
        ReleaseEntity::class,
        TrackEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class MusikkkDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao
}
