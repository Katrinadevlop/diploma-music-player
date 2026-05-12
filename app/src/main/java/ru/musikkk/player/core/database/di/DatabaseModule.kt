package ru.musikkk.player.core.database.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import ru.musikkk.player.core.database.MusikkkDatabase
import ru.musikkk.player.core.database.dao.DownloadDao
import ru.musikkk.player.core.database.dao.LibraryDao
import ru.musikkk.player.core.database.dao.UploadDao

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val DB_NAME = "musikkk.db"

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MusikkkDatabase =
        Room.databaseBuilder(context, MusikkkDatabase::class.java, DB_NAME)
            // Пока схема одна — `fallbackToDestructiveMigration` ок, при
            // изменениях схемы добавим явные Migration.
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideLibraryDao(database: MusikkkDatabase): LibraryDao = database.libraryDao()

    @Provides
    fun provideDownloadDao(database: MusikkkDatabase): DownloadDao = database.downloadDao()

    @Provides
    fun provideUploadDao(database: MusikkkDatabase): UploadDao = database.uploadDao()
}
